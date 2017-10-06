package com.dvorkin;

import java.io.UnsupportedEncodingException;

/**
 * Created by dvorkin on 30.09.2017.
 */
public class Constants {

    public static final String USAGE = "java -jar helper.jar <directory>";
    public static final String ENCRYPTED_FILE_EXTENSION = ".SN-3257977921942051-kiaracript@gmail.com".toUpperCase();
    public static final String ENCRYPTED_FILE_CONTENTS_START_STRING = "{F39SN97D-K73M-YLR9-1I59-YW9R799VKF}.SN-3257977921942051-kiaracript@gmail.com";
    public static final byte[] ENCRYPTED_FILE_CONTENTS_START;
    public static final long MAXIMUM_REALLY_ENCRYPTED_FILE_PART_SIZE = 0x2D5D3B;
    public static final long MINIMUM_ENCRYPTED_FILE_SIZE = ENCRYPTED_FILE_CONTENTS_START_STRING.length();
    public static final long MAXIMUM_CRYPTO_FILE_SIZE = 2972996;


    static {
        byte[] contents;
        try {
            contents = ENCRYPTED_FILE_CONTENTS_START_STRING.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            contents = null;
            System.out.println("UTF-8 should always present acccordint to Java spec ...");
            e.printStackTrace();
            System.exit(1);
        }
        ENCRYPTED_FILE_CONTENTS_START = contents;
    }

}
