package com.rampage.settings;

import com.rampage.settings.result.ResultCallGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.*;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.UnitValueBoxPair;


import java.util.*;

public class DataFlowAnalysis {
    protected static Logger logger = LoggerFactory.getLogger(DataFlowAnalysis.class);

    public static List<String> retrieveStringValue(Body body, Stmt stmt, Value value, List<String> visited, boolean isProperty, Map<String, Object> detail) {
        List<String> result = new ArrayList<>();

        if (value == null) {
            return result;
        }

        String md5 = Utils.calculateMD5("retrieveStringValue" + body.getMethod().getSignature() + stmt.toString() + value.toString());
        if (visited.contains(md5)) {
            return result;
        }
        else {
            visited.add(md5);
        }

        if (value instanceof StringConstant) {
            String propertyName = ((StringConstant) value).value;
            if (isProperty) {
                // TODO: property name here OK backtrace OK
                Map<String, List<String>> propertyMap = (Map<String, List<String>>) detail.get("propertyMap");
                if (!propertyMap.containsKey(propertyName)) {
                    propertyMap.put(propertyName, new ArrayList<>());
                }
                propertyMap.get(propertyName).add(body.getMethod().getSignature());

                // backtrace
                // property name in init or clinit
                if (body.getMethod().getName().equals("<init>") || body.getMethod().getName().equals("<clinit>")) {
                    if (AppConfig.fieldUseMethod != null) {
                        backtrace(AppConfig.fieldUseMethod, detail, 0);
                    }
                    else {
                        backtrace(body.getMethod(), detail, 0);
                    }
                }
                else {
                    backtrace(body.getMethod(), detail, 0);
                }
            }
            result.add(propertyName);
        }
        else if (value.getType().toString().equals("java.lang.String") || (stmt instanceof AssignStmt && ((AssignStmt) stmt).getRightOp() instanceof CastExpr)) {
            List<Unit> defUnits = findDefs(body, stmt, (Local) value);
            for (Unit defUnit: defUnits) {
                if (defUnit instanceof AssignStmt) {
                    if (((AssignStmt) defUnit).getRightOp() instanceof InvokeExpr) {
                        SootMethod invokeMethod = ((InvokeExpr) ((AssignStmt) defUnit).getRightOp()).getMethod();
                        if (invokeMethod.getSignature().equals("<java.lang.StringBuilder: java.lang.String toString()>")) {
                            Value stringBuilderArg = ((AssignStmt) defUnit).getRightOp().getUseBoxes().get(0).getValue();
                            List<String> temp = retrieveStringBuilderValue(body, (Stmt) defUnit, (Local) stringBuilderArg, visited, isProperty, detail);
                            result.addAll(temp);
                            // TODO: saveResult and judge validity
                        }
                        else if (invokeMethod.getSignature().equals("<java.util.Iterator: java.lang.Object next()>")) {
                            Value iteratorArg = ((AssignStmt) defUnit).getRightOp().getUseBoxes().get(0).getValue();
                            List<String> temp = retrieveIteratorValue(body, (Stmt) defUnit, (Local) iteratorArg, visited, isProperty, detail);
                            result.addAll(temp);
                        }
//                        else {
//                            System.out.println("Missed cases in method retrieveStringValue\n" + invokeMethod.getSignature());
//                        }
                    }
                    else if (((AssignStmt) defUnit).getRightOp() instanceof StringConstant) {
                        String propertyName = ((StringConstant) ((AssignStmt) defUnit).getRightOp()).value;
                        if (isProperty) {
                            // TODO: property name here OK backtrace OK
                            Map<String, List<String>> propertyMap = (Map<String, List<String>>) detail.get("propertyMap");
                            if (!propertyMap.containsKey(propertyName)) {
                                propertyMap.put(propertyName, new ArrayList<>());
                            }
                            propertyMap.get(propertyName).add(body.getMethod().getSignature());

                            // backtrace
                            // property name in init or clinit
                            if (body.getMethod().getName().equals("<init>") || body.getMethod().getName().equals("<clinit>")) {
                                if (AppConfig.fieldUseMethod != null) {
                                    backtrace(AppConfig.fieldUseMethod, detail, 0);
                                }
                                else {
                                    System.out.println("Wrong logic in retrieveStringValue");
                                }
                            }
                            else {
                                backtrace(body.getMethod(), detail, 0);
                            }
                        }
                        result.add(propertyName);
                    }
                    else if (((AssignStmt) defUnit).getRightOp() instanceof ArrayRef) {
                        List<String> temp = retrieveArrayValue(body, (Stmt) defUnit, visited, isProperty, detail);
                        result.addAll(temp);
                    }
                    else if (((AssignStmt) defUnit).getRightOp() instanceof FieldRef) {
                        // TODO: need check !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                        SootField targetField = ((FieldRef) ((AssignStmt) defUnit).getRightOp()).getField();
                        List<UnitValueBoxPair> uses = findFieldUses(targetField);
                        if (!uses.isEmpty() && AppConfig.fieldDefMethod != null) {
                            // TODO: inter procedure here OK
                            if (detail != null) {
                                ResultCallGraph rootCallGraph = (ResultCallGraph) detail.get("callGraph");
                                rootCallGraph.addChild(body.getMethod().getSignature(), AppConfig.fieldDefMethod.getSignature());
                                AppConfig.fileResult.addMethod(AppConfig.fieldDefMethod);
                            }

                            AppConfig.fieldUseMethod = body.getMethod();
                            for (UnitValueBoxPair use: uses) {
                                Unit useUnit = use.getUnit();
                                if (useUnit instanceof AssignStmt) {
                                    List<String> temp = retrieveStringValue(AppConfig.fieldDefMethod.retrieveActiveBody(), (Stmt) useUnit, ((AssignStmt) useUnit).getRightOp(), visited, isProperty, detail);
                                    result.addAll(temp);
                                }
                            }
                            AppConfig.fieldDefMethod = null;
                            AppConfig.fieldUseMethod = null;
                        }
                    }
                    else if (((AssignStmt) defUnit).getRightOp() instanceof CastExpr) {
                        CastExpr castExpr = (CastExpr) ((AssignStmt) defUnit).getRightOp();
                        List<String> temp = retrieveStringValue(body, (Stmt) defUnit, castExpr.getOp(), visited, isProperty, detail);
                        result.addAll(temp);
                    }
                    else if (((AssignStmt) defUnit).getRightOp() instanceof AnyNewExpr) {
                        List<UnitValueBoxPair> uses = findUsesForward(body, (Stmt) defUnit, (Local) value, true);
                        for (UnitValueBoxPair use: uses) {
                            Stmt useStmt = (Stmt) use.getUnit();
                            if (useStmt.containsInvokeExpr()) {
                                SootMethod invokeMethod = useStmt.getInvokeExpr().getMethod();
                                if (invokeMethod.getSignature().equals("<java.lang.String: void <init>(java.lang.String)>")) {
                                    Value targetValue = useStmt.getInvokeExpr().getArg(0);
                                    List<String> temp = retrieveStringValue(body, useStmt, targetValue, visited, isProperty, detail);
                                    result.addAll(temp);
                                }
                            }
                        }
                    }
                }
                else if (defUnit instanceof IdentityStmt) {
                    ParameterRef targetArg = (ParameterRef) ((IdentityStmt) defUnit).getRightOp();
                    List<String> argValues = retrieveArgValue(body.getMethod(), targetArg.getIndex(), visited, isProperty, detail);
                    result.addAll(argValues);
                }
            }
        }
        return result;
    }

