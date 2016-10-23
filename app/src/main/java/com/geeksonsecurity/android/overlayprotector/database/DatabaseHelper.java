package com.geeksonsecurity.android.overlayprotector.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.geeksonsecurity.android.overlayprotector.domain.DetectedOverlay;
import com.geeksonsecurity.android.overlayprotector.domain.SuspectedApp;
import com.geeksonsecurity.android.overlayprotector.domain.WhiteEntry;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;


public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

    private static DatabaseHelper instance;

    // name of the database file for your application -- change to something appropriate for your app
    private static final String DATABASE_NAME = "overlayprotector.db";
    // any time you make changes to your database objects, you may have to increase the database version
    private static final int DATABASE_VERSION = 2;
    private final String TAG = DatabaseHelper.class.getSimpleName();
    private Dao<WhiteEntry, Integer> whiteListDao = null;
    private Dao<DetectedOverlay, Integer> detectedOverlayDao = null;
    private Dao<SuspectedApp, Integer> suspectedAppDao = null;

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * This is called when the database is first created. Usually you should call createTable statements here to create
     * the tables that will store your data.
     */
    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            Log.i(TAG, "onCreate");
            TableUtils.createTable(connectionSource, WhiteEntry.class);
            TableUtils.createTable(connectionSource, SuspectedApp.class);
            TableUtils.createTable(connectionSource, DetectedOverlay.class);
        } catch (SQLException e) {
            Log.e(TAG, "Can't create database", e);
            throw new RuntimeException(e);
        }

        // Add system whitelist entries
        Dao<WhiteEntry, Integer> dao;
        try {
            dao = getWhiteListDao();
            long now = System.currentTimeMillis();

            String[] androidSystemSinglePackages = new String[]{"android", "system"};
            String[] androidSystemPackages = new String[]{"android.", "com.android", "system.", "com.google."};

            for (String i : androidSystemSinglePackages) {
                WhiteEntry w1 = new WhiteEntry(i, now, 0, true, true);
                dao.create(w1);
            }

            for (String i : androidSystemPackages) {
                WhiteEntry w1 = new WhiteEntry(i, now, 0, true, false);
                dao.create(w1);
            }
            Log.i(TAG, "created system whitelist entries in onCreate");
        } catch (SQLException e) {
            Log.e(TAG, "Exception adding system whitelist entries", e);
        }

    }

    /**
     * This is called when your application is upgraded and it has a higher version number. This allows you to adjust
     * the various data to match the new version number.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            Log.i(TAG, "onUpgrade");
            TableUtils.dropTable(connectionSource, SuspectedApp.class, true);
            Log.d(TAG, String.format("Dropped suspected app table!"));
            Dao<WhiteEntry, Integer> whiteListDao = getWhiteListDao();
            DeleteBuilder<WhiteEntry, Integer> deleteBuilder = whiteListDao.deleteBuilder();
            deleteBuilder.where().eq("systemEntry", Boolean.TRUE);
            int deleted = deleteBuilder.delete();
            Log.d(TAG, String.format("Delete %d old system whitelist entries", deleted));
            onCreate(db, connectionSource);
        } catch (SQLException e) {
            Log.e(TAG, "Can't drop databases", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the Database Access Object (DAO) for our SimpleData class. It will create it or just give the cached
     * value.
     */
    public Dao<WhiteEntry, Integer> getWhiteListDao() throws SQLException {
        if (whiteListDao == null) {
            whiteListDao = getDao(WhiteEntry.class);
        }
        return whiteListDao;
    }

    public Dao<DetectedOverlay, Integer> getDetectedOverlayDao() throws SQLException {
        if (detectedOverlayDao == null) {
            detectedOverlayDao = getDao(DetectedOverlay.class);
        }
        return detectedOverlayDao;
    }

    public Dao<SuspectedApp, Integer> getSuspectedAppDao() throws SQLException {
        if (suspectedAppDao == null) {
            suspectedAppDao = getDao(SuspectedApp.class);
        }
        return suspectedAppDao;
    }

    /**
     * Close the database connections and clear any cached DAOs.
     */
    @Override
    public void close() {
        super.close();
        whiteListDao = null;
    }

    public static synchronized DatabaseHelper getHelper(Context context) {
        if (instance == null)
            instance = new DatabaseHelper(context);
        return instance;
    }

}
