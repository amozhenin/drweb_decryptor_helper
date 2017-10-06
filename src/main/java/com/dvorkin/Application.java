package com.dvorkin;

import java.io.File;
import java.util.List;

/**
 * Created by dvorkin on 30.09.2017.
 */
public class Application {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println(Constants.USAGE);
            return;
        }
        File dir = new File(args[0]);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println(Constants.USAGE);
            return;
        }
        System.out.println("Start scanning");
        DirectoryScanner ds = new DirectoryScanner();
        List<File> cryptedFiles = ds.scanDirectory(dir);
        System.out.println("End scanning");
        FileProcessor fileProcessor = new FileProcessor(cryptedFiles);
        fileProcessor.processFiles();
        fileProcessor.showStatistics();
    }
}
