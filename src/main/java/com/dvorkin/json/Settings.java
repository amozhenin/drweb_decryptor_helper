package com.dvorkin.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Created by dvorkin on 15.10.2017.
 */
public class Settings {

    @JsonProperty("encrypted_file_extension")
    private String encryptedFileExtension;

    @JsonProperty("encrypted_file_contents_start_string")
    private String encryptedFileContentsStartString;

    @JsonProperty("maximum_really_encrypted_file_part_size")
    private long maximumReallyEncryptedFilePartSize;

    @JsonProperty("maximum_crypto_file_size")
    private long maximumCryptoFileSize;

    @JsonProperty("extension_commands")
    private Map<String, ExtensionCommand> extensionCommands;

    public String getEncryptedFileExtension() {
        return encryptedFileExtension.toUpperCase();
    }

    public String getEncryptedFileContentsStartString() {
        return encryptedFileContentsStartString;
    }

    public byte[] getEncryptedFileContentsStart() {
        try {
            return encryptedFileContentsStartString.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Cannot convert string to bytes", e);
        }
    }

    public long getMinimumEncryptedFileSize() {
        return getEncryptedFileContentsStart().length;
    }

    public long getMaximumReallyEncryptedFilePartSize() {
        return maximumReallyEncryptedFilePartSize;
    }

    public long getMaximumCryptoFileSize() {
        return maximumCryptoFileSize;
    }

    public ExtensionCommand getExtensionCommand(String extension) {
        return extensionCommands.get(extension);
    }
}
