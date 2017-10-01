package com.dvorkin;

import java.io.UnsupportedEncodingException;

/**
 * Created by dvorkin on 30.09.2017.
 */
public class Constants {

    public static final String USAGE = "java -jar helper.jar <directory>";
    public static final String ENCRYPTED_FILE_EXTENSION = ".SN-3257977921942051-kiaracript@gmail.com".toUpperCase();
    public static final byte[] ENCRYPTED_FILE_CONTENTS;
    static {
        byte[] contents;
        try {
            contents = "{F39SN97D-K73M-YLR9-1I59-YW9R799VKF}.SN-3257977921942051-kiaracript@gmail.com".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            contents = null;
            System.out.println("UTF-8 should always present acccordint to Java spec ...");
            e.printStackTrace();
            System.exit(1);
        }
        ENCRYPTED_FILE_CONTENTS = contents;
    }

}
