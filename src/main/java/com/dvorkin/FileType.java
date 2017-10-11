package com.dvorkin;

/**
 * Created by dvorkin on 02.10.2017.
 */
public enum FileType {

    EMPTY(""),
    TEXT("txt"),
    CSV("csv"),
    SQL("sql"),
    LAYOUT("layout"),
    MSG("msg"),
    DXF("dxf"),
    FBX("fbx"),
    PDF("pdf"),
    ARCHIVE1("zip"),
    ARCHIVE2("rar"),
    ARCHIVE3("7z"),
    ARCHIVE4("iso"),
    XML("xml"),
    IMAGE1("jpg"),
    IMAGE2("jpeg"),
    EXCEL1("xls"),
    EXCEL2("xlsx"),
//    WORD1("doc"),
//    WORD2("docx"),
    WORD3("rtf"),
    AUDIO1("mp3"),
    VIDEO1("mp4"),
    VIDEO2("avi"),
    VIDEO3("mov"),
    VIDEO4("wmv"),
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
