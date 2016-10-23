package com.geeksonsecurity.android.overlayprotector.domain;

import com.geeksonsecurity.android.overlayprotector.service.AdvancedDetectionService;
import com.geeksonsecurity.android.overlayprotector.service.BaseDetectionEngine;


public enum DetectionEngine {
    ADVANCED("Advanced", AdvancedDetectionService.class),
    BASE("Base", BaseDetectionEngine.class);

    private String _name;
    private Class _clazz;

    DetectionEngine(String name, Class clazz) {
        _name = name;
        _clazz = clazz;
    }

    @Override
    public String toString() {
        return _name;
    }

    public static DetectionEngine get(String name) {
        if (name == BASE.toString()) {
            return BASE;
        } else {
            return ADVANCED;
        }
    }

    public Class getDetectionClass() {
        return _clazz;
    }
}