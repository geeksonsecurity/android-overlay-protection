package com.geeksonsecurity.android.overlayprotector.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.geeksonsecurity.android.overlayprotector.MonitorAccessibilityService;
import com.geeksonsecurity.android.overlayprotector.R;
import com.geeksonsecurity.android.overlayprotector.database.DatabaseHelper;
import com.geeksonsecurity.android.overlayprotector.domain.ServiceCommunication;
import com.geeksonsecurity.android.overlayprotector.domain.WhiteEntry;
import com.j256.ormlite.dao.Dao;
import com.readystatesoftware.viewbadger.BadgeView;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class WhiteEntryAdapter extends ArrayAdapter<WhiteEntry> {

    private static final String TAG = WhiteEntryAdapter.class.getSimpleName();
    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    public WhiteEntryAdapter(Context c, List<WhiteEntry> items) {
        super(c, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        final WhiteEntry whiteEntry = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.whitelist_item, parent, false);
        }
        ImageView iconImageView = (ImageView) convertView.findViewById(R.id.iconImageView);
        TextView nameTextView = (TextView) convertView.findViewById(R.id.nameLabel);
        TextView timestampTextView = (TextView) convertView.findViewById(R.id.timestampLabel);
        // Populate the data into the template view using the data object
        nameTextView.setText(whiteEntry.getName() + (!whiteEntry.isExactMatch() ? "*" : ""));
        Drawable icon = getContext().getResources().getDrawable(whiteEntry.isSystemEntry() ? R.drawable.ic_system : R.drawable.ic_user);
        iconImageView.setImageDrawable(icon);

        if (whiteEntry.getHitCount() > 0) {
            BadgeView badge = new BadgeView(getContext(), iconImageView);
            badge.setText(whiteEntry.getHitCount() > 99 ? "99+" : String.valueOf(whiteEntry.getHitCount()));
            badge.show();
        }

        ImageView delete = (ImageView) convertView.findViewById(R.id.deleteImageView);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                // Set the dialog title
                builder.setTitle("Delete Confirmation");
                builder.setMessage("Delete selected white entry?");
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // Nothing
                    }
                });
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Dao<WhiteEntry, Integer> wed = DatabaseHelper.getHelper(getContext()).getWhiteListDao();
                            wed.delete(whiteEntry);
                            WhiteEntryAdapter.this.remove(whiteEntry);
                            notifyChangesToService();
                        } catch (SQLException e) {
                            Toast.makeText(getContext(), "Failed to delete entry!", Toast.LENGTH_SHORT);
                            Log.e(TAG, "Failed to delete whitelist entry", e);
                        }
                    }
                });
                builder.create().show();
            }
        });

        Date resultDate = new Date(whiteEntry.getAddedTimestamp());
        timestampTextView.setText(sdf.format(resultDate));
        // Return the completed view to render on screen
        return convertView;

    }

    private void notifyChangesToService() {
        Intent intent = new Intent(getContext(), MonitorAccessibilityService.class);
        intent.setAction(ServiceCommunication.MSG_WHITELIST_UPDATED);
        getContext().startService(intent);
    }

}