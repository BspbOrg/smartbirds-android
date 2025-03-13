package org.bspb.smartbirds.pro.ui.fragment;

import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.bspb.smartbirds.pro.R;
import org.bspb.smartbirds.pro.backend.dto.Nomenclature;
import org.bspb.smartbirds.pro.events.EEventBus;
import org.bspb.smartbirds.pro.events.LocationChangedEvent;
import org.bspb.smartbirds.pro.prefs.BirdPrefs;
import org.bspb.smartbirds.pro.prefs.CommonPrefs;
import org.bspb.smartbirds.pro.ui.views.DecimalNumberFormInput;
import org.bspb.smartbirds.pro.ui.views.PositiveDecimalNumberFormInput;
import org.bspb.smartbirds.pro.ui.views.SingleChoiceFormInput;
import org.bspb.smartbirds.pro.ui.views.SwitchFormInput;

import java.util.HashMap;
import java.util.Locale;

/**
 * Created by groupsky on 26.01.17.
 */

public class NewBirdsEntryRequiredFormFragment extends BaseFormFragment {

    protected static final String ARG_LAT = "lat";
    protected static final String ARG_LON = "lon";

    protected double lat;
    protected double lon;

    SingleChoiceFormInput countUnits;
    SingleChoiceFormInput countType;
    PositiveDecimalNumberFormInput count;
    DecimalNumberFormInput countMin;
    DecimalNumberFormInput countMax;
    SwitchFormInput confidential;
    TextView warningConfidential;
    EditText distanceView;

    BirdPrefs prefs;

    CommonPrefs commonPrefs;

    EEventBus eventBus = EEventBus.getInstance();

    NewEntryPicturesFragment picturesFragment;

    public static NewBirdsEntryRequiredFormFragment newInstance(boolean isNewEntry, boolean readOnly, double lat, double lon) {
        NewBirdsEntryRequiredFormFragment fragment = new NewBirdsEntryRequiredFormFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_NEW_ENTRY, isNewEntry);
        args.putBoolean(ARG_READ_ONLY, readOnly);
        args.putDouble(ARG_LAT, lat);
        args.putDouble(ARG_LON, lon);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void readArgs() {
        super.readArgs();
        if (getArguments() != null) {
            lat = getArguments().getDouble(ARG_LAT, 0);
            lon = getArguments().getDouble(ARG_LON, 0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_monitoring_form_new_birds_required_entry, container, false);
        }
        return view;
    }

    @Override
    protected void onBeforeCreate(@Nullable Bundle savedInstanceState) {
        super.onBeforeCreate(savedInstanceState);
        prefs = new BirdPrefs(requireContext());
        commonPrefs = new CommonPrefs(requireContext());
    }

    @Override
    protected void initViews() {
        super.initViews();

        if (getView() == null) return;

        countUnits = getView().findViewById(R.id.form_birds_count_units);
        countType = getView().findViewById(R.id.form_birds_count_type);
        count = getView().findViewById(R.id.form_birds_count);
        countMin = getView().findViewById(R.id.form_birds_count_min);
        countMax = getView().findViewById(R.id.form_birds_count_max);
        confidential = getView().findViewById(R.id.form_birds_confidential);
        warningConfidential = getView().findViewById(R.id.warning_confidential_nest);
        distanceView = getView().findViewById(R.id.form_birds_distance);

        if (confidential != null) {
            confidential.setOnCheckedChangeListener((buttonView, isChecked) -> showConfidentialWarningIfNeeded());
        }

        if (countUnits != null) {
            countUnits.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    showConfidentialWarningIfNeeded();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        if (countType != null) {
            countType.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    handleCountsLogic();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (lat != 0 && lon != 0 && isNewEntry()) {
            eventBus.registerSticky(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        eventBus.unregister(this);
    }

    public void onEvent(LocationChangedEvent e) {
        if (e == null || e.location == null) {
            return;
        }

        eventBus.unregister(this);

        Location entryLocation = new Location("entry");
        entryLocation.setLatitude(lat);
        entryLocation.setLongitude(lon);

        double distance = e.location.distanceTo(entryLocation);

        if (distance < 10) {
            return;
        }

        distanceView.setVisibility(View.VISIBLE);
        distanceView.setText(getString(R.string.form_birds_distance_value, distance));

    }

    @Override
    public void onResume() {
        super.onResume();
        if (isNewEntry()) {
            countUnits.setSelection(prefs.getCountUnits());
            countType.setSelection(prefs.getCountType());
            confidential.setChecked(commonPrefs.getConfidentialRecord());
        }
        handleCountsLogic();
        showConfidentialWarningIfNeeded();
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
    public void onPause() {
        super.onPause();
        prefs.setCountUnits(countUnits.getSelection());
        prefs.setCountType(countType.getSelection());
        commonPrefs.setConfidentialRecord(confidential.isChecked());
    }

    void showConfidentialWarningIfNeeded() {
        Nomenclature item = countUnits.getSelectedItem();
        String countUnit = item != null ? item.label.get("en") : null;
        if ("Nests".equals(countUnit)) {
            if (!confidential.isChecked()) {
                warningConfidential.setVisibility(View.VISIBLE);
            } else {
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    warningConfidential.setVisibility(View.INVISIBLE);
                } else {
                    warningConfidential.setVisibility(View.GONE);
                }
            }
        } else {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                warningConfidential.setVisibility(View.INVISIBLE);
            } else {
                warningConfidential.setVisibility(View.GONE);
            }
        }
    }

    void handleCountsLogic() {
        if (readOnly) {
            count.setEnabled(false);
            countMin.setEnabled(false);
            countMax.setEnabled(false);
            return;
        }
        Nomenclature item = countType.getSelectedItem();
        String countsType = item != null ? item.label.get("en") : null;
        switch (countsType != null ? countsType.toLowerCase(Locale.ENGLISH) : "") {
            case "exact number": // Exact count
                count.setEnabled(true);
                countMin.setEnabled(false);
                countMax.setEnabled(false);
                break;
            case "min.": // Min count
                count.setEnabled(false);
                countMin.setEnabled(true);
                countMax.setEnabled(false);
                break;
            case "max.": // Max count
                count.setEnabled(false);
                countMin.setEnabled(false);
                countMax.setEnabled(true);
                break;
            case "range": // Range count
                count.setEnabled(false);
                countMin.setEnabled(true);
                countMax.setEnabled(true);
                break;
            case "unspecified number": // Unspecified
                count.setEnabled(false);
                countMin.setEnabled(false);
                countMax.setEnabled(false);
                break;
            default:
                count.setEnabled(true);
                countMin.setEnabled(true);
                countMax.setEnabled(true);
                break;
        }
    }
}
