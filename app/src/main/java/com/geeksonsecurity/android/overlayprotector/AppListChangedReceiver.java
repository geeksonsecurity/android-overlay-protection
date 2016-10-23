package com.geeksonsecurity.android.overlayprotector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.geeksonsecurity.android.overlayprotector.database.DatabaseHelper;
import com.geeksonsecurity.android.overlayprotector.domain.Settings;
import com.geeksonsecurity.android.overlayprotector.domain.SuspectedApp;
import com.google.gson.Gson;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;

import java.util.Arrays;
import java.util.List;

public class AppListChangedReceiver extends BroadcastReceiver {
    private String TAG = this.getClass().getSimpleName();
    private Gson gson = new Gson();

    public AppListChangedReceiver() {
        Log.i(TAG, "Initialized");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getData() == null) {
            return;
        }
        String pkg = intent.getData().toString();
        pkg = pkg.replace("package:", "");
        Log.i(TAG, String.format("Install/Uninstall intent for %s received!", pkg));
        try {
            Dao<SuspectedApp, Integer> suspectedAppDao = DatabaseHelper.getHelper(context).getSuspectedAppDao();

            if (intent.getAction() == Intent.ACTION_PACKAGE_ADDED) {
                try {
                    PackageManager pm = context.getPackageManager();
                    PackageInfo packageInfo = pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS);
                    if (packageInfo != null && packageInfo.requestedPermissions != null) {
                        List<String> list = Arrays.asList(packageInfo.requestedPermissions);
                        if (list.contains("android.permission.SYSTEM_ALERT_WINDOW") && list.contains("android.permission.GET_TASKS")) {
                            SuspectedApp app = new SuspectedApp();
                            app.setPackageName(packageInfo.packageName);
                            final String applicationName = pm.getApplicationLabel(packageInfo.applicationInfo).toString();
                            app.setAppName(applicationName);
                            suspectedAppDao.createIfNotExists(app);
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

            } else if (intent.getAction() == Intent.ACTION_PACKAGE_REMOVED) {
                DeleteBuilder db = suspectedAppDao.deleteBuilder();
                db.where().eq("packageName", pkg);
                suspectedAppDao.delete(db.prepare());
            }
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String json = prefs.getString(Settings.KEY_SETTINGS, "");
            Settings settings = gson.fromJson(json, Settings.class);
            settings.setSuspectedAppUpdateTimestamp(System.currentTimeMillis());
            String serialized = gson.toJson(settings);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(Settings.KEY_SETTINGS, serialized);
            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }
}
