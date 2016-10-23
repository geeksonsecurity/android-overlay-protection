package com.geeksonsecurity.android.overlayprotector.service;

import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;
import android.util.LogPrinter;
import android.view.accessibility.AccessibilityEvent;

import com.geeksonsecurity.android.overlayprotector.database.DatabaseHelper;
import com.geeksonsecurity.android.overlayprotector.domain.EventCounter;
import com.geeksonsecurity.android.overlayprotector.domain.OverlayState;
import com.geeksonsecurity.android.overlayprotector.domain.ServiceCommunication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BaseDetectionEngine extends AbstractDetectionEngine {
    private static final String TAG = BaseDetectionEngine.class.getSimpleName();

    private final OverlayState _overlayState = new OverlayState();
    private final EventCounter _eventCounter = new EventCounter();
    private String _currentProcess = "";

    private KeyguardManager _keyguardManager;
    private ResultReceiver _resultReceiver = null;

    private String previousEventPackage = "";
    private List<String> _layoutClasses = new ArrayList<>(Arrays.asList("android.view.ViewGroup", "android.widget.LinearLayout", "android.widget.RelativeLayout", "android.widget.FrameLayout", "android.widget.GridLayout"));


    public BaseDetectionEngine(Context context, DatabaseHelper helper, KeyguardManager keyguardManager, IOverlayNotifyService notifyService, ProcessHelper processHelper) {
        super(context, helper, notifyService, processHelper);
        _keyguardManager = keyguardManager;
    }

    @Override
    public void handleEvent(AccessibilityEvent event) {
        // Avoid processing events when screen is locked
        if (_keyguardManager != null) {
            boolean locked = _keyguardManager.inKeyguardRestrictedInputMode();
            if (locked) {
                Log.i(TAG, "Screen locked, skipping overlay check!");
                return;
            }
        }

        Log.d(TAG, String.format("New event %s", event.toString()));
        _eventCounter.newEvent();
        _notifyService.updateNotificationCount(_eventCounter.getLastMinuteEventCount());
        if (_resultReceiver != null) {
            Bundle bundle = new Bundle();
            bundle.putLong("eventCount", _eventCounter.getLastMinuteEventCount());
            _resultReceiver.send(ServiceCommunication.MSG_EVENT_COUNT_UPDATE, bundle);
        }


        // When overlay is detected avoid performing useless computation
        if (_overlayState.isHasOverlay() || _overlayState.isPendingUninstall())
            return;

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.getPackageName() == null)
                return;

            String eventPackage = event.getPackageName().toString();
            ComponentName componentName = new ComponentName(
                    eventPackage,
                    event.getClassName().toString()
            );
            ActivityInfo activityInfo = tryGetActivity(componentName);
            boolean isActivity = activityInfo != null;
            if (isActivity) {
                LogPrinter logPrinter = new LogPrinter(Log.DEBUG, TAG);
                activityInfo.dump(logPrinter, "");
            }
            String className = event.getClassName().toString();

            // Perform detection
            boolean parentAvailable = event.getSource() != null ? event.getSource().getParent() != null : false;

            Log.d(TAG, String.format("Collected info isActivity %s, parentAvailable: %s", String.valueOf(isActivity), String.valueOf(parentAvailable)));

            if (_overlayState.getIgnoreOncePackage().equals(eventPackage)) {
                Log.d(TAG, String.format("Package %s ignored once", eventPackage));
            } else if (eventPackage.equals(previousEventPackage)) {
                Log.d(TAG, String.format("Last two event have the same package %s, skipping check!", eventPackage));
            } else if (_layoutClasses.contains(className) && !isActivity && !parentAvailable) {
                Log.d(TAG, String.format("Detected suspicious class %s without activity and parent for process %s, checking whitelist", className, eventPackage));
                if (!checkWhitelistHit(eventPackage)) {
                    Log.d(TAG, "No whitelist entry found");
                    if (checkSuspectedApps(eventPackage)) {
                        Log.d(TAG, String.format("******* VIEW OVERLAY DETECTED!!!"));
                        _overlayState.setOffender(eventPackage);
                        _overlayState.setProcess(_currentProcess);
                        _notifyService.processOverlayState(_overlayState);
                    }
                } else {
                    Log.d(TAG, "Whitelist hit skipping!");
                }
            } else if (isActivity && activityInfo.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE && !parentAvailable) {
                Log.d(TAG, String.format("Detected suspicious activity %s with single instance flag, checking whitelist", activityInfo.packageName));
                if (!checkWhitelistHit(eventPackage)) {
                    Log.d(TAG, "No whitelist entry found");
                    if (checkSuspectedApps(eventPackage)) {
                        Log.d(TAG, String.format("******* ACTIVITY OVERLAY DETECTED!!!"));
                        _overlayState.setOffender(eventPackage);
                        _overlayState.setProcess(_currentProcess);
                        _notifyService.processOverlayState(_overlayState);
                    }
                } else {
                    Log.d(TAG, "Whitelist hit skipping!");
                }
            }
            previousEventPackage = eventPackage;
        }
    }
}
