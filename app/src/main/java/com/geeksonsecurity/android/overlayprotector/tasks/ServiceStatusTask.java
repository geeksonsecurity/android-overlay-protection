package com.geeksonsecurity.android.overlayprotector.tasks;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.os.AsyncTask;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import java.util.List;

public class ServiceStatusTask extends AsyncTask<Void, Void, Boolean> {

    private AccessibilityManager accessibilityManager;
    private IServiceStatusProcessor listener;
    private Context context;

    public ServiceStatusTask(Context context, IServiceStatusProcessor serviceStatusProcessor) {
        this.context = context;
        accessibilityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        listener = serviceStatusProcessor;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        boolean running = false;
        List<AccessibilityServiceInfo> runningServices = accessibilityManager
                .getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK);
        for (AccessibilityServiceInfo service : runningServices) {
            if (service.getId().contains(context.getApplicationContext().getPackageName())) {
                running = true;
                break;
            }
        }
        return running;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        listener.processResult(result);
    }


}
