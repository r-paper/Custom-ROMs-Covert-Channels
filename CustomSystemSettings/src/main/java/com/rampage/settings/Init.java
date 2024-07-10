package com.rampage.settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.G;
import soot.Scene;
import soot.jimple.infoflow.android.SetupApplication;
import soot.options.Options;

import java.util.Collections;

public class Init {
    protected static Logger logger = LoggerFactory.getLogger(Init.class);
    public static SetupApplication flowdroidInit(String apkPath) {
        G.reset();

        SetupApplication app = new SetupApplication(AppConfig.platformsPath, apkPath);

        Options.v().set_prepend_classpath(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_output_format(Options.output_format_none);
        Options.v().set_android_jars(AppConfig.platformsPath);
        Options.v().set_src_prec(Options.src_prec_apk);
        Options.v().set_process_dir(Collections.singletonList(apkPath));
        Options.v().set_whole_program(true);
        Options.v().set_process_multiple_dex(true);
        Options.v().setPhaseOption("cg.spark verbose:true", "on");
        Scene.v().addBasicClass("android.app.Service,HIERARCHY");

        return app;
    }

    public static void sootInit(String filePath) {
        G.reset();

        Options.v().set_process_multiple_dex(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_prepend_classpath(true);
        Options.v().set_output_format(Options.output_format_none);
        Options.v().set_android_jars(AppConfig.platformsPath);//设置android jar包路径
        if (filePath.endsWith(".dex")) {
            Options.v().set_src_prec(Options.src_prec_apk_class_jimple);
        }
        else if (filePath.endsWith(".apk")) {
            Options.v().set_src_prec(Options.src_prec_apk);
        }
        Options.v().set_process_dir(Collections.singletonList(filePath));
        Options.v().set_force_overwrite(true);
        Options.v().set_whole_program(true);
        Options.v().set_wrong_staticness(Options.wrong_staticness_ignore);

        try{
            Scene.v().loadNecessaryClasses();
        }catch (Exception e) {
            logger.error("loadNecessaryClasses: " + filePath);
            e.printStackTrace();
        }
    }
}
