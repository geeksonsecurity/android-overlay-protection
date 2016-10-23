package com.geeksonsecurity.android.overlayprotector.view;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.geeksonsecurity.android.overlayprotector.MainActivity;
import com.geeksonsecurity.android.overlayprotector.R;
import com.geeksonsecurity.android.overlayprotector.adapter.SuspectedAppsAdapter;
import com.geeksonsecurity.android.overlayprotector.database.DatabaseHelper;
import com.geeksonsecurity.android.overlayprotector.database.DatabaseUtils;
import com.geeksonsecurity.android.overlayprotector.domain.Settings;
import com.geeksonsecurity.android.overlayprotector.domain.SuspectedApp;
import com.google.gson.Gson;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class SuspectedAppsFragment extends android.support.v4.app.Fragment implements AbsListView.OnItemClickListener {

    private static final String TAG = SuspectedAppsFragment.class.getSimpleName();
    /**
     * The fragment's ListView/GridView.
     */
    private ListView mListView;
    private TextView _headerTitle;
    private TextView _footerTitle;
    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private SuspectedAppsAdapter mAdapter;
    private Gson gson = new Gson();

    public static SuspectedAppsFragment newInstance() {
        SuspectedAppsFragment fragment = new SuspectedAppsFragment();
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SuspectedAppsFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mAdapter = new SuspectedAppsAdapter(getActivity());

        try {
            Context ctx = getActivity().getApplicationContext();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
            String json = prefs.getString(Settings.KEY_SETTINGS, "");
            if (!(json != null ? json.isEmpty() : true)) {
                Settings settings = gson.fromJson(json, Settings.class);
                if (settings.getSuspectedAppUpdateTimestamp() > 0) {
                    Dao<SuspectedApp, Integer> suspectedAppDao = DatabaseHelper.getHelper(getActivity()).getSuspectedAppDao();
                    List<SuspectedApp> suspectedApps = suspectedAppDao.queryBuilder().orderBy("appName", true).query();
                    mAdapter.clear();
                    mAdapter.addAll(suspectedApps);

                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        refreshHeaderView(getView());
        refreshList();
    }

    private void refreshList() {
        Dao<SuspectedApp, Integer> suspectedAppDao = null;
        try {
            suspectedAppDao = DatabaseHelper.getHelper(getActivity()).getSuspectedAppDao();
            List<SuspectedApp> suspectedApps = suspectedAppDao.queryBuilder().orderBy("appName", true).query();
            mAdapter.clear();
            mAdapter.addAll(suspectedApps);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.suspected_apps_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.op_sa_menu_refresh:
                DatabaseUtils.fillSuspectedApps(getActivity());
                refreshList();
                break;
            default:
                break;
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_suspectedapps, container, false);
        // Set the adapter
        mListView = (ListView) view.findViewById(android.R.id.list);
        mListView.setEmptyView(view.findViewById(android.R.id.empty));
        mListView.setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        return view;
    }

    private void refreshHeaderView(View view) {
        if (view == null)
            return;

        Context ctx = getActivity().getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String json = prefs.getString(Settings.KEY_SETTINGS, "");
        Settings settings = !json.isEmpty() ? gson.fromJson(json, Settings.class) : new Settings();

        Date date = new Date(settings.getSuspectedAppUpdateTimestamp());
        DateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        ListView listview = (ListView) view.findViewById(android.R.id.list);

        if (_headerTitle == null) {
            _headerTitle = new TextView(getActivity());
            _headerTitle.setPadding(20, 10, 5, 10);
            listview.addHeaderView(_headerTitle);
        }

        if (_footerTitle == null) {
            _footerTitle = new TextView(getActivity());
            _footerTitle.setPadding(20, 10, 5, 10);
            listview.addFooterView(_footerTitle);
        }
        _footerTitle.setText(R.string.od_suspectedapps_explanation);
        _headerTitle.setText(String.format("Last update: %s", settings.getSuspectedAppUpdateTimestamp() == 0 ? "never" : formatter.format(date)));


    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //if (null != mListener) {
        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        //mListener.onFragmentInteraction(DummyContent.ITEMS.get(position).id);
        //}
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(2);
    }

}
