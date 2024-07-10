package com.rampage.property.result;

import java.util.*;

public class ResultCallGraph {
    private String functionName;
    private List<ResultCallGraph> children;

    public ResultCallGraph(String functionName) {
        this.functionName = functionName;
        this.children = new ArrayList<>();
    }

    public String getFunctionName() {
        return functionName;
    }

    public List<ResultCallGraph> getChildren() {
        return children;
    }

    public void addChild(ResultCallGraph child) {
        if (!this.hasChild(child)) {
            this.children.add(child);
        }
    }

    public boolean addChild(String targetFunctionName, String newFunctionName) {
        ResultCallGraph targetFunction = findFunction(targetFunctionName, this);
        if (targetFunction != null) {
            targetFunction.addChild(new ResultCallGraph(newFunctionName));
            return true;
        } else {
            return false;
        }
    }

    public int getChildrenCount(String targetFunctionName) {
        ResultCallGraph targetFunction = findFunction(targetFunctionName, this);
        return targetFunction.getChildren().size();
    }

    private boolean hasChild(ResultCallGraph newChild) {
        for (ResultCallGraph child : this.getChildren()) {
            if (child.getFunctionName().equals(newChild.getFunctionName())) {
                return true;
            }
        }
        return false;
    }

    private ResultCallGraph findFunction(String functionName, ResultCallGraph currentNode) {
        if (currentNode.getFunctionName().equals(functionName)) {
            return currentNode;
        } else {
            for (ResultCallGraph child : currentNode.getChildren()) {
                ResultCallGraph result = findFunction(functionName, child);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "FunctionCall{" +
                "functionName='" + functionName + '\'' +
                ", children=" + children +
                '}';
    }
}
