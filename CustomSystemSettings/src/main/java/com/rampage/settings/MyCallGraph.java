package com.rampage.settings;

import org.slf4j.profiler.StopWatch;
import soot.*;
import soot.jimple.Stmt;
import soot.util.Chain;

import java.util.*;

public class MyCallGraph{
    public static void construct(){
        StopWatch stopWatch = new StopWatch("Custom System Properties Analysis");
        stopWatch.start("Custom System Properties Analysis");

        Map<SootMethod, List<Map<String, Object>>> callGraph = new HashMap<>();

        Chain<SootClass> sootClasses = Scene.v().getApplicationClasses();
        for (Iterator<SootClass> classIterator = sootClasses.snapshotIterator(); classIterator.hasNext();) {
            SootClass sootClass = classIterator.next();
            List<SootMethod> snapshot = new ArrayList<>(sootClass.getMethods());
            for (Iterator<SootMethod> iterator = snapshot.iterator(); iterator.hasNext(); ) {
                SootMethod srcMethod = iterator.next();
                if (!srcMethod.isConcrete()) {
                    continue;
                }
                Body body = srcMethod.retrieveActiveBody();
                for (Unit unit : body.getUnits()) {
                    Stmt stmt = (Stmt) unit;
                    if (stmt.containsInvokeExpr()) {
                        SootMethod tgtMethod = stmt.getInvokeExpr().getMethod();
                        Map<String, Object> srcMap = new HashMap<>();
                        srcMap.put("src", srcMethod);
                        srcMap.put("srcStmt", stmt);
                        if (!callGraph.containsKey(tgtMethod)) {
                            callGraph.put(tgtMethod, new ArrayList<>());
                        }
                        callGraph.get(tgtMethod).add(srcMap);
                    }
                }
            }
        }
        AppConfig.myCallGraph = callGraph;

        stopWatch.stop();
        System.out.println(stopWatch.elapsedTime() / 1000000000);
    }
}
