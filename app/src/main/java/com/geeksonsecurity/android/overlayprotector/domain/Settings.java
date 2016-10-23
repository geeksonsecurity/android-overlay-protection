package com.geeksonsecurity.android.overlayprotector.domain;

public class Settings {

    public static final String KEY_SETTINGS = "settings";

    // Setting default values
    private long suspectedAppUpdateTimestamp = 0;
    private int uninstallTimeoutSeconds = 15;
    private int ignoreOnceTimeoutSeconds = 10;
    private boolean advancedMode = true;
    private DetectionEngine detectionEngine = DetectionEngine.ADVANCED;

    public long getSuspectedAppUpdateTimestamp() {
        return suspectedAppUpdateTimestamp;
    }

    public void setSuspectedAppUpdateTimestamp(long suspectedAppUpdateTimestamp) {
        this.suspectedAppUpdateTimestamp = suspectedAppUpdateTimestamp;
    }

    public boolean isAdvancedMode() {
        return advancedMode;
    }

    public void setAdvancedMode(boolean advancedMode) {
        this.advancedMode = advancedMode;
    }

    public int getUninstallTimeoutSeconds() {
        return uninstallTimeoutSeconds;
    }

    public void setUninstallTimeoutSeconds(int uninstallTimeoutSeconds) {
        this.uninstallTimeoutSeconds = uninstallTimeoutSeconds;
    }

    public int getIgnoreOnceTimeoutSeconds() {
        return ignoreOnceTimeoutSeconds;
    }

    public void setIgnoreOnceTimeoutSeconds(int ignoreOnceTimeoutSeconds) {
        this.ignoreOnceTimeoutSeconds = ignoreOnceTimeoutSeconds;
    }

    public long getEventProcessingDelayMilliSeconds() {
        return 350;
    }

    public boolean isDetectOnTypeWindowsChanged() {
        return true;
    }

    public boolean isDetectOnTypeWindowContentChanged() {
        return true;
    }

    public boolean isDetectOnTypeWindowStateChanged() {
        return true;
    }

    public void setDetectionEngine(DetectionEngine detectionEngine) {
        this.detectionEngine = detectionEngine;
    }

    public DetectionEngine getDetectionEngine() {
        return detectionEngine;
    }
}