    public static List<String> retrieveIteratorValue(Body body, Stmt stmt, Local iteratorArg, List<String> visited, boolean isProperty, Map<String, Object> detail) {
        List<String> result = new ArrayList<>();

        List<Unit> iteratorDefs =  findDefs(body, stmt, iteratorArg);
        for (Unit iteratorDef: iteratorDefs) {
            Stmt iteratorDefStmt = (Stmt) iteratorDef;
            if (iteratorDefStmt instanceof AssignStmt && iteratorDefStmt.containsInvokeExpr()) {
                SootMethod invokeMethod = iteratorDefStmt.getInvokeExpr().getMethod();
                if (invokeMethod.getSignature().equals("<java.util.List: java.util.Iterator iterator()>")) {
                    Value listArg = ((AssignStmt) iteratorDefStmt).getRightOp().getUseBoxes().get(0).getValue();
                    List<Unit> listDefs =  findDefs(body, iteratorDefStmt, (Local) listArg);
                    for (Unit listDef: listDefs) {
                        if (listDef instanceof AssignStmt) {
                            AssignStmt listDefStmt = (AssignStmt) listDef;
                            if (listDefStmt.getRightOp() instanceof AnyNewExpr) {
                                List<UnitValueBoxPair> listUses = findUsesForward(body, listDefStmt, (Local) listDefStmt.getLeftOp(), true);
                                for (UnitValueBoxPair listUse: listUses) {
                                    Unit listUseUnit = listUse.getUnit();
                                    if (((Stmt) listUseUnit).containsInvokeExpr()) {
                                        SootMethod listInvokeMethod = ((Stmt) listUseUnit).getInvokeExpr().getMethod();
                                        if (listInvokeMethod.getSignature().equals("<java.util.List: boolean add(java.lang.Object)>")) {
                                            Value targetValue = ((Stmt) listUseUnit).getInvokeExpr().getArg(0);
                                            List<String> temp = retrieveStringValue(body, (Stmt) listUseUnit, targetValue, visited, isProperty, detail);
                                            result.addAll(temp);
                                        }
                                    }
                                }
                            }
                            else if (listDefStmt.getRightOp() instanceof FieldRef) {
                                // TODO: need check !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                                SootField targetField = ((FieldRef) listDefStmt.getRightOp()).getField();
                                List<UnitValueBoxPair> uses = findFieldUses(targetField);
                                if (!uses.isEmpty() && AppConfig.fieldDefMethod != null) {
                                    // TODO: inter procedure here OK
                                    if (detail != null) {
                                        ResultCallGraph rootCallGraph = (ResultCallGraph) detail.get("callGraph");
                                        rootCallGraph.addChild(body.getMethod().getSignature(), AppConfig.fieldDefMethod.getSignature());
                                        AppConfig.fileResult.addMethod(AppConfig.fieldDefMethod);
                                    }

                                    AppConfig.fieldUseMethod = body.getMethod();
                                    for (UnitValueBoxPair use: uses) {
                                        Unit useUnit = use.getUnit();
                                        if (((Stmt) useUnit).containsInvokeExpr()) {
                                            SootMethod listInvokeMethod = ((Stmt) useUnit).getInvokeExpr().getMethod();
                                            if (listInvokeMethod.getSignature().equals("<java.util.List: boolean add(java.lang.Object)>")) {
                                                Value targetValue = ((Stmt) useUnit).getInvokeExpr().getArg(0);
                                                List<String> temp = retrieveStringValue(AppConfig.fieldDefMethod.retrieveActiveBody(), (Stmt) useUnit, targetValue, visited, isProperty, detail);
                                                result.addAll(temp);
                                            }
                                        }
                                    }
                                    AppConfig.fieldDefMethod = null;
                                    AppConfig.fieldUseMethod = null;
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public static List<String> retrieveArrayValue(Body body, Stmt stmt, List<String> visited, boolean isProperty, Map<String, Object> detail) {
        List<String> result = new ArrayList<>();

        String md5 = Utils.calculateMD5("retrieveArrayValue" + body.getMethod().getSignature() + stmt.toString());
        if (visited.contains(md5)) {
            return result;
        }
        else {
            visited.add(md5);
        }

        ArrayRef target = (ArrayRef) ((AssignStmt) stmt).getRightOp();
        Value arrayValue = target.getBase();

        List<Unit> arrayDefs = findDefs(body, stmt, (Local) arrayValue);
        for (Unit arrayDef: arrayDefs) {
            if (arrayDef instanceof AssignStmt) {
                if (((AssignStmt) arrayDef).getRightOp() instanceof NewArrayExpr) {
                    List<UnitValueBoxPair> arrayUses = findUsesForward(body, (Stmt) arrayDef, (Local) ((AssignStmt) arrayDef).getLeftOp(), true);
                    for (UnitValueBoxPair arrayUse: arrayUses) {
                        Unit arrayUseUnit = arrayUse.getUnit();
                        if (arrayUseUnit instanceof AssignStmt && ((AssignStmt) arrayUseUnit).getLeftOp() instanceof ArrayRef) {
                            ArrayRef arrayRef = (ArrayRef) ((AssignStmt) arrayUseUnit).getLeftOp();
                            if (arrayRef.getBase().equals(arrayValue)) {
                                List<String> temp = retrieveStringValue(body, (Stmt) arrayUseUnit, ((AssignStmt) arrayUseUnit).getRightOp(), visited, isProperty, detail);
                                result.addAll(temp);
                            }
                        }
                    }
                }
                else if (((AssignStmt) arrayDef).getRightOp() instanceof FieldRef) {
                    // TODO: need check !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    SootField targetField = ((FieldRef) ((AssignStmt) arrayDef).getRightOp()).getField();
                    List<UnitValueBoxPair> uses = findFieldUses(targetField);
                    if (!uses.isEmpty() && AppConfig.fieldDefMethod != null) {
                        // TODO: inter procedure here OK
                        if (detail != null) {
                            ResultCallGraph rootCallGraph = (ResultCallGraph) detail.get("callGraph");
                            rootCallGraph.addChild(body.getMethod().getSignature(), AppConfig.fieldDefMethod.getSignature());
                            AppConfig.fileResult.addMethod(AppConfig.fieldDefMethod);
                        }

                        AppConfig.fieldUseMethod = body.getMethod();
                        for (UnitValueBoxPair use: uses) {
                            Unit useUnit = use.getUnit();
                            if (useUnit instanceof AssignStmt && ((AssignStmt) useUnit).getLeftOp() instanceof ArrayRef) {
                                List<String> temp = retrieveStringValue(AppConfig.fieldDefMethod.retrieveActiveBody(), (Stmt) useUnit, ((AssignStmt) useUnit).getRightOp(), visited, isProperty, detail);
                                result.addAll(temp);
                            }
                        }
                        AppConfig.fieldDefMethod = null;
                        AppConfig.fieldUseMethod = null;
                    }
                }
            }
            else if (arrayDef instanceof IdentityStmt) {
                ParameterRef targetArg = (ParameterRef) ((IdentityStmt) arrayDef).getRightOp();
                List<String> argValues = retrieveArgValue(body.getMethod(), targetArg.getIndex(), visited, isProperty, detail);
                result.addAll(argValues);
            }
        }

        return result;
    }

    public static List<UnitValueBoxPair> findFieldUses(SootField targetField) {
        List<UnitValueBoxPair> result = new ArrayList<>();

        SootMethod targetMethod = null;
        SootClass sootClass = targetField.getDeclaringClass();
        for (Iterator<SootMethod> methodIterator = sootClass.methodIterator(); methodIterator.hasNext(); ) {
            SootMethod method = methodIterator.next();
            if (targetField.isStatic() && method.getName().equals("<clinit>") && method.isConcrete()) {
                targetMethod = method;
                break;
            }
            else if (!targetField.isStatic() && method.getName().equals("<init>") && method.isConcrete()) {
                targetMethod = method;
                break;
            }
        }

        if (targetMethod != null) {
            Body body = targetMethod.retrieveActiveBody();
            for (Unit unit : body.getUnits()) {
                Stmt stmt = (Stmt) unit;
                if (stmt instanceof AssignStmt) {
                    if (((AssignStmt) stmt).getLeftOp() instanceof FieldRef) {
                        SootField field = ((FieldRef) ((AssignStmt) stmt).getLeftOp()).getField();
                        if (field.getSignature().equals(targetField.getSignature())) {
                            Value targetValue = ((AssignStmt) stmt).getRightOp();
                            if (targetValue instanceof StringConstant) {
                                result.add(new UnitValueBoxPair(stmt, stmt.getUseBoxes().get(0)));
                            }
                            else if (targetValue instanceof Local) {
                                List<Unit> defUnits = findDefs(body, stmt, (Local) targetValue);
                                for (Unit defUnit: defUnits) {
                                    if (defUnit instanceof AssignStmt && ((AssignStmt) defUnit).getRightOp() instanceof AnyNewExpr) {
                                        List<UnitValueBoxPair> temp = findUsesForward(body, (Stmt) defUnit, (Local) targetValue, true);
                                        result.addAll(temp);
                                    }
                                }
                            }
                        }
                    }
                    else if (targetField.getType().toString().equals("java.util.List") && ((AssignStmt) stmt).getRightOp() instanceof FieldRef) {
                        SootField field = ((FieldRef) ((AssignStmt) stmt).getRightOp()).getField();
                        if (field.getSignature().equals(targetField.getSignature())) {
                            Value targetValue = ((AssignStmt) stmt).getLeftOp();
                            List<UnitValueBoxPair> temp = findUsesForward(body, stmt, (Local) targetValue, true);
                            result.addAll(temp);
                        }
                    }
                }
            }
        }

        if (!result.isEmpty()) {
            // TODO: inter procedure here but process outside OK
            AppConfig.fieldDefMethod = targetMethod;
        }

        return result;
    }

    public static List<String> retrieveArgValue(SootMethod targetMethod, int argIndex, List<String> visited, boolean isProperty, Map<String, Object> detail) {
        List<String> result = new ArrayList<>();

        // analysis
        if (AppConfig.callGraph == null) {
            AppConfig.callGraph = Scene.v().getCallGraph();
        }
        Iterator<Edge> edgeIterator = AppConfig.callGraph.edgesInto(targetMethod);

        if (edgeIterator != null && edgeIterator.hasNext()) {
            while (edgeIterator.hasNext()) {
                // TODO: inter procedure here OK
                Edge edge = edgeIterator.next();
                Stmt srcStmt = edge.srcStmt();
                Value targetValue = srcStmt.getInvokeExpr().getArg(argIndex);
                // inter procedure case for reflection arg
                if (targetValue.getType().toString().equals("java.lang.Object[]")) {
                    if (detail != null) {
                        ResultCallGraph rootCallGraph = (ResultCallGraph) detail.get("callGraph");
                        rootCallGraph.addChild(targetMethod.getSignature(), edge.src().getSignature());
                        AppConfig.fileResult.addMethod(edge.src());
                    }

                    List<String> temp = retrieveReflectionArrayValue(edge.src().retrieveActiveBody(), srcStmt, targetValue, visited, isProperty, detail);
                    result.addAll(temp);
                }
                else {
                    if (detail != null) {
                        ResultCallGraph rootCallGraph = (ResultCallGraph) detail.get("callGraph");
                        rootCallGraph.addChild(targetMethod.getSignature(), edge.src().getSignature());
                        AppConfig.fileResult.addMethod(edge.src());
                    }

                    List<String> temp = retrieveStringValue(edge.src().retrieveActiveBody(), srcStmt, targetValue, visited, isProperty, detail);
                    result.addAll(temp);
                }
            }
        }
        else {
            if (AppConfig.myCallGraph == null) {
                MyCallGraph.construct();
            }
            if (AppConfig.myCallGraph.containsKey(targetMethod)) {
                // TODO: inter procedure here OK
                List<Map<String, Object>> srcList = AppConfig.myCallGraph.get(targetMethod);
                for (Map<String, Object> srcMap: srcList) {
                    SootMethod srcMethod = (SootMethod) srcMap.get("src");
                    Stmt srcStmt = (Stmt) srcMap.get("srcStmt");
                    Value targetValue = srcStmt.getInvokeExpr().getArg(argIndex);
                    // inter procedure case for reflection arg
                    if (targetValue.getType().toString().equals("java.lang.Object[]")) {
                        if (detail != null) {
                            ResultCallGraph rootCallGraph = (ResultCallGraph) detail.get("callGraph");
                            rootCallGraph.addChild(targetMethod.getSignature(), srcMethod.getSignature());
                            AppConfig.fileResult.addMethod(srcMethod);
                        }

                        List<String> temp = retrieveReflectionArrayValue(srcMethod.retrieveActiveBody(), srcStmt, targetValue, visited, isProperty, detail);
                        result.addAll(temp);
                    }
                    else {
                        if (detail != null) {
                            ResultCallGraph rootCallGraph = (ResultCallGraph) detail.get("callGraph");
                            rootCallGraph.addChild(targetMethod.getSignature(), srcMethod.getSignature());
                            AppConfig.fileResult.addMethod(srcMethod);
                        }

                        List<String> temp = retrieveStringValue(srcMethod.retrieveActiveBody(), srcStmt, targetValue, visited, isProperty, detail);
                        result.addAll(temp);
                    }
                }
            }
        }

        return result;
    }

    public static List<String> retrieveReflectionArrayValue(Body body, Stmt stmt, Value value, List<String> visited, boolean isProperty, Map<String, Object> detail) {
        List<String> result = new ArrayList<>();

        String md5 = Utils.calculateMD5("retrieveReflectionArrayValue" + body.getMethod().getSignature() + stmt.toString() + value.toString());
        if (visited.contains(md5)) {
            return result;
        }
        else {
            visited.add(md5);
        }

        List<Unit> reflectionArrayDefs = findDefs(body, stmt, (Local) value);
        for (Unit reflectionArrayDef: reflectionArrayDefs) {
            List<UnitValueBoxPair> reflectionArrayUses = findUsesForward(body, (Stmt) reflectionArrayDef, (Local) value, true);
            for (UnitValueBoxPair reflectionArrayUse: reflectionArrayUses) {
                Unit reflectionArrayUseUnit = reflectionArrayUse.getUnit();
                if (reflectionArrayUseUnit instanceof AssignStmt && ((AssignStmt) reflectionArrayUseUnit).getLeftOp() instanceof ArrayRef) {
                    if (((ArrayRef)((AssignStmt) reflectionArrayUseUnit).getLeftOp()).getIndex().toString().equals("0")) {
                        Value rop = ((AssignStmt) reflectionArrayUseUnit).getRightOp();
                        List<String> propertyNames = retrieveStringValue(body, (Stmt) reflectionArrayUseUnit, rop, visited, isProperty, detail);
                        result.addAll(propertyNames);
                    }
                }
            }
        }

        return result;
    }

    public static List<String> retrieveReflectedArgs(Body body, Stmt reflectStmt, List<String> visited, boolean isProperty, Map<String, Object> detail) {
        List<String> result = new ArrayList<>();

        if (((AssignStmt) reflectStmt).getLeftOp().getType().toString().equals("java.lang.Class")) {
            Stmt classStmt = reflectStmt;
            if (((AssignStmt) classStmt).getLeftOp() instanceof Local) {
                Local classValue = (Local) ((AssignStmt) classStmt).getLeftOp();
                List<UnitValueBoxPair> classUses = findUsesForward(body, classStmt, classValue, true);
                for (UnitValueBoxPair classUse: classUses) {
                    Unit classUseUnit = classUse.getUnit();
                    if (classUseUnit instanceof AssignStmt && ((AssignStmt) classUseUnit).getRightOp() instanceof InvokeExpr) {
                        SootMethod classInvokeMethod = ((InvokeExpr) ((AssignStmt) classUseUnit).getRightOp()).getMethod();
                        if (classInvokeMethod.getSignature().equals("<java.lang.Class: java.lang.reflect.Method getMethod(java.lang.String,java.lang.Class[])>")
                                || classInvokeMethod.getSignature().equals("<java.lang.Class: java.lang.reflect.Method getDeclaredMethod(java.lang.String,java.lang.Class[])>")) {
                            Local methodValue = (Local) ((AssignStmt) classUseUnit).getLeftOp();
                            List<UnitValueBoxPair> methodUses = findUsesForward(body, (Stmt) classUseUnit, methodValue, true);
                            for (UnitValueBoxPair methodUse: methodUses) {
                                Unit methodUseUnit = methodUse.getUnit();
                                if (methodUseUnit instanceof AssignStmt && ((AssignStmt) methodUseUnit).getRightOp() instanceof InvokeExpr) {
                                    SootMethod methodInvokeMethod = ((InvokeExpr) ((AssignStmt) methodUseUnit).getRightOp()).getMethod();
                                    if (methodInvokeMethod.getSignature().equals("<java.lang.reflect.Method: java.lang.Object invoke(java.lang.Object,java.lang.Object[])>")) {
                                        Value reflectionArgs = ((InvokeExpr) ((AssignStmt) methodUseUnit).getRightOp()).getArg(1);
                                        List<Unit> reflectionArgDefs = findDefs(body, (Stmt) methodUseUnit, (Local) reflectionArgs);
                                        for (Unit reflectionArgDef: reflectionArgDefs) {
                                            if (reflectionArgDef instanceof IdentityStmt) {
                                                ParameterRef targetArg = (ParameterRef) ((IdentityStmt) reflectionArgDef).getRightOp();
                                                List<String> argValues = retrieveArgValue(body.getMethod(), targetArg.getIndex(), visited, isProperty, detail);
                                                result.addAll(argValues);
                                            }
                                            else {
                                                List<UnitValueBoxPair> reflectionArgsUses = findUsesForward(body, (Stmt) reflectionArgDef, (Local) reflectionArgs, true);
                                                for (UnitValueBoxPair reflectionArgsUse: reflectionArgsUses) {
                                                    Unit reflectionArgsUseUnit = reflectionArgsUse.getUnit();
                                                    if (reflectionArgsUseUnit instanceof AssignStmt && ((AssignStmt) reflectionArgsUseUnit).getLeftOp() instanceof ArrayRef) {
                                                        if (((ArrayRef)((AssignStmt) reflectionArgsUseUnit).getLeftOp()).getIndex().toString().equals("0")) {
                                                            Value rop = ((AssignStmt) reflectionArgsUseUnit).getRightOp();
                                                            List<String> propertyNames = DataFlowAnalysis.retrieveStringValue(body, (Stmt) reflectionArgsUseUnit, rop, visited, isProperty, detail);
                                                            result.addAll(propertyNames);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                else if (methodUseUnit instanceof AssignStmt && ((AssignStmt) methodUseUnit).getRightOp().equals(methodValue)) {
                                    if (((AssignStmt) methodUseUnit).getLeftOp() instanceof FieldRef) {
                                        List<Map<String, Object>> targetValueBoxs = findFieldAssignForward(body, (Stmt) methodUseUnit, ((FieldRef) ((AssignStmt) methodUseUnit).getLeftOp()).getField());
                                        for (Map<String, Object> targetValueBox: targetValueBoxs) {
                                            if (targetValueBox.get("value") instanceof Value && targetValueBox.get("stmt") instanceof Stmt) {
                                                Value targetValue = (Value) targetValueBox.get("value");
                                                Stmt targetStmt = (Stmt) targetValueBox.get("stmt");
                                                List<String> temp = retrieveReflectedArgs(body, targetStmt, visited, isProperty, detail);
                                                result.addAll(temp);
                                            }
                                        }
                                    }
                                    else {
                                        List<String> temp = retrieveReflectedArgs(body, (Stmt) methodUseUnit, visited, isProperty, detail);
                                        result.addAll(temp);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        else if (((AssignStmt) reflectStmt).getLeftOp().getType().toString().equals("java.lang.reflect.Method")) {
            Stmt methodStmt = reflectStmt;
            if (((AssignStmt) methodStmt).getLeftOp() instanceof Local) {
                Local methodValue = (Local) ((AssignStmt) methodStmt).getLeftOp();
                List<UnitValueBoxPair> methodUses = findUsesForward(body, methodStmt, methodValue, true);
                for (UnitValueBoxPair methodUse: methodUses) {
                    Unit methodUseUnit = methodUse.getUnit();
                    if (methodUseUnit instanceof AssignStmt && ((AssignStmt) methodUseUnit).getRightOp() instanceof InvokeExpr) {
                        SootMethod methodInvokeMethod = ((InvokeExpr) ((AssignStmt) methodUseUnit).getRightOp()).getMethod();
                        if (methodInvokeMethod.getSignature().equals("<java.lang.reflect.Method: java.lang.Object invoke(java.lang.Object,java.lang.Object[])>")) {
                            Value reflectionArgs = ((InvokeExpr) ((AssignStmt) methodUseUnit).getRightOp()).getArg(1);
                            List<UnitValueBoxPair> reflectionArgsUses = findUses(body, (Stmt) methodUseUnit, (Local) reflectionArgs, false);
                            for (UnitValueBoxPair reflectionArgsUse: reflectionArgsUses) {
                                Unit reflectionArgsUseUnit = reflectionArgsUse.getUnit();
                                if (reflectionArgsUseUnit instanceof AssignStmt && ((AssignStmt) reflectionArgsUseUnit).getLeftOp() instanceof ArrayRef) {
                                    if (((ArrayRef)((AssignStmt) reflectionArgsUseUnit).getLeftOp()).getIndex().toString().equals("0")) {
                                        Value rop = ((AssignStmt) reflectionArgsUseUnit).getRightOp();
                                        List<String> propertyNames = DataFlowAnalysis.retrieveStringValue(body, (Stmt) reflectionArgsUseUnit, rop, visited, isProperty, detail);
                                        result.addAll(propertyNames);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public static List<Map<String, Object>> findFieldAssignForward(Body body, Stmt stmt, SootField targetField) {
        UnitGraph cfg = new BriefUnitGraph(body);
        LinkedHashSet<Unit> pstUnits = new LinkedHashSet<>();
        Queue<Unit> queue = new LinkedList<>();
        queue.addAll(cfg.getSuccsOf((Unit) stmt));
        while (!queue.isEmpty()) {
            Unit curUnit = queue.poll();
            if (!pstUnits.contains(curUnit)) {
                pstUnits.add(curUnit);
                for (Unit pstUnit: cfg.getSuccsOf(curUnit)) {
                    if (!pstUnits.contains(pstUnit) && !queue.contains(pstUnit)) {
                        queue.add(pstUnit);
                    }
                }
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> subResult = new HashMap<>();
        for (Unit unit: pstUnits) {
            Stmt pstStmt = (Stmt) unit;
            if (pstStmt instanceof AssignStmt && ((AssignStmt) pstStmt).getRightOp() instanceof FieldRef) {
                SootField field = ((FieldRef) ((AssignStmt) pstStmt).getRightOp()).getField();
                if (field.getSignature().equals(targetField.getSignature()) && !result.contains(((AssignStmt) pstStmt).getLeftOp())) {
                    subResult.put("value", ((AssignStmt) pstStmt).getLeftOp());
                    subResult.put("stmt", pstStmt);
                    if (!result.contains(subResult)) {
                        result.add(subResult);
                    }
                }
            }
        }

        return result;
    }

    public static List<String> retrieveStringBuilderValue(Body body, Stmt stmt, Local stringBuild, List<String> visited, boolean isProperty, Map<String, Object> detail) {
        List<String> appendedStrings = new ArrayList<>();
        List<String> tempResult;

//        String md5 = Utils.calculateMD5("retrieveStringBuilderValue" + body.getMethod().getSignature() + stmt.toString() + stringBuild.toString());
//        if (visited.contains(md5)) {
//            return appendedStrings;
//        }
//        else {
//            visited.add(md5);
//        }

        // first find the definition of StringBuilder
        Map<String, Object> defResult = findStringBuilderDefinition(body, stmt, stringBuild);
        if (defResult.get("value") instanceof Value && defResult.get("stmt") instanceof Stmt) {
            Value stringBuiderDef = (Value) defResult.get("value");
            Stmt stringBuiderDefStmt = (Stmt) defResult.get("stmt");

            // find all usage of StringBuilder
            List<UnitValueBoxPair> uses = findStringBuilderUsage(body, stringBuiderDefStmt, (Local) stringBuiderDef);
            for (UnitValueBoxPair use: uses) {
                Stmt useUnit = (Stmt) use.getUnit();
                if (useUnit.containsInvokeExpr()) {
                    SootMethod invokeMethod = useUnit.getInvokeExpr().getMethod();
                    if (invokeMethod.getSignature().equals("<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>")) {
                        Value appendedStringArg = useUnit.getInvokeExpr().getArg(0);
                        List<String> temp = retrieveStringValue(body, useUnit, appendedStringArg, visited, false, detail);
                        if (appendedStrings.isEmpty()) {
                            appendedStrings.addAll(temp);
                        }
                        else {
                            if (!temp.isEmpty()) {
                                tempResult = new ArrayList<>();
                                for (String appenedString: temp) {
                                    for (String existString: appendedStrings) {
                                        tempResult.add(existString + appenedString);
                                    }
                                }
                                appendedStrings = new ArrayList<>(tempResult);
                            }
                        }
                    }
                    else if (invokeMethod.getSignature().equals("<java.lang.StringBuilder: java.lang.StringBuilder append(int)>")) {
                        if (appendedStrings.isEmpty()) {
                            appendedStrings.add("(int)");
                        }
                        else {
                            tempResult = new ArrayList<>();
                            for (String existString: appendedStrings) {
                                tempResult.add(existString + "(int)");
                            }
                            appendedStrings = new ArrayList<>(tempResult);
                        }
                    }
                }
            }
            // TODO: property name here OK backtrace OK
            if (isProperty) {
                Map<String, List<String>> propertyMap = (Map<String, List<String>>) detail.get("propertyMap");
                for (String propertyName: appendedStrings) {
                    if (!propertyMap.containsKey(propertyName)) {
                        propertyMap.put(propertyName, new ArrayList<>());
                    }
                    propertyMap.get(propertyName).add(body.getMethod().getSignature());

                    // backtrace
                    // property name in init or clinit
                    if (body.getMethod().getName().equals("<init>") || body.getMethod().getName().equals("<clinit>")) {
                        if (AppConfig.fieldUseMethod != null) {
                            backtrace(AppConfig.fieldUseMethod, detail, 0);
                        }
                        else {
                            System.out.println("Wrong logic in retrieveStringValue");
                        }
                    }
                    else {
                        backtrace(body.getMethod(), detail, 0);
                    }
                }
            }
        }

        return appendedStrings;
    }

    public static List<UnitValueBoxPair> findStringBuilderUsage(Body body, Stmt stmt, Local stringBuildDef) {
        List<UnitValueBoxPair> result = new ArrayList<>();

        List<UnitValueBoxPair> uses = findUsesForward(body, stmt, stringBuildDef, true);
        result.addAll(uses);

        // complex case
        for (UnitValueBoxPair use: uses) {
            Stmt useUnit = (Stmt) use.getUnit();
            if (useUnit instanceof AssignStmt && ((AssignStmt) useUnit).getLeftOp().getType().toString().equals("java.lang.StringBuilder")) {
                List<UnitValueBoxPair> temp = findStringBuilderUsage(body, useUnit, (Local) ((AssignStmt) useUnit).getLeftOp());
                result.addAll(temp);
            }
        }

        return result;
    }

    public static Map<String, Object> findStringBuilderDefinition(Body body, Stmt stmt, Local stringBuildArg) {
        Map<String, Object> result = new HashMap<>();
        Value stringBuilderDef = null;
        Stmt stringBuilderDefStmt = null;

        List<Unit> defs = findDefs(body, stmt, stringBuildArg);
        if (defs.size() == 1) {
            Unit def = defs.get(0);
            // simple case
            if (def instanceof AssignStmt && ((AssignStmt) def).getRightOp() instanceof NewExpr) {
                stringBuilderDef = ((AssignStmt) def).getLeftOp();
                stringBuilderDefStmt = (Stmt) def;
            }
            else if (def instanceof AssignStmt && ((AssignStmt) def).getRightOp() instanceof InvokeExpr) {
                SootMethod invokeMethod = ((InvokeExpr) ((AssignStmt) def).getRightOp()).getMethod();
                if (invokeMethod.getReturnType().toString().equals("java.lang.StringBuilder")) {
                    Value stringBuilderArg = ((AssignStmt) def).getRightOp().getUseBoxes().get(0).getValue();
                    if (stringBuilderArg.getType().toString().equals("java.lang.StringBuilder")) {
                        Map<String, Object> temp = findStringBuilderDefinition(body, (Stmt) def, (Local) stringBuilderArg);
                        stringBuilderDef = (Value) temp.get("value");
                        stringBuilderDefStmt = (Stmt) temp.get("stmt");
                    }
                }
                else {
                    System.out.println("Missed cases in method retrieveStringBuilderValue1");
                }
            }
        }
        else {
            System.out.println("Missed cases in method findStringBuilderDefinition");
        }

        result.put("value", stringBuilderDef);
        result.put("stmt", stringBuilderDefStmt);
        return result;
    }

    public static void backtrace(SootMethod targetMethod, Map<String, Object> detail, int depth) {
        if (depth >= 5) {
            return;
        }

        if (AppConfig.callGraph == null) {
            AppConfig.callGraph = Scene.v().getCallGraph();
        }
        Iterator<Edge> edgeIterator = AppConfig.callGraph.edgesInto(targetMethod);

        if (edgeIterator != null && edgeIterator.hasNext()) {
            while (edgeIterator.hasNext()) {
                Edge edge = edgeIterator.next();
                SootMethod srcMethod = edge.src();

                if (detail != null) {
                    ResultCallGraph rootCallGraph = (ResultCallGraph) detail.get("callGraph");

                    if (rootCallGraph.getChildrenCount(targetMethod.getSignature()) <= 10) {
                        rootCallGraph.addChild(targetMethod.getSignature(), srcMethod.getSignature());
                        AppConfig.fileResult.addMethod(srcMethod);

                        backtrace(srcMethod, detail, depth + 1);
                    }
                    else {
                        return;
                    }
                }
            }
        }
        else {
            if (AppConfig.myCallGraph == null) {
                MyCallGraph.construct();
            }
            if (AppConfig.myCallGraph.containsKey(targetMethod)) {
                List<Map<String, Object>> srcList = AppConfig.myCallGraph.get(targetMethod);
                for (Map<String, Object> srcMap: srcList) {
                    SootMethod srcMethod = (SootMethod) srcMap.get("src");

                    if (detail != null) {
                        ResultCallGraph rootCallGraph = (ResultCallGraph) detail.get("callGraph");

                        if (rootCallGraph.getChildrenCount(targetMethod.getSignature()) <= 10) {
                            rootCallGraph.addChild(targetMethod.getSignature(), srcMethod.getSignature());
                            AppConfig.fileResult.addMethod(srcMethod);

                            backtrace(srcMethod, detail, depth + 1);
                        }
                        else {
                            return;
                        }
                    }
                }
            }
        }
    }

    public static List<UnitValueBoxPair> findUsesForward(Body body, Stmt stmt, Local local, boolean isAssign) {
        UnitGraph cfg = new BriefUnitGraph(body);
        LinkedHashSet<Unit> pstUnits = new LinkedHashSet<>();
        Queue<Unit> queue = new LinkedList<>();
        queue.addAll(cfg.getSuccsOf((Unit) stmt));
        while (!queue.isEmpty()) {
            Unit curUnit = queue.poll();
            if (!pstUnits.contains(curUnit)) {
                pstUnits.add(curUnit);
                for (Unit pstUnit: cfg.getSuccsOf(curUnit)) {
                    if (!pstUnits.contains(pstUnit) && !queue.contains(pstUnit)) {
                        queue.add(pstUnit);
                    }
                }
            }
        }
        List<UnitValueBoxPair> result = new ArrayList<>();
        List<UnitValueBoxPair> uses = findUses(body, stmt, local, isAssign);
        for (UnitValueBoxPair use: uses) {
            if (pstUnits.contains(use.unit)) {
                result.add(use);
            }
        }
        return result;
    }

    public static List<UnitValueBoxPair> findUsesBackward(Body body, Stmt stmt, Local local, boolean isAssign) {
        UnitGraph cfg = new ExceptionalUnitGraph(body);
        HashSet<Unit> preUnits = new HashSet<>();
        Queue<Unit> queue = new LinkedList<>();
        queue.addAll(cfg.getPredsOf((Unit) stmt));
        while (!queue.isEmpty()) {
            Unit curUnit = queue.poll();
            if (!preUnits.contains(curUnit)) {
                preUnits.add(curUnit);
                for (Unit preUnit: cfg.getPredsOf(curUnit)) {
                    if (!preUnits.contains(preUnit) && !queue.contains(preUnit)) {
                        queue.add(preUnit);
                    }
                }
            }
        }
        List<UnitValueBoxPair> result = new ArrayList<>();
        List<UnitValueBoxPair> uses = findUses(body, stmt, local, isAssign);
        for (UnitValueBoxPair use: uses) {
            if (preUnits.contains(use.unit)) {
                result.add(use);
            }
        }
        return result;
    }

    // 好像isAssign又没用了，需要后续进行考证
    public static List<UnitValueBoxPair> findUses(Body body, Stmt stmt, Local local, boolean isAssign) {
        List<UnitValueBoxPair> uses = new ArrayList<>();
        UnitGraph cfg = new BriefUnitGraph(body);
        SimpleLocalDefs simpleLocalDefs = new SimpleLocalDefs(cfg);
        if (isAssign) {
            SimpleLocalUses simpleLocalUses = new SimpleLocalUses(cfg, simpleLocalDefs);
            List<UnitValueBoxPair> pairs = simpleLocalUses.getUsesOf(stmt);
            uses.addAll(pairs);
        }
        else {
            List<Unit> defs = simpleLocalDefs.getDefsOfAt(local, stmt);
            SimpleLocalUses simpleLocalUses = new SimpleLocalUses(cfg, simpleLocalDefs);
            for (Unit def: defs) {
                List<UnitValueBoxPair> pairs = simpleLocalUses.getUsesOf(def);
                uses.addAll(pairs);
            }
        }
        return uses;
    }

    public static List<Unit> findDefs(Body body, Stmt stmt, Local local) {
        UnitGraph cfg = new BriefUnitGraph(body);
        SimpleLocalDefs simpleLocalDefs = new SimpleLocalDefs(cfg);
        List<Unit> defs = simpleLocalDefs.getDefsOfAt(local, stmt);

        return defs;
    }
}
