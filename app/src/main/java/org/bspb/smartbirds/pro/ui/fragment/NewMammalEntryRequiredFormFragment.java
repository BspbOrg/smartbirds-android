package org.bspb.smartbirds.pro.ui.fragment;

import android.os.Build;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.bspb.smartbirds.pro.R;
import org.bspb.smartbirds.pro.prefs.CommonPrefs_;
import org.bspb.smartbirds.pro.prefs.MammalPrefs_;
import org.bspb.smartbirds.pro.ui.views.DecimalNumberFormInput;
import org.bspb.smartbirds.pro.ui.views.SingleChoiceFormInput;
import org.bspb.smartbirds.pro.ui.views.SwitchFormInput;

import java.util.HashMap;

/**
 * Created by dani on 04.01.18.
 */

@EFragment(R.layout.fragment_monitoring_form_new_mammal_required_entry)
public class NewMammalEntryRequiredFormFragment extends BaseFormFragment {

    @ViewById(R.id.form_herp_habitat)
    SingleChoiceFormInput habitat;

    @ViewById(R.id.form_herp_count)
    DecimalNumberFormInput count;

    @ViewById(R.id.form_mammals_confidential)
    SwitchFormInput confidential;

    @Pref
    MammalPrefs_ prefs;


    @Pref
    CommonPrefs_ commonPrefs;

    @FragmentById(R.id.pictures_fragment)
    NewEntryPicturesFragment picturesFragment;
    private HashMap<String, String> pendingDeserialize;

    @Override
    public void onResume() {
        super.onResume();
        if (isNewEntry()) {
            habitat.setText(prefs.mammalHabitat().get());
            confidential.setChecked(commonPrefs.confidentialRecord().get());
        }
    }

    @Override
    protected HashMap<String, String> serialize() {
        HashMap<String, String> data = super.serialize();
        data.putAll(picturesFragment.serialize());
        return data;
    }

    @Override
    protected void deserialize(HashMap<String, String> data) {
        super.deserialize(data);
        if (picturesFragment == null) {
            pendingDeserialize = data;
        } else {
            picturesFragment.doDeserialize(monitoringCode, data);
        }
    }

    @AfterViews
    protected void flushDeserialize() {
        if (picturesFragment == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            picturesFragment = (NewEntryPicturesFragment) getChildFragmentManager().findFragmentById(R.id.pictures_fragment);
        }
        if (picturesFragment == null) {
            picturesFragment = (NewEntryPicturesFragment) getFragmentManager().findFragmentById(R.id.pictures_fragment);
        }
        if (pendingDeserialize != null) {
            picturesFragment.doDeserialize(monitoringCode, pendingDeserialize);
            pendingDeserialize = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        prefs.mammalHabitat().put(habitat.getText().toString());
        commonPrefs.confidentialRecord().put(confidential.isChecked());
    }

}
