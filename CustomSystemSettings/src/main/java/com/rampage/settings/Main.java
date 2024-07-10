package com.rampage.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rampage.settings.result.MyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.StopWatch;
import soot.PackManager;
import soot.Transform;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class Main {
    protected static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        StopWatch stopWatch = new StopWatch("Custom System Settings Analysis");
        stopWatch.start("Custom System Settings Analysis");

        CommandLineOptions options = new CommandLineOptions(args);

        // add command line options to AppConfig
        AppConfig.romPath = options.getRom();
        AppConfig.outputPath = options.getOutput();
        AppConfig.resultPath = options.getResult();
        AppConfig.timeout = options.getTimeout();
        AppConfig.platformsPath = options.getPlatforms();
        AppConfig.instanceNumber = options.getNumber();

        // create switch file
        File switchFile = new File(AppConfig.outputPath + File.separator + "switches" + File.separator + AppConfig.instanceNumber);
        try {
            if (!switchFile.createNewFile()) {
                logger.error("Create switch file failed: " + AppConfig.instanceNumber);
                return;
            }
        } catch (Exception e) {
            logger.error("Create switch file: " + e.getMessage());
        }

        // create resultPath
        Utils.createDirectoryIfNotExists(AppConfig.resultPath);

        AppConfig.romFilePaths = Utils.getFiles(new File(AppConfig.romPath));

        for (String romFile: AppConfig.romFilePaths) {
            boolean isTimedOut = false;
            // process apk files
            if (romFile.endsWith(".apk")) {
                String fileMD5 = Utils.calculateFileMD5(romFile);
                if (Utils.checkFinishedFile(fileMD5)) {
                    continue;
                }

                logger.info(Utils.getCurrentTime() + " Processing rom file -- " + romFile);
                AppConfig.currentAnalyzedFileType = "APK";

                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<?> future = executor.submit(() -> {
                    Init.sootInit(romFile);
                    AppConfig.fileResult = new MyResult(romFile);

                    PackManager.v().getPack("wjtp").add(new Transform("wjtp.MethodFeatureTransformer", new CoreAnalysis()));
                    try{
                        PackManager.v().runPacks();
                    }catch (Exception e) {
                        logger.error("CoreAnalysis");
                        e.printStackTrace();
                    }
                });
                try {
                    // 设置超时时间
                    future.get(AppConfig.timeout, TimeUnit.MINUTES);
                } catch (TimeoutException e) {
                    logger.info("Timed out -- " + romFile);
                    isTimedOut = true;
                    future.cancel(true);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    executor.shutdown();
                }
            }
            // process jar files
            else if (romFile.endsWith(".jar")) {
                String fileMD5 = Utils.calculateFileMD5(romFile);
                if (Utils.checkFinishedFile(fileMD5)) {
                    continue;
                }

                logger.info(Utils.getCurrentTime() + " Processing rom file -- " + romFile);
                AppConfig.currentAnalyzedFileType = "JAR";

                Utils.extractJarClasses(romFile);

                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<?> future = executor.submit(() -> {
                    AppConfig.fileResult = new MyResult(romFile);

                    for (String dexFile: Utils.getFiles(new File(AppConfig.outputPath + File.separator + AppConfig.instanceNumber))) {
                        Init.sootInit(dexFile);

                        PackManager.v().getPack("wjtp").add(new Transform("wjtp.MethodFeatureTransformer", new CoreAnalysis()));
                        try{
                            PackManager.v().runPacks();
                        }catch (Exception e) {
                            logger.error("CoreAnalysis");
                            e.printStackTrace();
                        }
                    }
                });
                try {
                    // 设置超时时间
                    future.get(AppConfig.timeout, TimeUnit.MINUTES);
                } catch (TimeoutException e) {
                    logger.info("Timed out -- " + romFile);
                    isTimedOut = true;
                    future.cancel(true);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    executor.shutdown();
                }

                Utils.deleteFolder(AppConfig.outputPath + File.separator + AppConfig.instanceNumber);
            }
            else {
                continue;
            }

            if (!isTimedOut) {
                Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                String fileResultJsonString = gson.toJson(AppConfig.fileResult);

                try {
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(AppConfig.resultPath + File.separator + Utils.getCurrentTime(), false));
                    bos.write(fileResultJsonString.getBytes(StandardCharsets.UTF_8));
                    bos.flush();
                    bos.close();

                    logger.info(Utils.getCurrentTime() + " Write result file done -- " + romFile);
                } catch (Exception e) {
                    logger.error(Utils.getCurrentTime() + " Write result file error -- " + romFile);
                    e.printStackTrace();
                }
            }

            AppConfig.resetTempConfig();
        }

        stopWatch.stop();

        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(AppConfig.timeFilePath, true));
            String logContent = AppConfig.romPath + " -- " + String.valueOf(stopWatch.elapsedTime() / 1000000000) + "\n";
            bos.write(logContent.getBytes());
            bos.flush();
            bos.close();
            logger.info(Utils.getCurrentTime() + " Write time.txt done");
        } catch (Exception e) {
            logger.error(Utils.getCurrentTime() + " Write time.txt error");
            e.printStackTrace();
        }

        // spare instance number
        if (switchFile.delete()) {
            logger.info(Utils.getCurrentTime() + " Delete switch file done -- " + AppConfig.outputPath + File.separator + "switches" + File.separator + AppConfig.instanceNumber);
        }
        else {
            logger.error(Utils.getCurrentTime() + " Delete switch file failed -- " + AppConfig.outputPath + File.separator + "switches" + File.separator + AppConfig.instanceNumber);
        }
    }
}

