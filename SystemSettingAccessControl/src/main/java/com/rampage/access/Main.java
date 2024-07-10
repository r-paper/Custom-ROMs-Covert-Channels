package com.rampage.access;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.StopWatch;
import soot.PackManager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class Main {
    protected static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        StopWatch stopWatch = new StopWatch("System Setting Access Control");
        stopWatch.start("System Setting Access Control");

        CommandLineOptions options = new CommandLineOptions(args);

        // add command line options to AppConfig
        AppConfig.apkPath = options.getApk();
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

        if (AppConfig.apkPath.endsWith(".jar")) {
            boolean isTimedOut = false;

            logger.info(Utils.getCurrentTime() + " Processing rom file -- " + AppConfig.apkPath);

            Utils.extractJarClasses(AppConfig.apkPath);

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<?> future = executor.submit(() -> {
                for (String dexFile: Utils.getFiles(new File(AppConfig.outputPath + File.separator + AppConfig.instanceNumber))) {
                    Init.sootInit(dexFile);

                    try{
                        PackManager.v().runPacks();
                    }catch (Exception e) {
                        logger.error("CoreAnalysis");
                        e.printStackTrace();
                    }

                    CoreAnalysis.analyze();
                }
            });
            try {
                // 设置超时时间
                future.get(AppConfig.timeout, TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                logger.info("Timed out -- " + AppConfig.apkPath);
                isTimedOut = true;
                future.cancel(true);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                executor.shutdown();
            }

            Utils.deleteFolder(AppConfig.outputPath + File.separator + AppConfig.instanceNumber);

            if (!isTimedOut) {
                Map<String, String> filePath = new HashMap<>();
                filePath.put("filePath", AppConfig.apkPath);
                AppConfig.fileResult.add(0, filePath);

                Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                String fileResultJsonString = gson.toJson(AppConfig.fileResult);

                try {
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(AppConfig.resultPath + File.separator + Utils.getCurrentTime(), false));
                    bos.write(fileResultJsonString.getBytes(StandardCharsets.UTF_8));
                    bos.flush();
                    bos.close();

                    logger.info(Utils.getCurrentTime() + " Write result file done -- " + AppConfig.apkPath);
                } catch (Exception e) {
                    logger.error(Utils.getCurrentTime() + " Write result file error -- " + AppConfig.apkPath);
                    e.printStackTrace();
                }
            }
        }

        stopWatch.stop();

        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(AppConfig.timeFilePath, true));
            String logContent = AppConfig.apkPath + " -- " + String.valueOf(stopWatch.elapsedTime() / 1000000000) + "\n";
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
