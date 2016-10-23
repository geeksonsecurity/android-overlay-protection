package com.geeksonsecurity.android.overlayprotector.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.geeksonsecurity.android.overlayprotector.database.DatabaseHelper;
import com.geeksonsecurity.android.overlayprotector.domain.Settings;
import com.geeksonsecurity.android.overlayprotector.domain.SuspectedApp;
import com.geeksonsecurity.android.overlayprotector.domain.WhiteEntry;
import com.google.gson.Gson;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


public abstract class AbstractDetectionEngine {

    private final String TAG = AbstractDetectionEngine.class.getSimpleName();

    protected final Context _context;
    protected final DatabaseHelper _helper;
    protected final IOverlayNotifyService _notifyService;
    protected final Gson _gson = new Gson();
    protected final ProcessHelper _processHelper;

    protected List<WhiteEntry> _whiteEntries = new ArrayList<>();
    protected Settings _settings = new Settings();


    public AbstractDetectionEngine(Context context, DatabaseHelper helper, IOverlayNotifyService notifyService, ProcessHelper processHelper) {

        _context = context;
        _helper = helper;
        _notifyService = notifyService;
        _processHelper = processHelper;

        // Initialize settings & whitelist entries
        refreshSettings();
        refreshWhiteList();
    }

    abstract public void handleEvent(AccessibilityEvent event);

    public void refreshWhiteList() {
        try {
            _whiteEntries = _helper.getWhiteListDao().queryForAll();
            Log.i(TAG, String.format("Loaded %d whitelist entries from database in cache", _whiteEntries.size()));
        } catch (SQLException e) {
            Log.e(TAG, "Failed to load white list entries", e);
        }
    }

    public void refreshSettings() {
        Context ctx = _context.getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        AtomicReference<String> json = new AtomicReference<>(prefs.getString(Settings.KEY_SETTINGS, ""));
        assert json.get() != null;
        if (!json.get().isEmpty()) {
            _settings = _gson.fromJson(json.get(), Settings.class);
        }
    }

    protected boolean checkWhitelistHit(String packageName) {
        boolean whitelistHit = false;
        for (WhiteEntry entry : _whiteEntries) {
            if (entry.isExactMatch() ? packageName.equals(entry.getName()) : packageName.startsWith(entry.getName())) {
                Log.d(TAG, "Whitelist entry hit " + entry.getName() + " saved " + packageName);
                whitelistHit = true;
                if (!entry.isSystemEntry()) {
                    entry.setHitCount(entry.getHitCount() + 1);
                    try {
                        _helper.getWhiteListDao().update(entry);
                        Log.i(TAG,
                                String.format("Updated white list entry for %s with new hit count of %d", entry.getName(), entry.getHitCount()));
                    } catch (SQLException e) {
                        Log.e(TAG,
                                String.format("Failed to update white list entry for %s with new hit count of %d", entry.getName(), entry.getHitCount()), e);
                    }
                }
                break;
            }
        }
        return whitelistHit;
    }

    protected boolean checkSuspectedApps(String packageName) {
        boolean matched = true;
        if (!_settings.isAdvancedMode()) {
            try {
                Dao<SuspectedApp, Integer> suspectedAppDao = DatabaseHelper.getHelper(_context.getApplicationContext()).getSuspectedAppDao();
                List<SuspectedApp> res = suspectedAppDao.queryForEq("packageName", packageName);
                if (res.size() == 0) {
                    Log.i(TAG, String.format("Skipping %s since not in suspected app list and is probably a FP", packageName));
                    matched = false;
                } else {
                    Log.d(TAG, String.format("Process %s found in suspected apps", packageName));
                    matched = true;
                }
            } catch (SQLException e) {
                Log.e(TAG, String.format("SQL exception: %s", e.getMessage()));
                matched = false;
            }
        }
        return matched;
    }


    protected ActivityInfo tryGetActivity(ComponentName componentName) {
        try {
            return _context.getPackageManager().getActivityInfo(componentName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

}
