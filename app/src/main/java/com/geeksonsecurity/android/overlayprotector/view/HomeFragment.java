package com.geeksonsecurity.android.overlayprotector.view;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.geeksonsecurity.android.overlayprotector.MainActivity;
import com.geeksonsecurity.android.overlayprotector.MonitorAccessibilityService;
import com.geeksonsecurity.android.overlayprotector.R;
import com.geeksonsecurity.android.overlayprotector.database.DatabaseHelper;
import com.geeksonsecurity.android.overlayprotector.domain.DetectedOverlay;
import com.geeksonsecurity.android.overlayprotector.domain.ServiceCommunication;
import com.geeksonsecurity.android.overlayprotector.tasks.IServiceStatusProcessor;
import com.geeksonsecurity.android.overlayprotector.tasks.ServiceStatusTask;
import com.geeksonsecurity.android.overlayprotector.wizard.ConfigWizardActivity;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class HomeFragment extends Fragment {

    private static final String TAG = HomeFragment.class.getSimpleName();
    private static Timer checkStatusTimer;
    private Dao<DetectedOverlay, Integer> detectedOverlayDao = null;
    private CustomResultReceiver resultReceiver = null;
    NotificationManager _notificationManager = null;

    public static Fragment newInstance() {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

        try {
            detectedOverlayDao = DatabaseHelper.getHelper(getActivity()).getDetectedOverlayDao();
        } catch (SQLException e) {
            Log.e(TAG, "Unable to load detected overlay DAO", e);
        }

        resultReceiver = new CustomResultReceiver(null);
        Intent intent = new Intent(getActivity(), MonitorAccessibilityService.class);
        intent.putExtra("receiver", resultReceiver);
        getActivity().startService(intent);
    }

    @Override
    public void onPause() {
        super.onPause();
        checkStatusTimer.cancel();
    }

    @Override
    public void onResume() {
        super.onResume();

        getStatusTimer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                new ServiceStatusTask(getActivity(), new IServiceStatusProcessor() {
                    @Override
                    public void processResult(boolean running) {
                        View view = getView();
                        if (view != null) {
                            final Button enableProtectionBtn = (Button) view.findViewById(R.id.opEnableProtectionBtn);
                            final TextView checkServiceRunningLbl = (TextView) view.findViewById(R.id.checkServiceRunningLbl);
                            checkServiceRunningLbl.setTextColor(running ? Color.GREEN : Color.RED);
                            checkServiceRunningLbl.setText(running ? "RUNNING" : "NOT RUNNING");
                            if (!running && _notificationManager != null) {
                                _notificationManager.cancel(MonitorAccessibilityService.NOTIFICATION_ID);
                            }
                            enableProtectionBtn.setVisibility(running ? View.GONE : View.VISIBLE);
                        }
                    }
                }).execute();

                View view = getView();
                if (view != null) {
                    final TextView monthCount = (TextView) view.findViewById(R.id.monthlyDetectedCount);
                    final TextView suspectedAppCountTextView = (TextView) view.findViewById(R.id.suspectedAppsCount);
                    QueryBuilder<DetectedOverlay, Integer> qb = detectedOverlayDao.queryBuilder();
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(new Date());
                    cal.add(Calendar.MONTH, -1);
                    try {
                        final long overlayCount = qb.where().between("timestamp", cal.getTimeInMillis(), System.currentTimeMillis()).countOf();
                        final String overlayCountLabel = String.format(getString(R.string.detectedCount), overlayCount);

                        final long suspectedAppsCount = DatabaseHelper.getHelper(getActivity()).getSuspectedAppDao().countOf();
                        final String suspectedAppsCountLabel = String.format(getString(R.string.suspectedAppCount), suspectedAppsCount);

                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if (monthCount != null)
                                    monthCount.setText(overlayCountLabel);
                                if (suspectedAppCountTextView != null)
                                    suspectedAppCountTextView.setText(suspectedAppsCountLabel);
                            }
                        });
                    } catch (SQLException e) {
                        Log.e(TAG, "Unable to compute amount of detected overlay for the last month", e);
                    }
                }

            }
        }, 0, 2000);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        final Button enableProtectionBtn = (Button) rootView.findViewById(R.id.opEnableProtectionBtn);
        enableProtectionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ConfigWizardActivity.class);
                getActivity().startActivity(intent);
            }
        });

        TextView e = (TextView) rootView.findViewById(R.id.eventProcessedCount);
        String s = getString(R.string.eventCountLoading);
        e.setText(String.format(s, 0));

        return rootView;
    }

    public Timer getStatusTimer() {
        checkStatusTimer = new Timer();
        return checkStatusTimer;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(1);
    }

    class CustomResultReceiver extends ResultReceiver {
        public CustomResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, final Bundle resultData) {

            if (resultCode == ServiceCommunication.MSG_EVENT_COUNT_UPDATE) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        UpdateEventCount(resultData.getLong("eventCount"));
                    }
                });
            }
        }
    }

    private void UpdateEventCount(long eventCount) {
        View v = getView();
        if (v != null) {
            TextView e = (TextView) v.findViewById(R.id.eventProcessedCount);
            String s = getString(R.string.eventCount);
            e.setText(String.format(s, eventCount));
        }
    }

}
