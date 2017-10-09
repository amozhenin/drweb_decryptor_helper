package com.dvorkin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by dvorkin on 02.10.2017.
 */
public class StatisticsCollector {

    private Set<String> unknownExtensions;

    private List<HelperContext> missingEncryptedFiles;
    private List<HelperContext> missingDecryptedFiles;
    private List<HelperContext> unknownExtensionsFiles;
    private List<HelperContext> notRealEncryptionFiles;

    private List<HelperContext> notProcessedEntries;
    private List<HelperContext> successfullyProcessedEntries;
    private List<HelperContext> rejectedEntries;
    private List<HelperContext> failedEntries;
    private int additionalCopies;
    private int removedCopies;

    private List<HelperContext> strangeFileEntries;

    public StatisticsCollector() {
        additionalCopies = 0;
        removedCopies = 0;
        unknownExtensions = new TreeSet<>();
        missingEncryptedFiles = new ArrayList<>();
        missingDecryptedFiles = new ArrayList<>();
        unknownExtensionsFiles = new ArrayList<>();
        notRealEncryptionFiles = new ArrayList<>();

        notProcessedEntries = new ArrayList<>();
        successfullyProcessedEntries = new ArrayList<>();
        rejectedEntries = new ArrayList<>();
        failedEntries = new ArrayList<>();

        strangeFileEntries = new ArrayList<>();
    }

    public void registerUnknownExtension(String extension, HelperContext context) {
        unknownExtensions.add(extension);
        unknownExtensionsFiles.add(context);
    }

    public void registerMissingEncryptedFile(HelperContext context) {
        missingEncryptedFiles.add(context);
    }

    public void registerMissingDecryptedFile(HelperContext context) {
        missingDecryptedFiles.add(context);
    }

    public void registerNotRealEncryption(HelperContext context){
        notRealEncryptionFiles.add(context);
    }

    public Set<String> getUnknownExtensions() {
        return unknownExtensions;
    }

    public List<HelperContext> getMissingEncryptedFiles() {
        return missingEncryptedFiles;
    }

    public List<HelperContext> getMissingDecryptedFiles() {
        return missingDecryptedFiles;
    }

    public List<HelperContext> getNotReallyEncryptedFiles() {
        return notRealEncryptionFiles;
    }

    public List<HelperContext> getUnknownExtensionsFiles() {
        return unknownExtensionsFiles;
    }

    public void registerNotProcessedEntry(HelperContext context) {
        notProcessedEntries.add(context);
    }

    public void registerSuccessfullyProcessedEntry(HelperContext context) {
        successfullyProcessedEntries.add(context);
    }

    public void registerRejectedEntry(HelperContext context) {
        rejectedEntries.add(context);
    }

    public void registerFailedEntry(HelperContext context) {
        failedEntries.add(context);
    }

    public List<HelperContext> getNotProcessedEntries() {
        return notProcessedEntries;
    }

    public List<HelperContext> getSuccessfullyProcessedEntries() {
        return successfullyProcessedEntries;
    }

    public List<HelperContext> getRejectedEntries() {
        return rejectedEntries;
    }

    public List<HelperContext> getFailedEntries() {
        return failedEntries;
    }

    public void registerAdditionalCopies(int copiesNumber) {
        if (copiesNumber > 0) {
            additionalCopies += copiesNumber;
        }
    }

    public int getAdditionalCopiesNumber() {
        return additionalCopies;
    }

    public void incrementRemovedCopiesNumber() {
        removedCopies++;
    }

    public int getRemovedCopiesNumber() {
        return removedCopies;
    }

    public void registerStrangeFile(HelperContext context) {
        strangeFileEntries.add(context);
    }

    public List<HelperContext> getStrangeFiles() {
        return strangeFileEntries;
    }
}
