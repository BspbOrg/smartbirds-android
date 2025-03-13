package org.bspb.smartbirds.pro.ui.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout;

import org.bspb.smartbirds.pro.R;
import org.bspb.smartbirds.pro.prefs.CommonPrefs;
import org.bspb.smartbirds.pro.ui.utils.FormsConfig;
import org.bspb.smartbirds.pro.ui.views.PositiveDecimalNumberFormInput;
import org.bspb.smartbirds.pro.ui.views.SingleChoiceConfigFormInput;
import org.bspb.smartbirds.pro.ui.views.SingleChoiceConfigRadioFormInput;
import org.bspb.smartbirds.pro.ui.views.SingleChoiceFormInput;
import org.bspb.smartbirds.pro.ui.views.SwitchFormInput;

import java.util.HashMap;

public class NewThreatsEntryRequiredFormFragment extends BaseFormFragment {

    interface OnPrimaryTypeChangedListener {
        void onPrimaryTypeChange(String primaryType);
    }

    public static NewThreatsEntryRequiredFormFragment newInstance(boolean isNewEntry, boolean readOnly) {
        NewThreatsEntryRequiredFormFragment fragment = new NewThreatsEntryRequiredFormFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_NEW_ENTRY, isNewEntry);
        args.putBoolean(ARG_READ_ONLY, readOnly);
        fragment.setArguments(args);
        return fragment;
    }

    SwitchFormInput confidential;
    SingleChoiceConfigRadioFormInput primaryType;
    SingleChoiceFormInput category;
    SingleChoiceConfigFormInput classInput;
    SingleChoiceFormInput species;
    PositiveDecimalNumberFormInput count;
    SingleChoiceFormInput estimate;
    SingleChoiceConfigRadioFormInput poisonedType;
    SingleChoiceFormInput stateCarcass;
    TextInputLayout categoryHint;
    TextInputLayout estimateHint;
    TextInputLayout classInputHint;
    TextInputLayout speciesHint;
    TextInputLayout countHint;
    TextInputLayout stateCarcassHint;

    NewEntryPicturesFragment picturesFragment;

    CommonPrefs commonPrefs;

    private OnPrimaryTypeChangedListener primaryTypeChangedListener;

    @Override
    protected void onBeforeCreate(@Nullable Bundle savedInstanceState) {
        super.onBeforeCreate(savedInstanceState);
        commonPrefs = new CommonPrefs(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_monitoring_form_new_threats_required_entry, container, false);
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isNewEntry()) {
            confidential.setChecked(commonPrefs.getConfidentialRecord());
        }
        handleInitialState();
    }

    @Override
    public void onPause() {
        super.onPause();
        commonPrefs.setConfidentialRecord(confidential.isChecked());
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

        confidential = requireView().findViewById(R.id.form_threats_confidential);
        primaryType = requireView().findViewById(R.id.form_threats_primary_type);
        category = requireView().findViewById(R.id.form_threats_category);
        classInput = requireView().findViewById(R.id.form_threats_class);
        species = requireView().findViewById(R.id.form_threats_species);
        count = requireView().findViewById(R.id.form_threats_count);
        estimate = requireView().findViewById(R.id.form_threats_estimate);
        poisonedType = requireView().findViewById(R.id.form_threats_poisoned_type);
        stateCarcass = requireView().findViewById(R.id.form_threats_state_carcass);
        categoryHint = requireView().findViewById(R.id.form_threats_category_hint);
        estimateHint = requireView().findViewById(R.id.form_threats_estimate_hint);
        classInputHint = requireView().findViewById(R.id.form_threats_class_hint);
        speciesHint = requireView().findViewById(R.id.form_threats_species_hint);
        countHint = requireView().findViewById(R.id.form_threats_count_hint);
        stateCarcassHint = requireView().findViewById(R.id.form_threats_state_carcass_hint);

        primaryType.setOnValueChangeListener(new SingleChoiceConfigRadioFormInput.OnValueChangeListener() {
            @Override
            public void onValueChanged(String value) {
                handlePrimaryType(value);
            }
        });

        poisonedType.setOnValueChangeListener(new SingleChoiceConfigRadioFormInput.OnValueChangeListener() {
            @Override
            public void onValueChanged(String value) {
                handlePoisonedType(value);
            }
        });
        classInput.setOnSelectionChangeListener(new SingleChoiceConfigFormInput.OnSelectionChangeListener() {
            @Override
            public void onSelectionChange(SingleChoiceConfigFormInput view) {
                handleClassInput(view.getSelectedItem());
            }
        });

        if (primaryTypeChangedListener != null) {
            primaryTypeChangedListener.onPrimaryTypeChange(primaryType.getSelectedItem());
        }
    }

    private void handlePoisonedType(String type) {
        hideAllFields();
        poisonedType.setVisibility(View.VISIBLE);

        if (type == null) {
            return;
        }

        FormsConfig.ThreatsPoisonedType poisonedType = FormsConfig.ThreatsPoisonedType.valueOf(type);
        switch (poisonedType) {
            case dead:
                showDeadForm();
                break;
            case alive:
                showAliveForm();
                break;
            case bait:
                showBaitForm();
                break;
        }
    }

    private void handlePrimaryType(String primaryTypeValue) {
        hideAllFields();
        if (primaryTypeValue == null) {
            return;
        }

        FormsConfig.ThreatsPrimaryType primaryType = FormsConfig.ThreatsPrimaryType.valueOf(primaryTypeValue);
        switch (primaryType) {
            case threat:
                showThreatForm();
                break;
            case poison:
                hideAllFields();
                poisonedType.setVisibility(View.VISIBLE);
                poisonedType.setRequired(true);
                break;
        }

        Log.d("++++++", "Primary type changed.....");
        if (primaryTypeChangedListener != null) {
            Log.d("++++++", ".....");
            primaryTypeChangedListener.onPrimaryTypeChange(primaryTypeValue);
        }
    }

    private void handleClassInput(String value) {
        species.setSelection((String) null);
        if (value == null) {
            return;
        }

        FormsConfig.SpeciesClass speciesClass = FormsConfig.SpeciesClass.valueOf(value);
        switch (speciesClass) {
            case birds:
                species.setKey("species_birds");
                break;
            case herptiles:
                species.setKey("species_herptiles");
                break;
            case mammals:
                species.setKey("species_mammals");
                break;
            case invertebrates:
                species.setKey("species_invertebrates");
                break;
            case plants:
                species.setKey("species_plants");
                break;
            case fishes:
                species.setKey("species_fishes");
                break;
            default:
                species.setKey(null);
                break;
        }
    }

    private void handleInitialState() {
        String primaryTypeValue = primaryType.getSelectedItem();

        if (primaryTypeValue == null) {
            hideAllFields();
            return;
        }

        if (FormsConfig.ThreatsPrimaryType.threat.isSame(primaryTypeValue)) {
            showThreatForm();
        } else {
            String poisonedTypeValue = poisonedType.getSelectedItem();
            if (poisonedTypeValue == null) {
                hideAllFields();
                poisonedType.setVisibility(View.VISIBLE);
                return;
            }
            FormsConfig.ThreatsPoisonedType poisonedType = FormsConfig.ThreatsPoisonedType.valueOf(poisonedTypeValue);
            switch (poisonedType) {
                case dead:
                    showDeadForm();
                    break;
                case alive:
                    showAliveForm();
                    break;
                case bait:
                    showBaitForm();
                    break;
            }
        }

        if (primaryTypeChangedListener != null) {
            primaryTypeChangedListener.onPrimaryTypeChange(primaryTypeValue);
        }
    }

    private void hideAllFields() {
        poisonedType.setVisibility(View.GONE);
        category.setVisibility(View.GONE);
        estimate.setVisibility(View.GONE);
        classInput.setVisibility(View.GONE);
        species.setVisibility(View.GONE);
        count.setVisibility(View.GONE);
        stateCarcass.setVisibility(View.GONE);

        categoryHint.setVisibility(View.GONE);
        estimateHint.setVisibility(View.GONE);
        classInputHint.setVisibility(View.GONE);
        speciesHint.setVisibility(View.GONE);
        countHint.setVisibility(View.GONE);
        stateCarcassHint.setVisibility(View.GONE);

        poisonedType.setRequired(false);
        category.setRequired(false);
        estimate.setRequired(false);
        classInput.setRequired(false);
        species.setRequired(false);
        count.setRequired(false);
        stateCarcass.setRequired(false);
    }

    private void showThreatForm() {
        hideAllFields();
        category.setVisibility(View.VISIBLE);
        classInput.setVisibility(View.VISIBLE);
        species.setVisibility(View.VISIBLE);
        count.setVisibility(View.VISIBLE);
        estimate.setVisibility(View.VISIBLE);

        categoryHint.setVisibility(View.VISIBLE);
        classInputHint.setVisibility(View.VISIBLE);
        speciesHint.setVisibility(View.VISIBLE);
        countHint.setVisibility(View.VISIBLE);
        estimateHint.setVisibility(View.VISIBLE);

        category.setRequired(true);
    }

    private void showDeadForm() {
        hideAllFields();
        poisonedType.setVisibility(View.VISIBLE);
        classInput.setVisibility(View.VISIBLE);
        species.setVisibility(View.VISIBLE);
        count.setVisibility(View.VISIBLE);
        stateCarcass.setVisibility(View.VISIBLE);

        classInputHint.setVisibility(View.VISIBLE);
        speciesHint.setVisibility(View.VISIBLE);
        countHint.setVisibility(View.VISIBLE);
        stateCarcassHint.setVisibility(View.VISIBLE);

        classInput.setRequired(true);
        species.setRequired(true);
        count.setRequired(true);
        stateCarcass.setRequired(true);
    }

    private void showAliveForm() {
        hideAllFields();
        poisonedType.setVisibility(View.VISIBLE);
        classInput.setVisibility(View.VISIBLE);
        species.setVisibility(View.VISIBLE);
        count.setVisibility(View.VISIBLE);

        classInputHint.setVisibility(View.VISIBLE);
        speciesHint.setVisibility(View.VISIBLE);
        countHint.setVisibility(View.VISIBLE);

        classInput.setRequired(true);
        species.setRequired(true);
        count.setRequired(true);
    }

    private void showBaitForm() {
        hideAllFields();
        poisonedType.setVisibility(View.VISIBLE);
        count.setVisibility(View.VISIBLE);
        countHint.setVisibility(View.VISIBLE);

        count.setRequired(true);
    }

    public void setOnPrimaryTypeChangedListener(OnPrimaryTypeChangedListener primaryTypeChangedListener) {
        this.primaryTypeChangedListener = primaryTypeChangedListener;
    }

}
