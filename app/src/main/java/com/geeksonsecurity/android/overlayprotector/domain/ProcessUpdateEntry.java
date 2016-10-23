package com.geeksonsecurity.android.overlayprotector.domain;

public class ProcessUpdateEntry {
    private String process;
    private boolean hasChanged;

    public boolean isHasChanged() {
        return hasChanged;
    }

    public void setHasChanged(boolean hasChanged) {
        this.hasChanged = hasChanged;
    }

    public String getProcess() {
        return process;
    }

    public void setProcess(String process) {
        this.process = process;
    }
}
