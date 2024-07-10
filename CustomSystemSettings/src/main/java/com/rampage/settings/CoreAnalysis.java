package com.rampage.settings;

import com.rampage.settings.result.ResultCallGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.util.*;

public class CoreAnalysis extends SceneTransformer {
    protected static Logger logger = LoggerFactory.getLogger(CoreAnalysis.class);
    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        Chain<SootClass> sootClasses = Scene.v().getClasses();
        for (Iterator<SootClass> classIterator = sootClasses.snapshotIterator(); classIterator.hasNext();) {
            SootClass sootClass = classIterator.next();
            List<SootMethod> snapshot = new ArrayList<>(sootClass.getMethods());
            for (Iterator<SootMethod> methodIterator = snapshot.iterator(); methodIterator.hasNext(); ) {
                SootMethod method = methodIterator.next();
                if (method.isConcrete()) {
//                if (method.isConcrete() && method.getSignature().contains("<android.provider.Settings:>")) {
//                if (method.isConcrete() && method.getSignature().contains("com.rampage.custompropertytestcases.MainActivity: void caseShell")) {
                    Body body = method.retrieveActiveBody();
                    for (Unit unit : body.getUnits()) {
                        Stmt stmt = (Stmt) unit;
                        if (stmt.containsInvokeExpr()) {
                            InvokeExpr invokeExpr = stmt.getInvokeExpr();
                            if (invokeExpr instanceof StaticInvokeExpr) {
                                SootClass invokedClass = invokeExpr.getMethod().getDeclaringClass();
                                if (invokedClass.getName().startsWith("android.provider.Settings")) {
                                    if (invokeExpr.getArgs().size() >= 2) {
                                        Map<String, Object> detail = new HashMap<>();
                                        detail.put("propertyMap", new HashMap<String, List<String>>());
                                        detail.put("callGraph", new ResultCallGraph(method.getSignature()));

                                        // system property name
                                        Value firstArg = invokeExpr.getArg(1);
                                        List<String> propertyNames = DataFlowAnalysis.retrieveStringValue(body, stmt, firstArg, new ArrayList<>(), true, detail);

                                        if (!propertyNames.isEmpty()) {
                                            // check
                                            Map<String, List<String>> propertyMap = (Map<String, List<String>>) detail.get("propertyMap");
                                            for (String propertyName: propertyNames) {
                                                if (!propertyMap.containsKey(propertyName)) {
                                                    System.out.println("Wrong1");
                                                }
                                            }

                                            AppConfig.fileResult.addMethod(method);
                                            AppConfig.fileResult.addDetail(detail);
                                        }
                                    }
                                    else if (invokeExpr.getMethod().getName().equals("getUriFor")) {
                                        Map<String, Object> detail = new HashMap<>();
                                        detail.put("propertyMap", new HashMap<String, List<String>>());
                                        detail.put("callGraph", new ResultCallGraph(method.getSignature()));

                                        // system property name
                                        Value firstArg = invokeExpr.getArg(0);
                                        List<String> propertyNames = DataFlowAnalysis.retrieveStringValue(body, stmt, firstArg, new ArrayList<>(), true, detail);

                                        if (!propertyNames.isEmpty()) {
                                            // check
                                            Map<String, List<String>> propertyMap = (Map<String, List<String>>) detail.get("propertyMap");
                                            for (String propertyName: propertyNames) {
                                                if (!propertyMap.containsKey(propertyName)) {
                                                    System.out.println("Wrong1");
                                                }
                                            }

                                            AppConfig.fileResult.addMethod(method);
                                            AppConfig.fileResult.addDetail(detail);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
