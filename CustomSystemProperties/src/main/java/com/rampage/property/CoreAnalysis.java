package com.rampage.property;

import com.rampage.property.result.ResultCallGraph;
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
//                if (method.isConcrete() && method.getSignature().contains("com.android.server.csdk.CSDKManagerService: java.lang.String getSerialNumber")) {
//                if (method.isConcrete() && method.getSignature().contains("com.rampage.custompropertytestcases.MainActivity: void caseShell")) {
                    Body body = method.retrieveActiveBody();
                    for (Unit unit: body.getUnits()) {
                        Stmt stmt = (Stmt) unit;
                        if (stmt.containsInvokeExpr()) {
                            InvokeExpr invokeExpr = stmt.getInvokeExpr();
                            if (invokeExpr instanceof StaticInvokeExpr) {
                                SootMethod invokedMethod = invokeExpr.getMethod();
                                if (AppConfig.acceptedClasses.contains(invokedMethod.getDeclaringClass().getName())) {
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
                                else if (invokedMethod.getSignature().equals("<java.lang.Class: java.lang.Class forName(java.lang.String)>")) {
                                    Value firstArg = invokeExpr.getArg(0);
                                    // TODO: filter method name
                                    // TODO: add inter procedure of reflectedClass OK
                                    Map<String, Object> detail = new HashMap<>();
                                    detail.put("propertyMap", new HashMap<String, List<String>>());
                                    detail.put("callGraph", new ResultCallGraph(method.getSignature()));
                                    List<String> reflectedClassNames = DataFlowAnalysis.retrieveStringValue(body, stmt, firstArg, new ArrayList<>(), false, detail);
                                    if (!reflectedClassNames.isEmpty() && AppConfig.acceptedClasses.stream().anyMatch(reflectedClassNames::contains)) {
                                        if (stmt instanceof AssignStmt) {
                                            // confirmed and retrieve property name
                                            List<String> propertyNames = DataFlowAnalysis.retrieveReflectedArgs(body, stmt, new ArrayList<>(), true, detail);
                                            if (!propertyNames.isEmpty()) {
                                                // check
                                                Map<String, List<String>> propertyMap = (Map<String, List<String>>) detail.get("propertyMap");
                                                for (String propertyName: propertyNames) {
                                                    if (!propertyMap.containsKey(propertyName)) {
                                                        System.out.println("Wrong2");
                                                    }
                                                }

                                                AppConfig.fileResult.addMethod(method);
                                                AppConfig.fileResult.addDetail(detail);
                                            }
                                        }
                                    }
                                }
                            }
                            else if (invokeExpr instanceof VirtualInvokeExpr) {
                                SootMethod invokedMethod = invokeExpr.getMethod();
                                if (invokedMethod.getSignature().equals("<java.lang.ClassLoader: java.lang.Class loadClass(java.lang.String)>")) {
                                    Value firstArg = invokeExpr.getArg(0);
                                    // TODO: filter method name
                                    // TODO: add inter procedure of reflectedClass OK
                                    Map<String, Object> detail = new HashMap<>();
                                    detail.put("propertyMap", new HashMap<String, List<String>>());
                                    detail.put("callGraph", new ResultCallGraph(method.getSignature()));
                                    List<String> reflectedClassNames = DataFlowAnalysis.retrieveStringValue(body, stmt, firstArg, new ArrayList<>(), false, detail);
                                    if (!reflectedClassNames.isEmpty() && AppConfig.acceptedClasses.stream().anyMatch(reflectedClassNames::contains)) {
                                        if (stmt instanceof AssignStmt) {
                                            // confirmed and retrieve property name
                                            List<String> propertyNames = DataFlowAnalysis.retrieveReflectedArgs(body, stmt, new ArrayList<>(), true, detail);
                                            if (!propertyNames.isEmpty()) {
                                                // check
                                                Map<String, List<String>> propertyMap = (Map<String, List<String>>) detail.get("propertyMap");
                                                for (String propertyName: propertyNames) {
                                                    if (!propertyMap.containsKey(propertyName)) {
                                                        System.out.println("Wrong3");
                                                    }
                                                }

                                                AppConfig.fileResult.addMethod(method);
                                                AppConfig.fileResult.addDetail(detail);
                                            }
                                        }
                                    }
                                }
                                // TODO: need check
                                else if (invokedMethod.getSignature().equals("<java.lang.Runtime: java.lang.Process exec(java.lang.String)>")) {
                                    Map<String, Object> detail = new HashMap<>();
                                    detail.put("propertyMap", new HashMap<String, List<String>>());
                                    detail.put("callGraph", new ResultCallGraph(method.getSignature()));

                                    Value firstArg = invokeExpr.getArg(0);
                                    List<String> shellCommands = DataFlowAnalysis.retrieveStringValue(body, stmt, firstArg, new ArrayList<>(), true, detail);
                                    for (String shellCommand: shellCommands) {
                                        if (shellCommand.startsWith("getprop ") ) {
                                            String propertyName = shellCommand.replace("getprop ", "");

                                            // check
                                            Map<String, List<String>> propertyMap = (Map<String, List<String>>) detail.get("propertyMap");
                                            if (!propertyMap.containsKey(shellCommand)) {
                                                System.out.println("Wrong4");
                                            }

                                            AppConfig.fileResult.addMethod(method);
                                            AppConfig.fileResult.addDetail(detail);
                                        }
                                    }
                                }
                            }

                            if (stmt instanceof AssignStmt
                                    && (((AssignStmt) stmt).getLeftOp().getType().toString().equals("java.lang.Class")
                                    || ((AssignStmt) stmt).getLeftOp().getType().toString().equals("java.lang.reflect.Method"))
                                    && invokeExpr.getMethod().isConcrete()) {
                                Map<String, Object> detail = new HashMap<>();
                                detail.put("propertyMap", new HashMap<String, List<String>>());
                                detail.put("callGraph", new ResultCallGraph(method.getSignature()));

                                SootMethod targetMethod = invokeExpr.getMethod();

                                ResultCallGraph rootCallGraph = (ResultCallGraph) detail.get("callGraph");
                                rootCallGraph.addChild(new ResultCallGraph(targetMethod.getSignature()));

                                boolean isTarget = DataFlowAnalysis.judgeReflectionInMethod(targetMethod, detail);
                                if (isTarget) {
                                    List<String> propertyNames = DataFlowAnalysis.retrieveReflectedArgs(body, stmt, new ArrayList<>(), true, detail);
                                    if (!propertyNames.isEmpty()) {
                                        // check
                                        Map<String, List<String>> propertyMap = (Map<String, List<String>>) detail.get("propertyMap");
                                        for (String propertyName: propertyNames) {
                                            if (!propertyMap.containsKey(propertyName)) {
                                                System.out.println("Wrong5");
                                            }
                                        }

                                        AppConfig.fileResult.addMethod(targetMethod);
                                        AppConfig.fileResult.addMethod(method);
                                        AppConfig.fileResult.addDetail(detail);
                                    }
                                }
                            }
                        }
                        else if (stmt instanceof AssignStmt && stmt.containsFieldRef()) {
                            SootField field = stmt.getFieldRef().getField();
                            if (field == null) {
                                continue;
                            }
                            if (field.getType().toString().equals("java.lang.Class") || field.getType().toString().equals("java.lang.reflect.Method")) {
                                Map<String, Object> detail = new HashMap<>();
                                detail.put("propertyMap", new HashMap<String, List<String>>());
                                detail.put("callGraph", new ResultCallGraph(method.getSignature()));

                                boolean isTarget = DataFlowAnalysis.judgeReflectionInField(field, detail);
                                if (isTarget) {
                                    List<String> propertyNames = DataFlowAnalysis.retrieveReflectedArgs(body, stmt, new ArrayList<>(), true, detail);
                                    if (!propertyNames.isEmpty()) {
                                        // check
                                        Map<String, List<String>> propertyMap = (Map<String, List<String>>) detail.get("propertyMap");
                                        for (String propertyName: propertyNames) {
                                            if (!propertyMap.containsKey(propertyName)) {
                                                System.out.println("Wrong6");
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
