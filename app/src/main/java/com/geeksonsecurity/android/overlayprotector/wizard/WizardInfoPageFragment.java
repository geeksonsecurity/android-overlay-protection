package com.geeksonsecurity.android.overlayprotector.wizard;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.geeksonsecurity.android.overlayprotector.R;
import com.tech.freak.wizardpager.model.Page;
import com.tech.freak.wizardpager.ui.PageFragmentCallbacks;

public class WizardInfoPageFragment extends Fragment {
    public static final String DESCRIPTION_KEY = "descriptionKey";
    public static final String IMAGE_KEY = "imageKey";
    private static final java.lang.String ARG_KEY = "key";

    private PageFragmentCallbacks mCallbacks;
    private String mKey;
    private Page mPage;
    private ImageView imageView;

    private Uri mNewImageUri;

    public static WizardInfoPageFragment create(String key) {
        Bundle args = new Bundle();
        args.putString(ARG_KEY, key);
        WizardInfoPageFragment f = new WizardInfoPageFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mKey = args.getString(ARG_KEY);
        mPage = mCallbacks.onGetPage(mKey);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.wizard_info_page,
                container, false);

        ((TextView) rootView.findViewById(android.R.id.title)).setText(mPage.getTitle());

        imageView = (ImageView) rootView.findViewById(R.id.imageView);

        String descriptionData = mPage.getData().getString(DESCRIPTION_KEY);
        ((TextView) rootView.findViewById(R.id.wizardDescriptionText)).setText(descriptionData);

        int res = mPage.getData().getInt(IMAGE_KEY);
        if (res > 0) {
            imageView.setImageDrawable(ResourcesCompat.getDrawable(getResources(), res, null));
        } else {
            imageView.setVisibility(View.INVISIBLE);
        }
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!(activity instanceof PageFragmentCallbacks)) {
            throw new ClassCastException(
                    "Activity must implement PageFragmentCallbacks");
        }

        mCallbacks = (PageFragmentCallbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

}