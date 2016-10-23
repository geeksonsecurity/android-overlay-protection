package com.geeksonsecurity.android.overlayprotector.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.geeksonsecurity.android.overlayprotector.R;
import com.geeksonsecurity.android.overlayprotector.domain.SuspectedApp;

import java.text.SimpleDateFormat;
import java.util.List;

public class SuspectedAppsAdapter extends ArrayAdapter<SuspectedApp> {

    private static final String TAG = SuspectedAppsAdapter.class.getSimpleName();
    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    public SuspectedAppsAdapter(Context c) {
        super(c, 0);
    }

    public SuspectedAppsAdapter(Context c, List<SuspectedApp> items) {
        super(c, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        final SuspectedApp suspectedApp = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.suspectedapps_item, parent, false);
        }
        ImageView iconImageView = (ImageView) convertView.findViewById(R.id.iconImageView);
        TextView appName = (TextView) convertView.findViewById(R.id.appName);
        TextView packageName = (TextView) convertView.findViewById(R.id.packageName);
        // Populate the data into the template view using the data object
        appName.setText(suspectedApp.getAppName());
        packageName.setText(suspectedApp.getPackageName());
        Drawable icon;
        try {
            icon = getContext().getPackageManager().getApplicationIcon(suspectedApp.getPackageName());
        } catch (PackageManager.NameNotFoundException e) {
            icon = getContext().getResources().getDrawable(R.drawable.ic_question_icon);
        }
        iconImageView.setImageDrawable(icon);


        ImageView delete = (ImageView) convertView.findViewById(R.id.uninstallImageView);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_DELETE);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                String packageName = "package:" + suspectedApp.getPackageName();
                intent.setData(Uri.parse(packageName));
                Log.i(TAG,
                        String.format("Started uninstall intent for %s", packageName));
                getContext().startActivity(intent);
            }
        });

        // Return the completed view to render on screen
        return convertView;

    }

}