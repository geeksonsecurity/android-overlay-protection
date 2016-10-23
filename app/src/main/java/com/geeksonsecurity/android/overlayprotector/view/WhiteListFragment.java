package com.geeksonsecurity.android.overlayprotector.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.geeksonsecurity.android.overlayprotector.MainActivity;
import com.geeksonsecurity.android.overlayprotector.MonitorAccessibilityService;
import com.geeksonsecurity.android.overlayprotector.R;
import com.geeksonsecurity.android.overlayprotector.adapter.WhiteEntryAdapter;
import com.geeksonsecurity.android.overlayprotector.database.DatabaseHelper;
import com.geeksonsecurity.android.overlayprotector.domain.ServiceCommunication;
import com.geeksonsecurity.android.overlayprotector.domain.WhiteEntry;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WhiteListFragment extends Fragment implements AbsListView.OnItemClickListener {

    private static final String TAG = WhiteListFragment.class.getSimpleName();
    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private WhiteEntryAdapter mAdapter;

    public static WhiteListFragment newInstance() {
        WhiteListFragment fragment = new WhiteListFragment();
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public WhiteListFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        refreshWhiteEntries();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.whitelist_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.op_wl_menu_refresh:
                refreshWhiteEntries();
                break;
            case R.id.op_wl_menu_add:
                final Dialog addDialog = new Dialog(getActivity());
                addDialog.setContentView(R.layout.add_whitelist_dialog);
                addDialog.setTitle("New white entry");

                Button cancel = (Button) addDialog.findViewById(R.id.cancelButton);
                cancel.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        addDialog.dismiss();
                    }
                });

                Button ok = (Button) addDialog.findViewById(R.id.okButton);
                ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String packageName = ((EditText) addDialog.findViewById(R.id.packageEditText)).getText().toString();
                        boolean exactlyMatch = !((CheckBox) addDialog.findViewById(R.id.wildcardCheckBox)).isChecked();

                        if (packageName.length() > 0) {
                            WhiteEntry we = new WhiteEntry(packageName, System.currentTimeMillis(), 0, false, exactlyMatch);
                            try {
                                DatabaseHelper.getHelper(getActivity()).getWhiteListDao().create(we);
                                mAdapter.add(we);
                                addDialog.dismiss();
                                notifyChangesToService();
                            } catch (SQLException e) {
                                Log.e(TAG, "Failed to add new white entry", e);
                                Toast.makeText(getActivity(), "Failed to add new white entry!", Toast.LENGTH_SHORT);
                            }
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            // Set the dialog title
                            builder.setTitle("Error");
                            builder.setMessage("The package field is empty!");
                            builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    // Nothing
                                }
                            });
                            builder.create().show();
                        }
                    }
                });
                addDialog.show();
                break;
            default:
                break;
        }
        return false;
    }

    private void notifyChangesToService() {
        Intent intent = new Intent(getActivity(), MonitorAccessibilityService.class);
        intent.setAction(ServiceCommunication.MSG_WHITELIST_UPDATED);
        getActivity().startService(intent);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_whitelist, container, false);

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
        ((MainActivity) activity).onSectionAttached(4);
    }

    public void refreshWhiteEntries() {
        List<WhiteEntry> listData = new ArrayList<>();
        try {
            listData = DatabaseHelper.getHelper(getActivity()).getWhiteListDao().queryBuilder().where().eq("systemEntry", Boolean.FALSE).query();
        } catch (SQLException e) {
            Log.e(getTag(), "Failed to obtain detected user-whitelist objects", e);
        }

        if (mAdapter == null) {
            mAdapter = new WhiteEntryAdapter(getActivity(), listData);
        } else {
            mAdapter.clear();
            mAdapter.addAll(listData);
        }
    }
}
