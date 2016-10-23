package com.geeksonsecurity.android.overlayprotector.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.geeksonsecurity.android.overlayprotector.domain.Settings;
import com.geeksonsecurity.android.overlayprotector.domain.SuspectedApp;
import com.google.gson.Gson;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class DatabaseUtils {

    public static void fillSuspectedApps(Context context) {
        Gson gson = new Gson();
        String TAG = "DatabaseUtils.fillSuspectedApps";


        Context ctx = context.getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        String json = prefs.getString(Settings.KEY_SETTINGS, "");
        Settings settings = new Settings();
        if (!(json != null ? json.isEmpty() : true)) {
            settings = gson.fromJson(json, Settings.class);
        }

        if (settings.getSuspectedAppUpdateTimestamp() > 0) {
            Log.d(TAG, "Suspected apps already populated, skipping");
            return;
        }

        List<SuspectedApp> listData = new ArrayList<>();
        PackageManager p = context.getPackageManager();
        final List<PackageInfo> installedApps =
                p.getInstalledPackages(PackageManager.GET_PERMISSIONS |
                        PackageManager.GET_PROVIDERS);

        long refreshTimestamp = System.currentTimeMillis();

        Dao<SuspectedApp, Integer> suspectedAppDao = null;
        try {
            suspectedAppDao = DatabaseHelper.getHelper(context).getSuspectedAppDao();
            DeleteBuilder<SuspectedApp, Integer> deleteBuilder = suspectedAppDao.deleteBuilder();
            deleteBuilder.where().gt("id", 0);
            int deleted = deleteBuilder.delete();
            Log.i(TAG, "Deleted " + deleted + " entries from suspected apps");
        } catch (SQLException e) {
            Log.e(TAG, "Failed to retrieve suspected apps DAO", e);
        }

        for (PackageInfo i : installedApps) {
            if (i != null && i.requestedPermissions != null) {

                if (i.packageName.equals(context.getPackageName()))
                    continue;

                List<String> list = Arrays.asList(i.requestedPermissions);
                if (list.contains("android.permission.SYSTEM_ALERT_WINDOW") && list.contains("android.permission.GET_TASKS")) {
                    SuspectedApp app = new SuspectedApp();
                    app.setPackageName(i.packageName);
                    final String applicationName = p.getApplicationLabel(i.applicationInfo).toString();
                    app.setAppName(applicationName);
                    listData.add(app);
                    if (suspectedAppDao != null) {
                        try {
                            suspectedAppDao.createIfNotExists(app);
                        } catch (SQLException e) {
                            Log.e(TAG, "Failed to create suspected app " + app.getPackageName(), e);
                        }
                    }
                }
            }
        }

        settings.setSuspectedAppUpdateTimestamp(refreshTimestamp);
        SharedPreferences.Editor editor = prefs.edit();
        String serialized = gson.toJson(settings);
        editor.putString(Settings.KEY_SETTINGS, serialized);
        editor.apply();
    }
}
