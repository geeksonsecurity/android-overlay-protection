package com.geeksonsecurity.android.overlayprotector.wizard;

import android.support.v4.app.Fragment;
import android.text.TextUtils;

import com.tech.freak.wizardpager.model.ModelCallbacks;
import com.tech.freak.wizardpager.model.Page;
import com.tech.freak.wizardpager.model.ReviewItem;

import java.util.ArrayList;

public class WizardInfoPage extends Page {

    public WizardInfoPage(ModelCallbacks callbacks, String title) {
        super(callbacks, title);
    }

    @Override
    public Fragment createFragment() {
        return WizardInfoPageFragment.create(getKey());
    }

    @Override
    public void getReviewItems(ArrayList<ReviewItem> dest) {
    }

    @Override
    public boolean isCompleted() {
        return !TextUtils.isEmpty(mData.getString(SIMPLE_DATA_KEY));
    }

    public WizardInfoPage setValue(int imageDrawableId, String descriptionText) {
        mData.putInt(WizardInfoPageFragment.IMAGE_KEY, imageDrawableId);
        mData.putString(WizardInfoPageFragment.DESCRIPTION_KEY, descriptionText);
        return this;
    }
}
