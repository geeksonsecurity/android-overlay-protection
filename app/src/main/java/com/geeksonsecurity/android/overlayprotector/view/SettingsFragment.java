package com.geeksonsecurity.android.overlayprotector.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.geeksonsecurity.android.overlayprotector.MainActivity;
import com.geeksonsecurity.android.overlayprotector.MonitorAccessibilityService;
import com.geeksonsecurity.android.overlayprotector.R;
import com.geeksonsecurity.android.overlayprotector.domain.DetectionEngine;
import com.geeksonsecurity.android.overlayprotector.domain.ServiceCommunication;
import com.geeksonsecurity.android.overlayprotector.domain.Settings;
import com.google.gson.Gson;

public class SettingsFragment extends Fragment {
    Gson gson = new Gson();

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_settings, container, false);

        loadValues(view);

        // Load handlers for buttons

        Button startAccessibilityBtn = (Button) view.findViewById(R.id.overlayStartAccessibiltyBtn);

        startAccessibilityBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivityForResult(intent, 0);
            }
        });

        Button resetDefault = (Button) view.findViewById(R.id.resetDefaultButton);
        resetDefault.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context ctx = getActivity().getApplicationContext();
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

                String serialized = gson.toJson(new Settings());
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(Settings.KEY_SETTINGS, serialized);
                editor.apply();
                loadValues(view);
                notifySettingsChanged();
            }
        });

        Button showOverlay = (Button) view.findViewById(R.id.showOverlayButton);
        showOverlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT);

                final WindowManager windowManager = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);

                LayoutInflater li = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View oView = li.inflate(R.layout.overlaydetected, null);

                Button ignore = (Button) oView.findViewById(R.id.odIgnoreButton);
                ignore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        windowManager.removeView(oView);
                    }
                });

                Button uninstall = (Button) oView.findViewById(R.id.odUninstallButton);
                uninstall.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        windowManager.removeView(oView);
                    }
                });

                windowManager.addView(oView, params);
            }
        });

        return view;

    }

    private void loadValues(View view) {
        Context ctx = getActivity().getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String json = prefs.getString(Settings.KEY_SETTINGS, "");
        Settings settings = new Settings();

        if (!(json.isEmpty())) {
            settings = gson.fromJson(json, Settings.class);
        }

        CheckBox nonSuspectedAppModeCheckbox = (CheckBox) view.findViewById(R.id.enabledAdvancedProtectionCheckBox);
        nonSuspectedAppModeCheckbox.setChecked(settings.isAdvancedMode());


        SeekBar uninstallSeekBar = (SeekBar) view.findViewById(R.id.uninstallTimeoutSeekBar);
        final TextView uninstallValue = (TextView) view.findViewById(R.id.uninstallValueTextView);
        final String sec = getResources().getString(R.string.od_seconds_label);
        uninstallValue.setText(String.format(sec, settings.getUninstallTimeoutSeconds()));
        uninstallSeekBar.setProgress(settings.getUninstallTimeoutSeconds());
        uninstallSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 5) {
                    progress = 5;
                }
                seekBar.setProgress(progress);
                uninstallValue.setText(String.format(sec, progress));

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        SeekBar ignoreSeekBar = (SeekBar) view.findViewById(R.id.ignoreOnceTimeoutSeekBar);
        final TextView ignoreValue = (TextView) view.findViewById(R.id.ignoreOnceValueTextView);
        ignoreValue.setText(String.format(sec, settings.getIgnoreOnceTimeoutSeconds()));
        ignoreSeekBar.setProgress(settings.getIgnoreOnceTimeoutSeconds());
        ignoreSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 5) {
                    progress = 5;
                }
                seekBar.setProgress(progress);
                ignoreValue.setText(String.format(sec, progress));

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        Spinner detectionEngineSpinner = (Spinner) view.findViewById(R.id.detectionEngineSpinner);
        ArrayAdapter<DetectionEngine> detectionEngineArrayAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1, DetectionEngine.values());
        detectionEngineSpinner.setAdapter(detectionEngineArrayAdapter);
        detectionEngineSpinner.setSelection(detectionEngineArrayAdapter.getPosition(settings.getDetectionEngine()));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.settings_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.op_settings_menu_save:
                save();
                break;
            default:
                break;
        }
        return false;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(5);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void save() {
        View view = getView();
        if (view != null) {
            Settings settings = new Settings();

            CheckBox nonSuppectedAppModeCheckbox = (CheckBox) view.findViewById(R.id.enabledAdvancedProtectionCheckBox);
            settings.setAdvancedMode(nonSuppectedAppModeCheckbox.isChecked());

            SeekBar uninstallSeekbar = (SeekBar) view.findViewById(R.id.uninstallTimeoutSeekBar);
            settings.setUninstallTimeoutSeconds(uninstallSeekbar.getProgress());

            SeekBar ignoreSeekbak = (SeekBar) view.findViewById(R.id.ignoreOnceTimeoutSeekBar);
            settings.setIgnoreOnceTimeoutSeconds(ignoreSeekbak.getProgress());

            Spinner detectionEngineSpinner = (Spinner) view.findViewById(R.id.detectionEngineSpinner);
            DetectionEngine selectedItem = (DetectionEngine) detectionEngineSpinner.getSelectedItem();
            settings.setDetectionEngine(selectedItem);

            Context ctx = getActivity().getApplicationContext();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

            String serialized = gson.toJson(settings);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(Settings.KEY_SETTINGS, serialized);
            editor.apply();
            Toast.makeText(ctx, "Settings saved!", Toast.LENGTH_SHORT).show();
            notifySettingsChanged();
        }
    }

    private void notifySettingsChanged() {
        Intent intent = new Intent(getActivity(), MonitorAccessibilityService.class);
        intent.setAction(ServiceCommunication.MSG_SETTINGS_UPDATED);
        getActivity().startService(intent);
    }
}