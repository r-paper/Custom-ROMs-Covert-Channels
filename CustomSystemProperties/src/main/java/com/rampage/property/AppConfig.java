package com.rampage.property;

import com.rampage.property.result.MyResult;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
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
    public static List<String> propertyPrefixes = new ArrayList<>();
    public static List<String> acceptedApis = new ArrayList<>();
    public static List<String> acceptedClasses = new ArrayList<>();
    private static String propertiesInAospFilePath;
    public static String finishedFileFilePath;
    public static List<String> propertiesInAosp = new ArrayList<>();
    public static CallGraph callGraph = null;
    public static Map<SootMethod, List<Map<String, Object>>> myCallGraph = null;
    public static SootMethod fieldDefMethod = null;
    public static SootMethod fieldUseMethod = null;
    public static MyResult fileResult = null;

    static {
        // init propertyPrefixes
        propertyPrefixes.add("gsm.");
        propertyPrefixes.add("ro.");
        propertyPrefixes.add("persist.");
        propertyPrefixes.add("ril.");
        propertyPrefixes.add("sys.");
        propertyPrefixes.add("vendor.");

        // add acceptedApis
        acceptedApis.add("<android.os.SystemProperties: java.lang.String get(java.lang.String,java.lang.String)>");
        acceptedApis.add("<android.os.SystemProperties: java.lang.String get(java.lang.String)>");
        acceptedApis.add("<android.os.SystemProperties: void set(java.lang.String,java.lang.String)>");
        acceptedApis.add("<android.os.SemSystemProperties: java.lang.String get(java.lang.String,java.lang.String)>");
        acceptedApis.add("<android.os.SemSystemProperties: java.lang.String get(java.lang.String)>");
        acceptedApis.add("<android.os.SemSystemProperties: void set(java.lang.String,java.lang.String)>");

        // add acceptedClasses
        acceptedClasses.add("android.os.SystemProperties");
        acceptedClasses.add("android.os.SemSystemProperties");

        // init timeFilePath
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            currentOs = "Windows";
            timeFilePath = "D:\\Temp\\testDir\\time.txt";
            propertiesInAospFilePath = "D:\\workDir\\custom_system_properties\\properties_in_aosp.txt";
            finishedFileFilePath = "D:\\workDir\\custom_system_properties\\finished_files.txt";
        } else if (os.contains("nux")) {
            currentOs = "Linux";
            timeFilePath = "/home/dongzikan/workdir/result/time.txt";
            propertiesInAospFilePath = "/home/dongzikan/workdir/properties_in_aosp.txt";
            finishedFileFilePath = "/home/dongzikan/workdir/finished_files.txt";
        }

        // init propertiesInAosp
        try (BufferedReader reader = new BufferedReader(new FileReader(propertiesInAospFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                propertiesInAosp.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void resetTempConfig() {
        currentAnalyzedFileType = "";
        callGraph = null;
        myCallGraph = null;
        fileResult = null;
        fieldDefMethod = null;
        fieldUseMethod = null;
    }
}
