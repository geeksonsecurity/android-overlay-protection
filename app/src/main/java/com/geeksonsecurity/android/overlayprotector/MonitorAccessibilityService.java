package com.geeksonsecurity.android.overlayprotector;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.geeksonsecurity.android.overlayprotector.database.DatabaseHelper;
import com.geeksonsecurity.android.overlayprotector.domain.DetectedOverlay;
import com.geeksonsecurity.android.overlayprotector.domain.EventCounter;
import com.geeksonsecurity.android.overlayprotector.domain.OverlayState;
import com.geeksonsecurity.android.overlayprotector.domain.ServiceCommunication;
import com.geeksonsecurity.android.overlayprotector.domain.Settings;
import com.geeksonsecurity.android.overlayprotector.domain.WhiteEntry;
import com.geeksonsecurity.android.overlayprotector.service.AbstractDetectionEngine;
import com.geeksonsecurity.android.overlayprotector.service.IOverlayNotifyService;
import com.geeksonsecurity.android.overlayprotector.service.ProcessHelper;
import com.google.gson.Gson;
import com.j256.ormlite.dao.Dao;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class MonitorAccessibilityService extends AccessibilityService implements IOverlayNotifyService {

    public static final int NOTIFICATION_ID = 666999;
    private static final String TAG = "MonitorService";

    private final Gson _gson = new Gson();
    private final EventCounter _eventCounter = new EventCounter();

    private List<WhiteEntry> _whiteEntries = new ArrayList<>();
    private DatabaseHelper _helper = null;
    private Settings _settings = new Settings();
    private NotificationCompat.Builder _notificationBuilder;

    private NotificationManager _notificationManager;
    private ResultReceiver _resultReceiver = null;
    private AbstractDetectionEngine _detectionEngine;
    private KeyguardManager _keyguardManager;


    @Override
    protected void onServiceConnected() {
        refreshSettings();
        _keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

        _helper = DatabaseHelper.getHelper(getApplicationContext());

        InitializeDetectionEngine();


        _notificationBuilder = new NotificationCompat.Builder(getApplicationContext());
        _notificationBuilder.setSmallIcon(R.drawable.ic_notification_small).setContentTitle("Overlay Protector").setContentText("Running...");
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_circle);

        _notificationBuilder.setLargeIcon(largeIcon);

        Intent notificationIntent = new Intent(this,
                MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        _notificationBuilder.setContentIntent(pending);
        _notificationBuilder.setOngoing(true);
        _notificationBuilder.setShowWhen(false);
        _notificationBuilder.setPriority(Notification.PRIORITY_LOW);
        _notificationBuilder.setOnlyAlertOnce(true);

        _notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        _notificationManager.notify(NOTIFICATION_ID, _notificationBuilder.build());

        super.onServiceConnected();
    }

    private void InitializeDetectionEngine() {
        Class clazz = _settings.getDetectionEngine().getDetectionClass();
        if (_detectionEngine != null) {
            if (clazz.isInstance(_detectionEngine)) {
                return;
            }
        }

        try {
            ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(ACTIVITY_SERVICE);
            ProcessHelper processHelper = new ProcessHelper(activityManager);
            _detectionEngine = (AbstractDetectionEngine) clazz
                    .getConstructor(Context.class, DatabaseHelper.class, KeyguardManager.class, IOverlayNotifyService.class, ProcessHelper.class)
                    .newInstance(getApplicationContext(), _helper, _keyguardManager, this, processHelper);
        } catch (Exception e) {
            Log.e(TAG, "Failed to instantiate detection engine", e);
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (_helper == null)
            _helper = DatabaseHelper.getHelper(getApplicationContext());

        if (null != intent) {
            if (intent.getAction() != null) {
                Log.i(TAG, "Received intent action " + intent.getAction());
                if (intent.getAction().equals(ServiceCommunication.MSG_SETTINGS_UPDATED)) {
                    Log.i(TAG, "Settings updated!");
                    refreshSettings();
                    InitializeDetectionEngine();
                    if (_detectionEngine != null) {
                        _detectionEngine.refreshSettings();
                    }
                } else if (intent.getAction().equals(ServiceCommunication.MSG_WHITELIST_UPDATED)) {
                    Log.i(TAG, "White-list updated!");
                    if (_detectionEngine != null) {
                        _detectionEngine.refreshWhiteList();
                    }
                }
            }

            _resultReceiver = intent.getParcelableExtra("receiver");
        }
        return super.onStartCommand(intent, flags, startId);
    }


    public void refreshSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        AtomicReference<String> json = new AtomicReference<>(prefs.getString(Settings.KEY_SETTINGS, ""));
        assert json.get() != null;
        if (!json.get().isEmpty()) {
            _settings = _gson.fromJson(json.get(), Settings.class);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        _detectionEngine.handleEvent(event);
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Interrupt");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.cancel(NOTIFICATION_ID);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.cancel(NOTIFICATION_ID);
        return super.onUnbind(intent);
    }

    @Override
    public void processOverlayState(final OverlayState state) {
        if (!state.isHasOverlay())
            return;

        try {
            String[] command = {"logcat", "-v", "time", "-t", "300", "-s", "MonitorService"};
            // Load logcat output to additional information
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.insert(0, line + "\n");
            }

            state.setAdditionalInfo(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.i(TAG, "showOverlayWarning");
        state.setHasOverlay(true);
        state.setOverlayShown(true);

        Log.i(TAG, "showOverlayWarning: layout parameter set");

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);

        final WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        final View oView = View.inflate(getApplicationContext(), R.layout.overlaydetected, null);

        if (oView != null) {
            final TextView offender = (TextView) oView.findViewById(R.id.odOffenderNameLabel);
            final String offenderName = state.getOffender();

            final PackageManager pm = getApplicationContext().getPackageManager();
            ApplicationInfo ai;
            try {
                ai = pm.getApplicationInfo(offenderName, 0);
            } catch (final PackageManager.NameNotFoundException e) {
                ai = null;
            }
            final String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : offenderName);

            offender.setText(Html.fromHtml("<b>" + applicationName + "</b> <small> (" + offenderName + ")</small>"));

            ImageView iconImageView = (ImageView) oView.findViewById(R.id.iconImageView);

            Drawable icon;
            try {
                icon = getPackageManager().getApplicationIcon(offenderName);
            } catch (PackageManager.NameNotFoundException e) {
                icon = getResources().getDrawable(R.drawable.ic_question_icon);
            }
            iconImageView.setImageDrawable(icon);

            TextView uninstallWarning = (TextView) oView.findViewById(R.id.uninstallWarningTextView);
            String s = getResources().getString(R.string.op_uninstallWarning);
            s = String.format(s, _settings.getUninstallTimeoutSeconds());
            uninstallWarning.setText(s);

            Button ignore = (Button) oView.findViewById(R.id.odIgnoreButton);
            ignore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        // Add entry and update cache
                        Dao<WhiteEntry, Integer> whiteListDao = _helper.getWhiteListDao();
                        long duplicate = whiteListDao.queryBuilder().where().eq("name", offenderName).countOf();
                        if (duplicate == 0) {
                            WhiteEntry we = new WhiteEntry(offenderName, System.currentTimeMillis(), 1, false, true);
                            whiteListDao.create(we);
                            _whiteEntries.add(we);
                            Log.i(TAG,
                                    String.format("Added user white list entry for %s", we.getName()));
                        } else {
                            Log.w(TAG, String.format("White entry %s already in DB", offenderName));
                        }
                    } catch (SQLException e) {
                        Log.e(TAG,
                                String.format("Failed to update white list entry for %s", offenderName), e);
                    }
                    windowManager.removeViewImmediate(oView);
                    state.resetState();
                }
            });

            Button ignoreOnce = (Button) oView.findViewById(R.id.odIgnoreOnce);
            ignoreOnce.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    state.setIgnoreOncePackage(offenderName);
                    windowManager.removeViewImmediate(oView);
                    CountDownTimer cdt = new CountDownTimer(_settings.getIgnoreOnceTimeoutSeconds() * 1000, _settings.getIgnoreOnceTimeoutSeconds() * 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {

                        }

                        @Override
                        public void onFinish() {
                            Log.d(TAG, "Ignore once timeout expired, removing flag...");
                            state.resetState();
                        }
                    };
                    cdt.start();
                }
            });

            Button uninstall = (Button) oView.findViewById(R.id.odUninstallButton);
            uninstall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                    am.killBackgroundProcesses(offenderName);

                    try {
                        Dao<DetectedOverlay, Integer> detectedOverlayDao = _helper.getDetectedOverlayDao();
                        DetectedOverlay detectedOverlay = new DetectedOverlay(offenderName, System.currentTimeMillis());
                        detectedOverlayDao.create(detectedOverlay);
                        Log.i(TAG,
                                String.format("Added new detected overlay entry for offender %s", detectedOverlay.getOffender()));
                    } catch (SQLException e) {
                        Log.e(TAG,
                                String.format("Failed to add detected overlay entry for offender %s", offenderName), e);
                    }

                    Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_NEW_TASK);

                    String packageName = "package:" + offenderName;
                    intent.setData(Uri.parse(packageName));
                    Log.i(TAG,
                            String.format("Started uninstall intent for %s", packageName));
                    startActivity(intent);
                    windowManager.removeViewImmediate(oView);
                    state.setPendingUninstall(true);

                    final List<String> offenderProcesses = new ArrayList<>();
                    List<ActivityManager.RunningServiceInfo> runningServices = am.getRunningServices(1000);
                    for (ActivityManager.RunningServiceInfo s : runningServices) {
                        if (s.process.equals(state.getOffender())) {
                            offenderProcesses.add(s.service.getClassName());
                        }
                    }
                    offenderProcesses.add(state.getOffender());
                    CountDownTimer cdt = new CountDownTimer(_settings.getUninstallTimeoutSeconds() * 1000, 250) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            for (String o : offenderProcesses)
                                am.killBackgroundProcesses(o);
                        }

                        @Override
                        public void onFinish() {
                            Log.d(TAG, "Uninstall timeout expired, removing flag...");
                            state.setPendingUninstall(false);
                        }
                    };
                    state.resetState();
                    Log.d(TAG, "Starting uninstall countdown!");
                    cdt.start();

                }
            });

            Log.i(TAG, "!!!  OVERLAY WARNING VIEW ADDED !!!");
            windowManager.addView(oView, params);
        }
    }

    @Override
    public void updateNotificationCount(long lastMinuteEventCount) {
        _notificationBuilder.setContentText(String.format("Secured %d UI events in the last minute!", lastMinuteEventCount));
        _notificationManager.notify(NOTIFICATION_ID, _notificationBuilder.build());
    }
}
