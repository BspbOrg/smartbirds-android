package org.bspb.smartbirds.pro.ui.fragment;

import android.os.Build;
import android.util.Log;
import android.view.View;

import com.google.android.material.textfield.TextInputLayout;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.bspb.smartbirds.pro.R;
import org.bspb.smartbirds.pro.prefs.CommonPrefs_;
import org.bspb.smartbirds.pro.ui.utils.FormsConfig;
import org.bspb.smartbirds.pro.ui.views.DecimalNumberFormInput;
import org.bspb.smartbirds.pro.ui.views.SingleChoiceConfigFormInput;
import org.bspb.smartbirds.pro.ui.views.SingleChoiceFormInput;
import org.bspb.smartbirds.pro.ui.views.SingleChoiceRadioFormInput;
import org.bspb.smartbirds.pro.ui.views.SwitchFormInput;

import java.util.HashMap;

@EFragment(R.layout.fragment_monitoring_form_new_threats_required_entry)
public class NewThreatsEntryRequiredFormFragment extends BaseFormFragment {

    interface OnPrimaryTypeChangedListener {
        void onPrimaryTypeChange(String primaryType);
    }

    @ViewById(R.id.form_threats_confidential)
    SwitchFormInput confidential;

    @ViewById(R.id.form_threats_primary_type)
    SingleChoiceRadioFormInput primaryType;

    @ViewById(R.id.form_threats_category)
    SingleChoiceFormInput category;

    @ViewById(R.id.form_threats_class)
    SingleChoiceConfigFormInput classInput;

    @ViewById(R.id.form_threats_species)
    SingleChoiceFormInput species;

    @ViewById(R.id.form_threats_count)
    DecimalNumberFormInput count;

    @ViewById(R.id.form_threats_estimate)
    SingleChoiceFormInput estimate;

    @ViewById(R.id.form_threats_poisoned_type)
    SingleChoiceRadioFormInput poisonedType;

    @ViewById(R.id.form_threats_state_carcass)
    SingleChoiceFormInput stateCarcass;

    @ViewById(R.id.form_threats_category_hint)
    TextInputLayout categoryHint;

    @ViewById(R.id.form_threats_estimate_hint)
    TextInputLayout estimateHint;

    @ViewById(R.id.form_threats_class_hint)
    TextInputLayout classInputHint;

    @ViewById(R.id.form_threats_species_hint)
    TextInputLayout speciesHint;

    @ViewById(R.id.form_threats_count_hint)
    TextInputLayout countHint;

    @ViewById(R.id.form_threats_state_carcass_hint)
    TextInputLayout stateCarcassHint;

    @FragmentById(R.id.pictures_fragment)
    NewEntryPicturesFragment picturesFragment;

    @Pref
    CommonPrefs_ commonPrefs;

    private OnPrimaryTypeChangedListener primaryTypeChangedListener;

    @Override
    public void onResume() {
        super.onResume();
        if (isNewEntry()) {
            confidential.setChecked(commonPrefs.confidentialRecord().get());
        }
        handleInitialState();
    }

    @Override
    public void onPause() {
        super.onPause();
        commonPrefs.confidentialRecord().put(confidential.isChecked());
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
        if (picturesFragment != null) {
            picturesFragment.doDeserialize(monitoringCode, data);
        }
    }

    @AfterViews
    protected void flushDeserialize() {
        initViews();
        if (picturesFragment == null) {
            picturesFragment = (NewEntryPicturesFragment) getChildFragmentManager().findFragmentById(R.id.pictures_fragment);
        }
    }

    private void initViews() {
        primaryType.setOnValueChangeListener(new SingleChoiceRadioFormInput.OnValueChangeListener() {
            @Override
            public void onValueChanged(String value) {
                handlePrimaryType(value);
            }
        });

        poisonedType.setOnValueChangeListener(new SingleChoiceRadioFormInput.OnValueChangeListener() {
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
