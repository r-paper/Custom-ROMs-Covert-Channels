package com.rampage.access;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AppConfig {
    public static String apkPath;
    public static String outputPath;
    public static String resultPath;
    public static int timeout;
    public static String platformsPath;
    public static int instanceNumber;
    public static String currentOs;
    public static String timeFilePath;
    public static String finishedFileFilePath;
    public static List<String> targetClass = new ArrayList<>();
    public static List<Map<String, String>> fileResult = new ArrayList<>();

    static {
        // init targetClass
        targetClass.add("android.provider.Settings$Global");
        targetClass.add("android.provider.Settings$Secure");
        targetClass.add("android.provider.Settings$System");

        // init timeFilePath
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            currentOs = "Windows";
            timeFilePath = "D:\\Temp\\testDir\\time.txt";
        } else if (os.contains("nux")) {
            currentOs = "Linux";
            timeFilePath = "/home/dongzikan/workdir/result/time.txt";
        }
    }
}
