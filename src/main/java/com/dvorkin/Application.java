package com.dvorkin;

import com.dvorkin.json.Settings;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by dvorkin on 30.09.2017.
 */
public class Application {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println(Constants.USAGE);
            return;
        }
        File dir = new File(args[0]);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println(Constants.USAGE);
            return;
        }
        Settings settings = loadSettings();
        System.out.println("Start scanning");
        DirectoryScanner ds = new DirectoryScanner(settings);
        List<File> cryptedFiles = ds.scanDirectory(dir);
        System.out.println("End scanning");
        FileProcessor fileProcessor = new FileProcessor(settings, cryptedFiles);
        fileProcessor.processFiles();
        fileProcessor.showStatistics();
    }

    private static Settings loadSettings() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        File file = new File("settings.json");
        Settings ret;
        if (file.exists()) {
            ret = mapper.readValue(file, Settings.class);
        } else {
            InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("settings.json");
            ret = mapper.readValue(in, Settings.class);
        }
        return ret;
    }
}
