package com.geeksonsecurity.android.overlayprotector;

import android.app.Application;

import com.geeksonsecurity.android.overlayprotector.database.DatabaseUtils;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DatabaseUtils.fillSuspectedApps(getApplicationContext());
    }
}
