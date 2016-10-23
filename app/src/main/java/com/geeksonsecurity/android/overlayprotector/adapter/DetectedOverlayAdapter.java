package com.geeksonsecurity.android.overlayprotector.adapter;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.geeksonsecurity.android.overlayprotector.R;
import com.geeksonsecurity.android.overlayprotector.domain.DetectedOverlay;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class DetectedOverlayAdapter extends ArrayAdapter<DetectedOverlay> {

    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    public DetectedOverlayAdapter(Context c, List<DetectedOverlay> items) {
        super(c, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        DetectedOverlay detectedOverlay = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.detectedoverlay_item, parent, false);
        }
        ImageView iconImageView = (ImageView) convertView.findViewById(R.id.iconImageView);
        TextView offenderTextView = (TextView) convertView.findViewById(R.id.offenderLabel);
        TextView timestampTextView = (TextView) convertView.findViewById(R.id.timestampLabel);
        // Populate the data into the template view using the data object
        offenderTextView.setText(detectedOverlay.getOffender());
        Drawable icon;
        try {
            icon = getContext().getPackageManager().getApplicationIcon(detectedOverlay.getOffender());
        } catch (PackageManager.NameNotFoundException e) {
            icon = getContext().getResources().getDrawable(R.drawable.ic_question_icon);
        }
        iconImageView.setImageDrawable(icon);
        Date resultdate = new Date(detectedOverlay.getTimestamp());
        timestampTextView.setText(sdf.format(resultdate));
        // Return the completed view to render on screen
        return convertView;

    }

}