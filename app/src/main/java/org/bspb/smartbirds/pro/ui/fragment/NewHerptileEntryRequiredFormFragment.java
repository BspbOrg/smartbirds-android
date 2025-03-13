package org.bspb.smartbirds.pro.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.bspb.smartbirds.pro.R;
import org.bspb.smartbirds.pro.prefs.CommonPrefs;
import org.bspb.smartbirds.pro.prefs.HerptilePrefs;
import org.bspb.smartbirds.pro.ui.views.PositiveDecimalNumberFormInput;
import org.bspb.smartbirds.pro.ui.views.SingleChoiceFormInput;
import org.bspb.smartbirds.pro.ui.views.SwitchFormInput;

import java.util.HashMap;

/**
 * Created by dani on 04.01.18.
 */

public class NewHerptileEntryRequiredFormFragment extends BaseFormFragment {

    SingleChoiceFormInput habitat;
    PositiveDecimalNumberFormInput count;
    SwitchFormInput confidential;

    HerptilePrefs prefs;
    CommonPrefs commonPrefs;

    NewEntryPicturesFragment picturesFragment;

    public static NewHerptileEntryRequiredFormFragment newInstance(boolean isNewEntry, boolean readOnly) {
        NewHerptileEntryRequiredFormFragment fragment = new NewHerptileEntryRequiredFormFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_NEW_ENTRY, isNewEntry);
        args.putBoolean(ARG_READ_ONLY, readOnly);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void onBeforeCreate(@Nullable Bundle savedInstanceState) {
        super.onBeforeCreate(savedInstanceState);
        prefs = new HerptilePrefs(requireContext());
        commonPrefs = new CommonPrefs(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_monitoring_form_new_herptile_required_entry, container, false);
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isNewEntry()) {
            habitat.setText(prefs.getHerptileHabitat());
            confidential.setChecked(commonPrefs.getConfidentialRecord());
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
        // In some cases picturesFragment is still null. Try to find it by id
        if (picturesFragment == null) {
            picturesFragment = (NewEntryPicturesFragment) getChildFragmentManager().findFragmentById(R.id.pictures_fragment);
        }
        if (picturesFragment != null) {
            picturesFragment.doDeserialize(monitoringCode, data);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (picturesFragment == null) {
            picturesFragment = (NewEntryPicturesFragment) getChildFragmentManager().findFragmentById(R.id.pictures_fragment);
        }

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    protected void initViews() {
        super.initViews();

        habitat = requireView().findViewById(R.id.form_herp_habitat);
        count = requireView().findViewById(R.id.form_herp_count);
        confidential = requireView().findViewById(R.id.form_herptiles_confidential);
    }


    @Override
    public void onPause() {
        super.onPause();
        prefs.setHerptileHabitat(habitat.getText().toString());
        commonPrefs.setConfidentialRecord(confidential.isChecked());
    }

}
