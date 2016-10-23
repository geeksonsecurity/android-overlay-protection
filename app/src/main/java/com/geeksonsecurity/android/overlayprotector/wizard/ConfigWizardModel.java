package com.geeksonsecurity.android.overlayprotector.wizard;

import android.content.Context;

import com.geeksonsecurity.android.overlayprotector.R;
import com.tech.freak.wizardpager.model.AbstractWizardModel;
import com.tech.freak.wizardpager.model.PageList;

public class ConfigWizardModel extends AbstractWizardModel {

    public ConfigWizardModel(Context context) {
        super(context);
    }

    @Override
    protected PageList onNewRootPageList() {
        return new PageList(new WizardInfoPage(this, mContext.getString(R.string.op_wizard_step1_title))
                .setValue(0, mContext.getString(R.string.op_wizard_step1_content)),
                new WizardInfoPage(this, mContext.getString(R.string.op_wizard_step2_title)).setValue(R.drawable.configstep1, mContext.getString(R.string.op_wizard_step2_content)),
                new WizardInfoPage(this, mContext.getString(R.string.op_wizard_step3_title)).setValue(R.drawable.configstep2, mContext.getString(R.string.op_wizard_step3_content)),
                new WizardInfoPage(this, mContext.getString(R.string.op_wizard_step4_title)).setValue(R.drawable.configstep3, mContext.getString(R.string.op_wizard_step4_content)));

    }
}
