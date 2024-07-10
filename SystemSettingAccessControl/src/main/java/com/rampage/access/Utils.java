package com.rampage.access;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Utils {
    public static List<String> getFiles(File rootDir) {
        List<String> filePathList = new ArrayList<>();

        if (rootDir.isDirectory() && !rootDir.getName().endsWith("_extracted")) {
            File[] files = rootDir.listFiles();
            if (files != null){
                for (File file: files) {
                    if (file.isDirectory()) {
                        List<String> subResult = getFiles(file);
                        filePathList.addAll(subResult);
                    }
                    else if (file.isFile()) {
                        filePathList.add(file.getAbsolutePath());
                    }
                }
            }
        }
        return filePathList;
    }

    public static void createDirectoryIfNotExists(String directoryPath) {
        Path path = Paths.get(directoryPath);

        try {
            if (!Files.exists(path)) {
                Files.createDirectory(path);
            }
        } catch (Exception e) {
            System.err.println("Directory creation failed. Error: " + e.getMessage());
        }
    }

    public static void extractJarClasses(String jarFilePath) {
        try {
            JarFile jarFile = new JarFile(jarFilePath);
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                String entryName = jarEntry.getName();

                if (entryName.endsWith(".dex")) {
                    String outputPath = AppConfig.outputPath + File.separator + AppConfig.instanceNumber + File.separator + entryName;
                    File outputFile = new File(outputPath);

                    // make dir if needed
                    if (!outputFile.getParentFile().exists()) {
                        outputFile.getParentFile().mkdirs();
                    }

                    // extract dex from jar file
                    InputStream inputStream = jarFile.getInputStream(jarEntry);
                    FileOutputStream outputStream = new FileOutputStream(outputFile);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
            }
            jarFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteFolder(String folderPath){
        String deleteCommand = "rm -r " + folderPath;
        Utils.executeLinuxCmd(deleteCommand);
    }

    public static List<String> executeLinuxCmd(String cmd) {
        if (!AppConfig.currentOs.equals("Linux")) {
            return null;
        }
        List<String> list = new ArrayList<>();

        ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c", cmd);
        processBuilder.redirectErrorStream(true); // This is the important line

        try {
            Process process = processBuilder.start();
            InputStream in = process.getInputStream();
            BufferedReader bs = new BufferedReader(new InputStreamReader(in));
            String result;
            while ((result = bs.readLine()) != null) {
                list.add(result);
            }
            in.close();
            process.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static String getCurrentTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM_dd_HH_mm_ss_SSS");
        return now.format(formatter);
    }
}
