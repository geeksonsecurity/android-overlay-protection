package com.geeksonsecurity.android.overlayprotector;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.geeksonsecurity.android.overlayprotector.database.DatabaseUtils;
import com.geeksonsecurity.android.overlayprotector.view.DetectedOverlayFragment;
import com.geeksonsecurity.android.overlayprotector.view.HomeFragment;
import com.geeksonsecurity.android.overlayprotector.view.SettingsFragment;
import com.geeksonsecurity.android.overlayprotector.view.SuspectedAppsFragment;
import com.geeksonsecurity.android.overlayprotector.view.WhiteListFragment;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    AppListChangedReceiver _packageUpdateReceiver = new AppListChangedReceiver();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(_packageUpdateReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");

        registerReceiver(_packageUpdateReceiver, intentFilter);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        restoreActionBar();

        DatabaseUtils.fillSuspectedApps(this);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.app_name);
                break;
            case 2:
                mTitle = getString(R.string.suspected_app_section);
                break;
            case 3:
                mTitle = getString(R.string.overlay_detected_section);
                break;
            case 4:
                mTitle = getString(R.string.whitelist_section);
                break;
            case 5:
                mTitle = getString(R.string.settings_section);
                break;
        }
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(mTitle);
        } else {
            setTitle(mTitle);
        }
    }


    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(mTitle);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setIcon(R.drawable.ic_launcher_transparent);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
       /* if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }*/
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static Fragment newInstance(int sectionNumber) {
            switch (sectionNumber) {
                case 1:
                    return HomeFragment.newInstance();
                case 2:
                    return SuspectedAppsFragment.newInstance();
                case 3:
                    return DetectedOverlayFragment.newInstance();
                case 4:
                    return WhiteListFragment.newInstance();
                case 5:
                    return SettingsFragment.newInstance();
            }

            return null;
        }
    }

}
