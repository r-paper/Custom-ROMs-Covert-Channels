package com.rampage.access;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.tagkit.*;
import soot.util.Chain;

import java.util.*;

public class CoreAnalysis {
    protected static Logger logger = LoggerFactory.getLogger(CoreAnalysis.class);

    public static void analyze() {
        Chain<SootClass> sootClasses = Scene.v().getClasses();
        for (Iterator<SootClass> classIterator = sootClasses.snapshotIterator(); classIterator.hasNext();) {
            SootClass sootClass = classIterator.next();
            if (AppConfig.targetClass.contains(sootClass.getName())) {
                for (SootField field: sootClass.getFields()) {
                    Map<String, String> fieldInfo = new HashMap<>();
                    for (Tag tag: field.getTags()) {
                        if (tag instanceof StringConstantValueTag) {
                            fieldInfo.put("fieldName", sootClass.getName() + ": " + ((StringConstantValueTag) tag).getConstant().toString());
                        }
                        else if (tag instanceof VisibilityAnnotationTag) {
                            StringBuilder fieldAnnotations = new StringBuilder();
                            VisibilityAnnotationTag visibilityAnnotationTag = ((VisibilityAnnotationTag) tag);
                            for (AnnotationTag annotationTag: visibilityAnnotationTag.getAnnotations()) {
                                if (fieldAnnotations.length() != 0) {
                                    fieldAnnotations.append(" | ");
                                }
                                fieldAnnotations.append(annotationTag.getType());
                            }
                            fieldInfo.put("fieldAnnotations", fieldAnnotations.toString());
                        }
                    }
                    if (fieldInfo.size() > 0) {
                        AppConfig.fileResult.add(fieldInfo);
                    }
                }
            }
        }
    }
}
