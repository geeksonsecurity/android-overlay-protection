package com.geeksonsecurity.android.overlayprotector.service;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.geeksonsecurity.android.overlayprotector.database.DatabaseHelper;
import com.geeksonsecurity.android.overlayprotector.domain.ConcurrentArrayList;
import com.geeksonsecurity.android.overlayprotector.domain.EventCounter;
import com.geeksonsecurity.android.overlayprotector.domain.OverlayState;
import com.geeksonsecurity.android.overlayprotector.domain.ServiceCommunication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class AdvancedDetectionService extends AbstractDetectionEngine {

    private static final String TAG = AdvancedDetectionService.class.getSimpleName();

    private final OverlayState _overlayState = new OverlayState();
    private final EventCounter _eventCounter = new EventCounter();
    private final ConcurrentArrayList<CountDownTimer> _eventTimers = new ConcurrentArrayList<>();

    private String _oldProcess = "";
    private String _currentProcess = "";

    private KeyguardManager _keyguardManager;
    private ResultReceiver _resultReceiver = null;
    private List<String> _baseProcesses = new ArrayList<>(Arrays.asList("com.google.android.googlequicksearchbox", "com.sec.android.app.launcher"));

    public AdvancedDetectionService(Context context, DatabaseHelper helper, KeyguardManager keyguardManager, IOverlayNotifyService notifyService, ProcessHelper processHelper) {
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

        // When overlay is detected avoid performing useless computation
        if (_overlayState.isHasOverlay())
            return;

        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            CountDownTimer eventTimer = new CountDownTimer(100, 100) {

                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {
                    updateProcessName("");
                    Log.d(TAG, String.format("New process: " + _currentProcess));
                }
            };
            eventTimer.start();
        }

        final String eventDescription = event.toString();
        final AccessibilityNodeInfo info = event.getSource();
        final String source = info != null ? info.getClassName().toString() : "";
        final String eventPackage = event.getPackageName() != null ? event.getPackageName().toString() : "";
        final int eventType = event.getEventType();

        long postponeMs = _settings.getEventProcessingDelayMilliSeconds();
        Log.i(TAG, String.format("Postponing event %d of %d ms, %s", eventType, postponeMs, eventDescription));

        /*if(event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            AccessibilityNodeInfo parent = info.getParent();
            if (parent != null) {
                Log.d(TAG, "Node parent: " + parent.toString());
                int child = info.getChildCount();
                for (int i = 0; i < child; i++) {
                    AccessibilityNodeInfo childNodeView = parent.getChild(i);
                    Log.d(TAG, childNodeView.toString());
                }
            } else {
                Log.d(TAG, String.format("Event %s has no parent", info.toString()));
            }
        }*/

        // Postpone events to avoid false positive during application startup
        CountDownTimer eventTimer = new CountDownTimer(postponeMs, postponeMs) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                /*if (eventPackage.equals(_oldProcess)) {
                    Log.d(TAG, String.format("Event " + eventType + " processing avoided since still owned by old ng%s", _oldProcess));
                    return;
                }*/

                if (!_eventTimers.contains(this)) {
                    Log.d(TAG, "Event " + eventType + " processing avoided since timer event has been canceled");
                    return;
                }

                // No one would inject on a base process right?
                if (_baseProcesses.contains(_currentProcess)) {
                    Log.d(TAG, String.format("Event %s processing aborted since its base process %s since its a base process", eventType, _currentProcess));
                    return;
                }

                if (eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
                    Log.i(TAG, "*** WINDOW CHANGED! ***");

                    if (_settings.isDetectOnTypeWindowsChanged()) {
                        Log.i(TAG, String.format("Checking %s for overlay: %s, %s", _currentProcess, source, eventPackage));
                        checkForOverlay(_currentProcess, source, eventPackage);
                    }


                } else if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                    Log.i(TAG, "*** WINDOW CONTENT CHANGED! ***");

                    if (_settings.isDetectOnTypeWindowContentChanged()) {
                        Log.i(TAG, String.format("Checking %s for overlay: %s, %s", _currentProcess, source, eventPackage));
                        checkForOverlay(_currentProcess, source, eventPackage);
                    }

                } else if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    Log.i(TAG, "PopupWindow, Menu or Dialog opened");

                    if (_settings.isDetectOnTypeWindowStateChanged()) {
                        checkForOverlay(_currentProcess, source, eventPackage);

                    }
                } else {
                    Log.w(TAG, String.format("Unknown event %d", eventType));
                }

                Log.i(TAG, String.format("Event %d, %s", eventType, eventDescription));
                _eventTimers.remove(this);
                Log.i(TAG, String.format("Finished processing event %d", eventType));
            }
        };
        _eventTimers.add(eventTimer);
        eventTimer.start();

        if (_eventCounter.getLastMinuteEventCount() % 5 == 0) {
            _notifyService.updateNotificationCount(_eventCounter.getLastMinuteEventCount());

            if (_resultReceiver != null) {
                Bundle bundle = new Bundle();
                bundle.putLong("eventCount", _eventCounter.getLastMinuteEventCount());
                _resultReceiver.send(ServiceCommunication.MSG_EVENT_COUNT_UPDATE, bundle);
            }
        }
    }

    private String updateProcessName(String procName) {
        String foregroundProcess = procName;
        if (procName.isEmpty()) {
            foregroundProcess = _processHelper.getForegroundApp();
            if (foregroundProcess.isEmpty()) {
                Log.w(TAG, String.format("*** Unable to resolve foreground process"));
            } else {
                Log.i(TAG, String.format("--- Process %s", foregroundProcess));
            }
        }
        // Just for the first time!
        if (_currentProcess.isEmpty()) {
            _currentProcess = foregroundProcess;
        }

        if (!foregroundProcess.equals(_currentProcess)) {
            _oldProcess = _currentProcess;
            _currentProcess = foregroundProcess;
            Log.i(TAG, String.format("--- PROCESS CHANGED ---- From %s to %s", _oldProcess, foregroundProcess));
            // If is not starting
            if (!_baseProcesses.contains(_oldProcess)) {
                clearEventTimers();
            }
        }

        return _currentProcess;
    }

    private void clearEventTimers() {
        Iterator<CountDownTimer> it = _eventTimers.iterator();

        Log.i(TAG, "All event timers canceled and cleared! Count " + _eventTimers.size());
        _eventTimers.clear();

        while (it.hasNext()) {
            it.next().cancel();
        }

    }

    private void checkForOverlay(final String process, final String... compares) {
        _eventCounter.newEvent();
        if (_overlayState.isHasOverlay()) {
            Log.d(TAG, "checkForOverlay: Overlay already detected, skipping check");
            return;
        } else if (process.isEmpty()) {
            Log.d(TAG, "checkForOverlay: Process is empty");
            return;
        }

        boolean result = false;
        for (String source : compares) {
            result = true;
            if (source.isEmpty()) {
                Log.d(TAG, "Source empty continuing");
                continue;
            }

            // Is our overlay detected?
            if (source.startsWith(_context.getApplicationContext().getPackageName())) {
                Log.d(TAG, String.format("Source contains same application package as our: %s", _context.getApplicationContext().getPackageName()));
                continue;
            }

            if (!checkWhitelistHit(source)) {
                Log.d(TAG, String.format("Comparing %s with process %s", source, _overlayState.getProcess()));
                if (source.startsWith(process)) {
                    Log.d(TAG, String.format("Source %s match foreground package %s", source, process));
                    result = false;
                }

                if (result && checkSuspectedApps(source)) {
                    Log.d(TAG, String.format("Overlay found by package %s over %s", source, process));
                    _overlayState.setOffender(source);
                    _overlayState.setProcess(process);
                    result = true;
                    break;
                }
            }
        }

        if (result && !_overlayState.getOffender().isEmpty() && !_overlayState.isOverlayShown()) {
            Log.w(TAG, "Overlay by " + _overlayState.getOffender());
            _overlayState.setHasOverlay(true);
            clearEventTimers();
            _notifyService.processOverlayState(_overlayState);
        }
    }
}
