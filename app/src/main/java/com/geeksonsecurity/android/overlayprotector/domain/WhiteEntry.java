package com.geeksonsecurity.android.overlayprotector.domain;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;


@DatabaseTable(tableName = "whitelist")
public class WhiteEntry {
    @DatabaseField(generatedId = true)
    public int id;

    @DatabaseField(index = true)
    private String name;

    public WhiteEntry() {

    }

    public WhiteEntry(String name, long addedTimestamp, int hitCount, boolean systemEntry, boolean exactMatch) {
        this.name = name;
        this.addedTimestamp = addedTimestamp;
        this.hitCount = hitCount;
        this.systemEntry = systemEntry;
        this.exactMatch = exactMatch;
    }

    public WhiteEntry(String name, long addedTimestamp) {
        this(name, addedTimestamp, 0, false, false);
    }

    @DatabaseField
    private long addedTimestamp;

    @DatabaseField
    private int hitCount;

    @DatabaseField
    private boolean systemEntry;

    @DatabaseField
    private boolean exactMatch;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getAddedTimestamp() {
        return addedTimestamp;
    }

    public void setAddedTimestamp(long addedTimestamp) {
        this.addedTimestamp = addedTimestamp;
    }

    public int getHitCount() {
        return hitCount;
    }

    public void setHitCount(int hitCount) {
        this.hitCount = hitCount;
    }

    public boolean isSystemEntry() {
        return systemEntry;
    }

    public void setSystemEntry(boolean systemEntry) {
        this.systemEntry = systemEntry;
    }

    public boolean isExactMatch() {
        return exactMatch;
    }

    public void setExactMatch(boolean exactMatch) {
        this.exactMatch = exactMatch;
    }
}
