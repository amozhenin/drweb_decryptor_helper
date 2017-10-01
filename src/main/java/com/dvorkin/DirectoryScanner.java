package com.dvorkin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by dvorkin on 30.09.2017.
 */
public class DirectoryScanner {

    public List<File> scanDirectory(File scanRoot) {
        List<File> result = new ArrayList<>();
        if (scanRoot.isDirectory()) {
            result.addAll(recursivelyScan(scanRoot));
        }
        return result;
    }

    private List<File> recursivelyScan(File dir) {
        List<File> ret = new ArrayList<>();
        if (!dir.canRead()) {
            return ret;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            System.out.println("Something strange in " + dir.getAbsolutePath());
            return ret;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                ret.addAll(recursivelyScan(child));
            } else if (child.getName().toUpperCase().endsWith(Constants.ENCRYPTED_FILE_EXTENSION)) {
                ret.add(child);
            }
        }
        return ret;
    }
}
