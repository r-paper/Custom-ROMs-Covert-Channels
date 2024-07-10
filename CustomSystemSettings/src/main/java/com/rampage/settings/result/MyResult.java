package com.rampage.settings.result;

import soot.Body;
import soot.SootMethod;
import soot.Unit;

import java.util.*;

public class MyResult {
    private String filePath;
    private List<Map<String, Object>> details;
    private Map<String, String> methodBodyMap;
    public MyResult(String filePath){
        this.filePath = filePath;
        this.details = new ArrayList<>();
        this.methodBodyMap = new HashMap<>();
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void addDetail(Map<String, Object> detail) {
        this.details.add(detail);
    }

    public void addMethod(SootMethod sootMethod) {
        if (this.methodBodyMap.containsKey(sootMethod.getSignature())) {
            return;
        }
        Body body = sootMethod.retrieveActiveBody();
        StringBuilder stringBuilder = new StringBuilder();
        for (Unit unit : body.getUnits()) {
            stringBuilder.append(unit.toString()).append("\n");
        }
        this.methodBodyMap.put(sootMethod.getSignature(), stringBuilder.toString());
    }
}
