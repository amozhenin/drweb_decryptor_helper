package com.dvorkin;

import java.io.File;

/**
 * This class is used to store various data and status of the process
 * Created by dvorkin on 01.10.2017.
 */
public class HelperContext {


    //This is the original file that is now corrupted
    private File encryptedFile;

    //This file is created by DrWeb when it is decrypted
    private File decryptedFile;

    //This is the file created by the virus. We are searching for them
    private File cryptoFile;

    private FileType fileType;
    private ProcessingStatus status;

    public HelperContext() {
        status = ProcessingStatus.NOT_PROCESSED;
    }


    public File getEncryptedFile() {
        return encryptedFile;
    }

    public void setEncryptedFile(File encryptedFile) {
        this.encryptedFile = encryptedFile;
    }

    public File getDecryptedFile() {
        return decryptedFile;
    }

    public void setDecryptedFile(File decryptedFile) {
        this.decryptedFile = decryptedFile;
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
