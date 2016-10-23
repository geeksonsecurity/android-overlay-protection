package com.geeksonsecurity.android.overlayprotector.domain;

import com.j256.ormlite.field.DatabaseField;

public class SuspectedApp {
    @DatabaseField(generatedId = true)
    public int id;

    @DatabaseField
    private String packageName;

    @DatabaseField
    private String appName;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

}
