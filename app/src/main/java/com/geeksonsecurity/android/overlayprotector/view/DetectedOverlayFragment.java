package com.geeksonsecurity.android.overlayprotector.view;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;

import com.geeksonsecurity.android.overlayprotector.MainActivity;
import com.geeksonsecurity.android.overlayprotector.R;
import com.geeksonsecurity.android.overlayprotector.adapter.DetectedOverlayAdapter;
import com.geeksonsecurity.android.overlayprotector.database.DatabaseHelper;
import com.geeksonsecurity.android.overlayprotector.domain.DetectedOverlay;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DetectedOverlayFragment extends Fragment implements AbsListView.OnItemClickListener {
    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private DetectedOverlayAdapter mAdapter;

    public static DetectedOverlayFragment newInstance() {
        return new DetectedOverlayFragment();
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public DetectedOverlayFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        List<DetectedOverlay> listData = new ArrayList<>();
        mAdapter = new DetectedOverlayAdapter(getActivity(), listData);
        refreshItems();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.detected_overlays_menu, menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.op_od_menu_refresh:
                refreshItems();
                break;
            case R.id.op_od_menu_clear:
                clearHistory();
                break;
            default:
                break;
        }
        return false;
    }

    private void clearHistory() {
        try {
            Dao<DetectedOverlay, Integer> detectedOverlayDao = DatabaseHelper.getHelper(getActivity()).getDetectedOverlayDao();
            DeleteBuilder<DetectedOverlay, Integer> deleteBuilder = detectedOverlayDao.deleteBuilder();
            deleteBuilder.where().gt("id", 0);
            int deleted = deleteBuilder.delete();
            Log.i(getTag(), "Deleted " + deleted + " entries from DetectedOverlay");
        } catch (SQLException e) {
            Log.e(getTag(), "Failed to deleted entries", e);
        }
        mAdapter.clear();
    }

    private void refreshItems() {
        List<DetectedOverlay> listData = new ArrayList<>();
        try {
            listData = DatabaseHelper.getHelper(getActivity()).getDetectedOverlayDao().queryBuilder().orderBy("timestamp", false).query();
        } catch (SQLException e) {
            Log.e(getTag(), "Failed to obtain detected overlays objects", e);
        }
        mAdapter.clear();
        mAdapter.addAll(listData);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detectedoverlay, container, false);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        mListView.setEmptyView(view.findViewById(android.R.id.empty));
        mListView.setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        return view;
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
        ((MainActivity) activity).onSectionAttached(3);
    }
}
