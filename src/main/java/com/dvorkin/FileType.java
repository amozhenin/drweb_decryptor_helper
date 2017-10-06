package com.dvorkin;

/**
 * Created by dvorkin on 02.10.2017.
 */
public enum FileType {

    EMPTY(""),
    TEXT("txt"),
    PDF("pdf"),
    ARCHIVE("zip"),
    XML("xml"),
//    IMAGE("jpg"),
//    VIDEO("xxx"),
    UNKNOWN("zzzzzz");

    FileType(String extension) {
        this.extension = extension;
    }

    private String extension;

    public String getExtension() {
        return this.extension;
    }

    public static FileType getByExtension(String extension) {
        for (FileType type : values()) {
            if (type.getExtension().equalsIgnoreCase(extension)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
