package com.geeksonsecurity.android.overlayprotector.domain;

public class OverlayState {
    private boolean hasOverlay = false;
    private boolean overlayShown = false;
    private String process = "";
    private String offender = "";
    private long processTimestamp = 0;
    private boolean pendingUninstall = false;
    private String ignoreOncePackage = "";
    private String additionalInfo;

    public synchronized boolean isHasOverlay() {
        return hasOverlay;
    }

    public synchronized void setHasOverlay(boolean hasOverlay) {
        this.hasOverlay = hasOverlay;
    }

    public synchronized void setProcess(String process) {
        this.process = process;
        processTimestamp = System.currentTimeMillis();
    }

    public synchronized String getOffender() {
        return offender;
    }

    public synchronized void setOffender(String offender) {
        this.offender = offender;
    }

    public synchronized boolean isOverlayShown() {
        return overlayShown;
    }

    public synchronized void setOverlayShown(boolean overlayShown) {
        this.overlayShown = overlayShown;
    }

    public synchronized void resetState() {
        process = "";
        hasOverlay = false;
        overlayShown = false;
        offender = "";
        ignoreOncePackage = "";
        additionalInfo = "";
    }

    public synchronized void setPendingUninstall(boolean pendingUninstall) {
        this.pendingUninstall = pendingUninstall;
    }

    public synchronized boolean isPendingUninstall() {
        return pendingUninstall;
    }

    public synchronized void setIgnoreOncePackage(String ignoreOncePackage) {
        this.ignoreOncePackage = ignoreOncePackage;
    }

    public synchronized String getIgnoreOncePackage() {
        return ignoreOncePackage;
    }

    public synchronized String getProcess() {
        return process;
    }

    public synchronized void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }
}
