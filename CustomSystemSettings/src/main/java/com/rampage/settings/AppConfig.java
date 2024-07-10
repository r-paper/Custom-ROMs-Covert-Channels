package com.rampage.settings;

import com.rampage.settings.result.MyResult;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;

import java.util.List;
import java.util.Map;

public class AppConfig {
    public static String romPath;
    public static String outputPath;
    public static String resultPath;
    public static int timeout;
    public static String platformsPath;
    public static int instanceNumber;
    public static String timeFilePath;
    public static String currentOs;
    public static String currentAnalyzedFileType;
    public static List<String> romFilePaths;
    public static String finishedFileFilePath;
    public static CallGraph callGraph = null;
    public static Map<SootMethod, List<Map<String, Object>>> myCallGraph = null;
    public static SootMethod fieldDefMethod = null;
    public static SootMethod fieldUseMethod = null;
    public static MyResult fileResult = null;


    static {
        // init timeFilePath
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            currentOs = "Windows";
            timeFilePath = "D:\\Temp\\testDir\\time.txt";
            finishedFileFilePath = "D:\\workDir\\custom_system_properties\\finished_files.txt";
        } else if (os.contains("nux")) {
            currentOs = "Linux";
            timeFilePath = "/home/dongzikan/workdir/result/time.txt";
            finishedFileFilePath = "/home/dongzikan/workdir/finished_files.txt";
        }
    }

    public static void resetTempConfig() {
        currentAnalyzedFileType = "";
        callGraph = null;
        myCallGraph = null;
        fieldDefMethod = null;
        fieldUseMethod = null;
    }
}

