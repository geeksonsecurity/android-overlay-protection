package com.geeksonsecurity.android.overlayprotector.domain;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;


@DatabaseTable(tableName = "detected_overlays")
public class DetectedOverlay {
    @DatabaseField(generatedId = true)
    public int id;

    @DatabaseField(index = true)
    private String offender;

    @DatabaseField
    private long timestamp;

    public DetectedOverlay() {

    }

    public DetectedOverlay(String offender, long timestamp) {
        this.offender = offender;
        this.timestamp = timestamp;
    }

    public String getOffender() {
        return offender;
    }

    public void setOffender(String offender) {
        this.offender = offender;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return getTimestamp() + ": " + getOffender();
    }
}
