package com.dvorkin;

import org.apache.commons.io.FileUtils;

import javax.swing.JOptionPane;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by dvorkin on 01.10.2017.
 */
public class FileProcessor {

    private List<HelperContext> data;
    private StatisticsCollector statCollector;
    private boolean stopFlag;


    public FileProcessor(List<File> cryptoFiles) {
        data = new ArrayList<>(cryptoFiles.size());
        statCollector = new StatisticsCollector();
        stopFlag = false;

        cryptoFiles.forEach(cryptoFile -> {
            HelperContext context = new HelperContext();
            data.add(context);
            context.setCryptoFile(cryptoFile);
            String fileName = cryptoFile.getName().toUpperCase();
            int cryptoExtensionIndex = fileName.lastIndexOf(Constants.ENCRYPTED_FILE_EXTENSION);
            if (cryptoExtensionIndex == -1) {
                throw new RuntimeException("Impossible! File that just matched the criteria now fails it");
            }
            String originalFileName = cryptoFile.getName().substring(0, cryptoExtensionIndex);
            File encryptedFile = new File(cryptoFile.getParentFile(), originalFileName);
            if (encryptedFile.exists()) {
                context.setEncryptedFile(encryptedFile);
            }
            int extensionIndex = originalFileName.lastIndexOf(".");
            String extension;
            String name;
            String decryptedFileName;
            int copyIndex = 0;
            while (true) {
                if (extensionIndex == -1) {
                    name = originalFileName;
                    extension = "";
                    decryptedFileName = name + "(" + copyIndex + ")";
                } else {
                    name = originalFileName.substring(0, extensionIndex);
                    extension = originalFileName.substring(extensionIndex + 1);
                    decryptedFileName = name + "(" + copyIndex + ")." + extension;
                }
                File decryptedFile = new File(cryptoFile.getParentFile(), decryptedFileName);
                if (decryptedFile.exists()) {
                    context.addDecryptedFile(decryptedFile);
                    copyIndex++;
                } else {
                    break;
                }
            }
            statCollector.registerAdditionalCopies(context.getDecryptedFileList().size() - 1);
            FileType fileType = FileType.getByExtension(extension);
            context.setFileType(fileType);
            if (FileType.UNKNOWN == fileType) {
                statCollector.registerUnknownExtension(extension, context);
            }
        });
    }

    public void processFiles() {
        data.forEach(context -> {
            if (context.getEncryptedFile() == null) {
                statCollector.registerMissingEncryptedFile(context);
            } else if (context.getDecryptedFileList().isEmpty()) {
                statCollector.registerMissingDecryptedFile(context);
            } else {
                boolean singleDecryptedCopy = true;
                if (context.getDecryptedFileList().size() > 1) {
                  singleDecryptedCopy = handleMultipleDecryptions(context);
                }
                if (singleDecryptedCopy) {
                    handleDecryption(context);
                }
            }
        });
        postCollectStatistics();
    }

