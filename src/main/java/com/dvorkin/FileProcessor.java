package com.dvorkin;

import com.dvorkin.json.ExtensionCommand;
import com.dvorkin.json.Settings;
import org.apache.commons.io.FileUtils;

import javax.swing.JOptionPane;
import java.io.BufferedInputStream;
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
    private Settings settings;

    public FileProcessor(Settings settings, List<File> cryptoFiles) {
        this.settings = settings;
        data = new ArrayList<>(cryptoFiles.size());
        statCollector = new StatisticsCollector();
        stopFlag = false;

        cryptoFiles.forEach(cryptoFile -> {
            HelperContext context = new HelperContext();
            data.add(context);
            context.setCryptoFile(cryptoFile);
            String fileName = cryptoFile.getName().toUpperCase();
            int cryptoExtensionIndex = fileName.lastIndexOf(settings.getEncryptedFileExtension());
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
            context.setExtension(extension);
            if (settings.getExtensionCommand(extension) == null) {
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
        EncryptionStatus status = determineEncryptionStatus(context);
        switch (status) {
            case POTENTIAL_MIRACLE: //rare case
                System.out.println("Potential miracle on " + context.getEncryptedFile().getAbsolutePath());
            case OK:
                handleOkDecrypted(context);
                break;
            case MIRACLE_DUPLICATE: //rare case
                System.out.println("Miracle duplicate on " + context.getEncryptedFile().getAbsolutePath());
            case OK_DUPLICATE:
                handleOkDuplicate(context);
                break;
            case OK_DIFFERENT_FILE:
                handleOkDifferentFile(context);
                break;
            case LOGICAL_ERROR: //I believe it will never happen
                System.out.println("Internal logic error. Stopping further processing");
                setStopFlag();
                context.setStatus(ProcessingStatus.REJECT);
                break;
            case DECRYPTION_ERROR:
                handleDecryptionError(context);
                break;
            case DECRYPTION_ERROR_NOT_ENCRYPTED:
                handleDecryptionErrorNotEncrypted(context);
                break;
            case NOT_ENCRYPTED:
                handleNotEncrypted(context);
                break;
            case ENCRYPTION_ERROR: //should not happen
                statCollector.registerEncryptionError(context);
                break;
            case PROVED_NO_RECOVERY:
                statCollector.registerNoRecovery(context);
                break;

        }
    }

    private void handleDecryptionErrorNotEncrypted(HelperContext context) {
        ExtensionCommand command = settings.getExtensionCommand(context.getExtension());
        if ((command != null) && !isStopFlagSet()) {
            if (isKnownDecryptionBug(context)) {
                if (prepareCleanDecryption(context)) {
                    activateHumanCheck(command, context.getDecryptedFileList().get(1));

                    String message = "You have just saw decrypted (and fixed)(copy) file \"" +
                            context.getDecryptedFileList().get(1).getAbsolutePath() +
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
                        boolean deleted = context.getCryptoFile().delete();
                        if (!deleted) {
                            context.setStatus(ProcessingStatus.FAILURE);
                        } else {
                            deleted = context.getDecryptedFileList().get(0).delete();
                            if (!deleted) {
                                context.setStatus(ProcessingStatus.FAILURE);
                            } else {
                                Path path = context.getDecryptedFileList().get(1).toPath();
                                DosFileAttributeView view = Files.getFileAttributeView(path, DosFileAttributeView.class);
                                if (view == null) {
                                    context.setStatus(ProcessingStatus.FAILURE);
                                } else {
                                    try {
                                        view.setArchive(false);
                                        view.setSystem(false);
                                        view.setReadOnly(false);
                                        view.setHidden(false);
                                        context.setStatus(ProcessingStatus.SUCCESS);
                                    } catch (IOException e) {
                                        context.setStatus(ProcessingStatus.FAILURE);
                                    }
                                }
                            }
                        }

                    }
                } else {
                    context.setStatus(ProcessingStatus.FAILURE);
                }
            } else {
                statCollector.registerUnknownDecryptionError(context);
            }
        }
    }

    private void handleDecryptionError(HelperContext context) {
        ExtensionCommand command = settings.getExtensionCommand(context.getExtension());
        if ((command != null) && !isStopFlagSet()) {
            if (isKnownDecryptionBug(context)) {
                if (prepareCleanDecryption(context)) {
                    activateHumanCheck(command, context.getDecryptedFileList().get(1));

                    String message = "You have just saw decrypted (and fixed) file \"" +
                            context.getDecryptedFileList().get(1).getAbsolutePath() +
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
                        boolean deleted = context.getCryptoFile().delete();
                        if (!deleted) {
                            context.setStatus(ProcessingStatus.FAILURE);
                        } else {
                            deleted = context.getDecryptedFileList().get(0).delete();
                            if (!deleted) {
                                context.setStatus(ProcessingStatus.FAILURE);
                            } else {
                                deleted = context.getEncryptedFile().delete();
                                if (!deleted) {
                                    context.setStatus(ProcessingStatus.FAILURE);
                                } else {
                                    Path path = context.getDecryptedFileList().get(1).toPath();
                                    DosFileAttributeView view = Files.getFileAttributeView(path, DosFileAttributeView.class);
                                    if (view == null) {
                                        context.setStatus(ProcessingStatus.FAILURE);
                                    } else {
                                        try {
                                            view.setArchive(false);
                                            view.setSystem(false);
                                            view.setReadOnly(false);
                                            view.setHidden(false);
                                            boolean renamed = context.getDecryptedFileList().get(1).renameTo(context.getEncryptedFile());
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
                } else {
                    context.setStatus(ProcessingStatus.FAILURE);
                }
            } else {
                statCollector.registerUnknownDecryptionError(context);
            }
        }
    }

    private boolean prepareCleanDecryption(HelperContext context) {
        try {
            byte[] content = FileUtils.readFileToByteArray(context.getDecryptedFile());
            int index = content.length - 4;
            while (index > 0 && content[index - 1] == 0) {
                index--;
            }

            File bad = context.getDecryptedFile();
            String filename = bad.getName();
            int dotIndex = filename.lastIndexOf(".");
            String justName = filename.substring(0, dotIndex);
            if (!justName.endsWith("(0)")) {
                throw new RuntimeException("Single copy invariant failed");
            }
            String rightName = justName.substring(0, justName.length() - 3) + "(fix)" + filename.substring(dotIndex);
            File good = new File(bad.getParentFile(), rightName);
            FileUtils.writeByteArrayToFile(good, content, 0, index);
            context.addDecryptedFile(good);
            return true;
        } catch (IOException e) {
            System.out.println("Error making clean decryption " + e.getMessage());
            context.setStatus(ProcessingStatus.FAILURE);
            return false;
        }
    }

    private boolean isKnownDecryptionBug(HelperContext context) {
        if (context.getDecryptedFile().length() != context.getCryptoFile().length()) {
            return false;
        }
        if (context.getEncryptedFile().length() <= context.getDecryptedFile().length()) {
            return false;
        }
        try {
            byte[] decrBytes = FileUtils.readFileToByteArray(context.getDecryptedFile());
            byte[] cryptoBytes = FileUtils.readFileToByteArray(context.getCryptoFile());
            if (decrBytes.length == cryptoBytes.length) {
                int index = decrBytes.length - 1;
                if (decrBytes[index] != cryptoBytes[index]) {
                    return false;
                }
                index--;
                if (decrBytes[index] != cryptoBytes[index]) {
                    return false;
                }
                index--;
                if (decrBytes[index] != cryptoBytes[index]) {
                    return false;
                }
                index--;
                return decrBytes[index] == cryptoBytes[index];
            } else {
                return false;
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            return false;
        }
    }

    private void handleOkDecrypted(HelperContext context) {
        ExtensionCommand command = settings.getExtensionCommand(context.getExtension());
        if ((command != null) && !isStopFlagSet()) {
            activateHumanCheck(command, context.getDecryptedFile());

            String message = "You have just saw decrypted file \"" +
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

    private void handleOkDuplicate(HelperContext context) {
        ExtensionCommand command = settings.getExtensionCommand(context.getExtension());
        if ((command != null) && !isStopFlagSet()) {
            activateHumanCheck(command, context.getDecryptedFile());

            String message = "You have just saw decrypted(copy) file \"" +
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
                boolean deleted = context.getCryptoFile().delete();
                if (!deleted) {
                    context.setStatus(ProcessingStatus.FAILURE);
                } else {
                    try {
                        if (FileUtils.contentEquals(context.getEncryptedFile(), context.getDecryptedFile())) {
                            deleted = context.getDecryptedFile().delete();
                            if (!deleted) {
                                context.setStatus(ProcessingStatus.FAILURE);
                            } else {
                                context.setStatus(ProcessingStatus.SUCCESS);
                            }
                        } else {
                            Path path = context.getDecryptedFile().toPath();
                            DosFileAttributeView view = Files.getFileAttributeView(path, DosFileAttributeView.class);
                            if (view == null) {
                                context.setStatus(ProcessingStatus.FAILURE);
                            } else {
                                view.setArchive(false);
                                view.setSystem(false);
                                view.setReadOnly(false);
                                view.setHidden(false);
                                context.setStatus(ProcessingStatus.SUCCESS);
                            }
                        }
                    } catch (IOException e) {
                        context.setStatus(ProcessingStatus.FAILURE);
                    }
                }

            }
        }
    }

    private void handleOkDifferentFile(HelperContext context) {
        ExtensionCommand command = settings.getExtensionCommand(context.getExtension());
        if ((command != null) && !isStopFlagSet()) {
            if (isKnownDecryptionBug(context)) {
                if (prepareCleanDecryption(context)) {
                    activateHumanCheck(command, context.getDecryptedFileList().get(1));

                    String message = "You have just saw decrypted (and fixed) file \"" +
                            context.getDecryptedFileList().get(1).getAbsolutePath() +
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
                        boolean deleted = context.getCryptoFile().delete();
                        if (!deleted) {
                            context.setStatus(ProcessingStatus.FAILURE);
                        } else {
                            deleted = context.getDecryptedFileList().get(0).delete();
                            if (!deleted) {
                                context.setStatus(ProcessingStatus.FAILURE);
                            } else {
                                Path path = context.getDecryptedFileList().get(1).toPath();
                                DosFileAttributeView view = Files.getFileAttributeView(path, DosFileAttributeView.class);
                                if (view == null) {
                                    context.setStatus(ProcessingStatus.FAILURE);
                                } else {
                                    try {
                                        view.setArchive(false);
                                        view.setSystem(false);
                                        view.setReadOnly(false);
                                        view.setHidden(false);
                                        context.setStatus(ProcessingStatus.SUCCESS);
                                    } catch (IOException e) {
                                        context.setStatus(ProcessingStatus.FAILURE);
                                    }
                                }
                            }
                        }

                    }
                } else {
                    context.setStatus(ProcessingStatus.FAILURE);
                }
            } else {
                activateHumanCheck(command, context.getDecryptedFile());

                String message = "You have just saw decrypted(different) file \"" +
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
                    boolean deleted = context.getCryptoFile().delete();
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
                                context.setStatus(ProcessingStatus.SUCCESS);
                            } catch (IOException e) {
                                context.setStatus(ProcessingStatus.FAILURE);
                            }
                        }
                    }

                }
            }

        }
    }

    private void handleNotEncrypted(HelperContext context) {
        ExtensionCommand command = settings.getExtensionCommand(context.getExtension());
        if ((command != null) && !isStopFlagSet()) {
            activateHumanCheck(command, context.getEncryptedFile());

            String message = "You have just saw original file \"" +
                    context.getEncryptedFile().getAbsolutePath() +
                    "\", is the content OK and we leave it?";
            int ret = JOptionPane.showConfirmDialog(null, message,
                    "Leave the file?", JOptionPane.YES_NO_CANCEL_OPTION);
            //0 means YES, 1 means NO, 2 means CANCEL, -1 means user closed the dialog
            if (ret == 2 || ret == -1) {
                setStopFlag(); //eny attempt to close dialog without choosing means user wants to stop us
                context.setStatus(ProcessingStatus.REJECT);
            } else if (ret == 1) {
                context.setStatus(ProcessingStatus.REJECT);
            } else if (ret == 0) {
                boolean deleted = context.getCryptoFile().delete();
                if (!deleted) {
                    context.setStatus(ProcessingStatus.FAILURE);
                } else {
                    deleted = context.getDecryptedFile().delete();
                    if (deleted) {
                        context.setStatus(ProcessingStatus.SUCCESS);
                    } else {
                        context.setStatus(ProcessingStatus.FAILURE);
                    }
                }
            }
        }
    }

    /**
     * Method handles only multiple copies, leaving just single copy
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

    private EncryptionStatus determineEncryptionStatus(HelperContext context) {
        long origSize = context.getEncryptedFile().length();
        long decriptedSize = context.getDecryptedFile().length();
        long cryptoSize = context.getCryptoFile().length();
        boolean sourceEncrypted;
        boolean largeSource = false;
        if (origSize < settings.getMinimumEncryptedFileSize()) {
            sourceEncrypted = false;
        } else {
            sourceEncrypted = ensureEncryption(context.getEncryptedFile());
            largeSource = (origSize > settings.getMaximumReallyEncryptedFilePartSize());
        }
        boolean noRecoveryPossible = (cryptoSize == 0);
        boolean sourceSizeMatch = origSize == decriptedSize;
        boolean cryptoSizeMatch = cryptoSize == decriptedSize;
        boolean maxCryptoSize = cryptoSize == settings.getMaximumCryptoFileSize();
        if (sourceEncrypted) {
            if (noRecoveryPossible) {
                if (largeSource) {
                    if (sourceSizeMatch) {
                        if (cryptoSizeMatch) {
                            return EncryptionStatus.LOGICAL_ERROR;
                        } else {
                            return EncryptionStatus.POTENTIAL_MIRACLE;
                        }
                    } else {
                        if (cryptoSizeMatch) {
                            return EncryptionStatus.DECRYPTION_ERROR;
                        } else {
                            if (origSize - decriptedSize != settings.getMaximumReallyEncryptedFilePartSize()) {
                                return EncryptionStatus.DECRYPTION_ERROR;
                            }
                            if (ensureEndMatches(context.getEncryptedFile(), context.getDecryptedFile())) {
                                return EncryptionStatus.PROVED_NO_RECOVERY;
                            } else {
                                return EncryptionStatus.DECRYPTION_ERROR;
                            }
                        }
                    }
                } else {
                    if (sourceSizeMatch) {
                        if (cryptoSizeMatch) {
                            return EncryptionStatus.LOGICAL_ERROR;
                        } else {
                            return EncryptionStatus.POTENTIAL_MIRACLE;
                        }
                    } else {
                        if (cryptoSizeMatch) {
                            return EncryptionStatus.PROVED_NO_RECOVERY;
                        } else {
                            return EncryptionStatus.DECRYPTION_ERROR;
                        }
                    }
                }
            } else {
                if (largeSource && !maxCryptoSize) {
                    return EncryptionStatus.ENCRYPTION_ERROR;
                }
                if (sourceSizeMatch) {
                    if (cryptoSizeMatch) {
                        return EncryptionStatus.LOGICAL_ERROR;
                    } else {
                        return EncryptionStatus.OK;
                    }
                } else {
                    return EncryptionStatus.DECRYPTION_ERROR;
                }

            }
        } else {
            if (noRecoveryPossible) {
                if (largeSource) {
                    if (sourceSizeMatch) {
                        if (cryptoSizeMatch) {
                            return EncryptionStatus.LOGICAL_ERROR;
                        } else {
                            return EncryptionStatus.MIRACLE_DUPLICATE;
                        }
                    } else {
                        if (cryptoSizeMatch) {
                            return EncryptionStatus.DECRYPTION_ERROR_NOT_ENCRYPTED;
                        } else {
                            if (origSize - decriptedSize != settings.getMaximumReallyEncryptedFilePartSize()) {
                                return EncryptionStatus.DECRYPTION_ERROR_NOT_ENCRYPTED;
                            }
                            if (ensureEndMatches(context.getEncryptedFile(), context.getDecryptedFile())) {
                                return EncryptionStatus.NOT_ENCRYPTED;
                            } else {
                                return EncryptionStatus.DECRYPTION_ERROR_NOT_ENCRYPTED;
                            }
                        }
                    }
                } else {
                    if (sourceSizeMatch) {
                        if (cryptoSizeMatch) {
                            return EncryptionStatus.LOGICAL_ERROR;
                        } else {
                            return EncryptionStatus.MIRACLE_DUPLICATE;
                        }
                    } else {
                        if (cryptoSizeMatch) {
                            return EncryptionStatus.NOT_ENCRYPTED;
                        } else {
                            return EncryptionStatus.DECRYPTION_ERROR_NOT_ENCRYPTED;
                        }
                    }
                }
            } else {
                if (largeSource && !maxCryptoSize) {
                    return EncryptionStatus.ENCRYPTION_ERROR;
                }
                if (sourceSizeMatch) {
                    if (cryptoSizeMatch) {
                        return EncryptionStatus.LOGICAL_ERROR;
                    } else {
                        return EncryptionStatus.OK_DUPLICATE;
                    }
                } else {
                    return EncryptionStatus.OK_DIFFERENT_FILE;
                }
            }
        }
    }

    private boolean ensureEncryption(File file) {
        int totalBytesRead = 0;
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead = 0;
            while (bytesRead >= 0) {
                bytesRead = in.read(buffer);
                for (int i = 0; i < bytesRead; i++) {
                    if (totalBytesRead + i < settings.getMinimumEncryptedFileSize()) {
                        if (buffer[i] != settings.getEncryptedFileContentsStart()[totalBytesRead + i]) {
                            return false;
                        }
                    } else if (totalBytesRead + i >= settings.getMaximumReallyEncryptedFilePartSize()) {
                        return true;
                    } else {
                        if (buffer[i] != 0) {
                            return false;
                        }
                    }
                }
                if (bytesRead > 0) {
                    totalBytesRead += bytesRead;
                }
            }

        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private boolean ensureEndMatches(File original, File decrypted) {
        if (original.length() < settings.getMaximumReallyEncryptedFilePartSize()) {
            return false;
        }
        try (BufferedInputStream origStream = new BufferedInputStream(new FileInputStream(original));
             BufferedInputStream decrStream = new BufferedInputStream(new FileInputStream(decrypted))) {
            long rest = settings.getMaximumReallyEncryptedFilePartSize();
            while (rest > 0) {
                long skipped = origStream.skip(rest);
                rest -= skipped;
            }
            while (true) {
                int b1 = origStream.read();
                int b2 = decrStream.read();
                if (b1 == -1) {
                    return b2 == -1;
                } else {
                    if (b2 == -1) {
                        return false;
                    } else {
                        if (b2 != b1) {
                            return false;
                        }
                    }
                }
            }
        } catch (IOException e) {
            return false;
        }
    }

    private void activateHumanCheck(ExtensionCommand command, File file) {
        List<String> commandArguments = new ArrayList<>(3);
        if (command != null) {
            commandArguments.add(command.getCommand());
            for (String option : command.getOptions()) {
                commandArguments.add(option);
            }
        } else {
            throw new RuntimeException("Could not process unknown file type");
        }
        commandArguments.add(file.getAbsolutePath());
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
        System.out.println(statCollector.getEncryptionErrorFiles().size() + " files are with encryption errors");
        System.out.println(statCollector.getUnknownDecryptionErrorFiles().size() + " files are with unknown decryption errors");
        System.out.println(statCollector.getUnknownExtensionsFiles().size() + " files are of unknown type");
        System.out.println(statCollector.getNoRecoveryFiles().size() + " files are cannot be restored");
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
            switch (context.getStatus()) {
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
