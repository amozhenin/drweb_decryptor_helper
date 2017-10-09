package com.dvorkin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to store various data and status of the process
 * Created by dvorkin on 01.10.2017.
 */
public class HelperContext {


    //This is the original file that is now corrupted
    private File encryptedFile;

    //This file is created by DrWeb when it is decrypted
    private List<File> decryptedFileList;

    //This is the file created by the virus. We are searching for them
    private File cryptoFile;

    private FileType fileType;
    private ProcessingStatus status;

    public HelperContext() {
        status = ProcessingStatus.NOT_PROCESSED;
        decryptedFileList = new ArrayList<>(1);
    }


    public File getEncryptedFile() {
        return encryptedFile;
    }

    public void setEncryptedFile(File encryptedFile) {
        this.encryptedFile = encryptedFile;
    }

    public List<File> getDecryptedFileList() {
        return decryptedFileList;
    }

    public void addDecryptedFile(File decryptedFile) {
        this.decryptedFileList.add(decryptedFile);
    }

    public File getDecryptedFile() {
        if (decryptedFileList.size() != 1) {
            throw new RuntimeException("Precondition failed, size is " + decryptedFileList.size());
        }
        return decryptedFileList.get(0);
    }

    public File getCryptoFile() {
        return cryptoFile;
    }

    public void setCryptoFile(File cryptoFile) {
        this.cryptoFile = cryptoFile;
    }

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public ProcessingStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessingStatus status) {
        this.status = status;
    }
}