    private void handleDecryption(HelperContext context) {
        if (!ensureRealEncryption(context)) {
            statCollector.registerNotRealEncryption(context);
        } else {
            if ((context.getFileType() != FileType.UNKNOWN) && !isStopFlagSet()) {
                activateHumanCheck(context);

                String message = "You have just saw file \"" +
                        context.getDecryptedFile().getAbsolutePath() +
                        "\", is the content OK and it should be restored?";
                int ret = JOptionPane.showConfirmDialog(null, message,
                        "Restore the file?", JOptionPane.YES_NO_CANCEL_OPTION);
                //0 means YES, 1 means NO, 2 means CANCEL, -1 means user closed the dialog
                if (ret == 2 || ret == -1) {
                    setStopFlag(); //eny attempt to close dialog without choosing means user wants to stop us
                    context.setStatus(ProcessingStatus.REJECT);
                } else if (ret == 1) {
                    context.setStatus(ProcessingStatus.REJECT);
                } else if (ret == 0) {
                    boolean deleted = context.getEncryptedFile().delete();
                    if (!deleted) {
                        context.setStatus(ProcessingStatus.FAILURE);
                    } else {
                        deleted = context.getCryptoFile().delete();
                        if (!deleted) {
                            context.setStatus(ProcessingStatus.FAILURE);
                        } else {
                            Path path = context.getDecryptedFile().toPath();
                            DosFileAttributeView view = Files.getFileAttributeView(path, DosFileAttributeView.class);
                            if (view == null) {
                                context.setStatus(ProcessingStatus.FAILURE);
                            } else {
                                try {
                                    view.setArchive(false);
                                    view.setSystem(false);
                                    view.setReadOnly(false);
                                    view.setHidden(false);
                                    boolean renamed = context.getDecryptedFile().renameTo(context.getEncryptedFile());
                                    if (renamed) {
                                        context.setStatus(ProcessingStatus.SUCCESS);
                                    } else {
                                        context.setStatus(ProcessingStatus.FAILURE);
                                    }
                                } catch (IOException e) {
                                    context.setStatus(ProcessingStatus.FAILURE);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     *  Method handles only multiple copies, leaving just single copy
     */
    private boolean handleMultipleDecryptions(HelperContext context) {
        List<File> decryptedCopies = context.getDecryptedFileList();
        File firstCopy = decryptedCopies.get(0);
        boolean ret = true;
        try {
            for (int i = 1; i < decryptedCopies.size(); i++) {
                if (!FileUtils.contentEquals(firstCopy, decryptedCopies.get(i))) {
                    System.out.println("Found different copies of decrypted file " + context.getEncryptedFile().getAbsolutePath()
                            + " Seems like this file needs to be reported to DrWeb for further help with decryption");
                    statCollector.registerStrangeFile(context);
                    context.setStatus(ProcessingStatus.FAILURE);
                    ret = false;
                    break;
                }
            }
            if (ret) {
                for (int i = 1; i < decryptedCopies.size(); i++) {
                    ret = decryptedCopies.get(i).delete();
                    if (!ret) {
                        context.setStatus(ProcessingStatus.FAILURE);
                        break;
                    }
                    statCollector.incrementRemovedCopiesNumber();
                }
                if (ret) {
                    for (int i = decryptedCopies.size() - 1; i > 0; i--) {
                        decryptedCopies.remove(i);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error during handling of multiple copies:" + e.getMessage());
            context.setStatus(ProcessingStatus.FAILURE);
            ret = false;
        }
        return ret;
    }

    private void setStopFlag() {
        stopFlag = true;
    }

    private boolean isStopFlagSet() {
        return stopFlag;
    }

    private boolean ensureRealEncryption(HelperContext context) {
        boolean ret = false;
        long origSize = context.getEncryptedFile().length();
        long decriptedSize = context.getDecryptedFile().length();
        long cryptoSize = context.getCryptoFile().length();
        if (origSize > Constants.MINIMUM_ENCRYPTED_FILE_SIZE) {
            if (origSize != decriptedSize) {
                return ret;
            }
            ret = ensureEncryption(context.getEncryptedFile());
        } else if (origSize < Constants.MINIMUM_ENCRYPTED_FILE_SIZE) {
            return ret;
        } else {
            if (decriptedSize >= cryptoSize) {
                return ret;
            }
            ret = ensureEncryption(context.getEncryptedFile());
        }
        return ret;
    }

    private boolean ensureEncryption(File file) {
        boolean ret = false;
        int totalBytesRead = 0;
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead = 0;
            while (bytesRead >= 0) {
                bytesRead = in.read(buffer);
                for (int i = 0; i < bytesRead; i++) {
                    if (totalBytesRead + i < Constants.ENCRYPTED_FILE_CONTENTS_START.length) {
                        if (buffer[i] != Constants.ENCRYPTED_FILE_CONTENTS_START[totalBytesRead + i]) {
                            return ret;
                        }
                    } else if (totalBytesRead + i >= Constants.MAXIMUM_REALLY_ENCRYPTED_FILE_PART_SIZE) {
                        ret = true;
                        return ret;
                    } else {
                        if (buffer[i] != 0) {
                            return ret;
                        }
                    }
                }
                if (bytesRead > 0) {
                    totalBytesRead += bytesRead;
                }
            }

        } catch (IOException e) {
            return ret;
        }
        ret = true;
        return ret;
    }

    private void activateHumanCheck(HelperContext context) {
        List<String> commandArguments = new ArrayList<>(3);
        switch (context.getFileType()) {
            case EMPTY:
            case TEXT:
            case XML:
            case CSV:
            case SQL:
                commandArguments.add("\"C:\\Program Files\\Far2\\Far.exe\"");
                commandArguments.add("/v");
                break;
            case PDF:
                commandArguments.add("\"C:\\Program Files (x86)\\Adobe\\Reader 10.0\\Reader\\AcroRd32.exe\"");
                break;
            case IMAGE1:
            case IMAGE2:
                commandArguments.add("C:\\Windows\\System32\\mspaint.exe");
                break;
            case ARCHIVE1:
            case ARCHIVE2:
                commandArguments.add("\"C:\\Program Files\\7-Zip\\7zFM.exe\"");
                break;
            case EXCEL1:
            case EXCEL2:
                commandArguments.add("\"C:\\Program Files\\Microsoft Office\\Office14\\EXCEL.EXE\"");
                break;
//            case WORD1:
//            case WORD2:
            case WORD3:
                commandArguments.add("\"C:\\Program Files\\Microsoft Office\\Office14\\WINWORD.EXE\"");
                break;
            case VIDEO1:
            case VIDEO2:
            case VIDEO3:
            case AUDIO1:
                commandArguments.add("\"C:\\Program Files (x86)\\VideoLAN\\VLC\\vlc.exe\"");
                break;
            case UNKNOWN:
                throw new RuntimeException("Unknown type should not present");
            default:
                throw new RuntimeException("Unknown type should not present");
        }
        commandArguments.add(context.getDecryptedFile().getAbsolutePath());
        ProcessBuilder pb = new ProcessBuilder(commandArguments);
        pb = pb.inheritIO();
        try {
            Process process = pb.start();
            int ret = process.waitFor();
            if (ret != 0) {
                System.out.println("Abnormal viewer termination");
                System.out.println(ret);
            }
        } catch (Exception e) {
            System.out.println("Something bad happened");
            e.printStackTrace();
        }
    }

    public void showStatistics() {
        System.out.println("Total: " + data.size() + " files");
        System.out.println(statCollector.getMissingEncryptedFiles().size() + " original files are misssing");
        System.out.println(statCollector.getMissingDecryptedFiles().size() + " files are not decrypted by DrWeb");
        System.out.println(statCollector.getNotReallyEncryptedFiles().size() + " files are not really encrypted");
        System.out.println(statCollector.getUnknownExtensionsFiles().size() + " files are of unknown type");
        System.out.println(statCollector.getAdditionalCopiesNumber() + " decrypted files were decrypted several times");
        System.out.println(statCollector.getRemovedCopiesNumber() + " copies of decrypted files were cleared");
        System.out.println("Unknown extensions: " + statCollector.getUnknownExtensions());
        System.out.println("======");
        System.out.println(statCollector.getSuccessfullyProcessedEntries().size() + " files were successfully processed");
        System.out.println(statCollector.getNotProcessedEntries().size() + " files were not processed");
        System.out.println(statCollector.getRejectedEntries().size() + " files were rejected");
        System.out.println(statCollector.getFailedEntries().size() + " files were failed");
        System.out.println(statCollector.getStrangeFiles().size() + " files are strange");
    }

    private void postCollectStatistics() {
        data.forEach(context -> {
            switch(context.getStatus()) {
                case NOT_PROCESSED:
                    statCollector.registerNotProcessedEntry(context);
                    break;
                case SUCCESS:
                    statCollector.registerSuccessfullyProcessedEntry(context);
                    break;
                case FAILURE:
                    statCollector.registerFailedEntry(context);
                    break;
                case REJECT:
                    statCollector.registerRejectedEntry(context);
                    break;
                default:
                    throw new RuntimeException("Unknown processing status");
            }
        });
    }
}
