package org.bspb.smartbirds.pro.ui.partial;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Checkable;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.bspb.smartbirds.pro.R;
import org.bspb.smartbirds.pro.SmartBirdsApplication;
import org.bspb.smartbirds.pro.content.MonitoringEntry;
import org.bspb.smartbirds.pro.enums.EntryType;
import org.bspb.smartbirds.pro.tools.Reporting;
import org.bspb.smartbirds.pro.ui.utils.FormsConfig;

import java.util.Locale;

/**
 * Created by groupsky on 08.03.17.
 */

public class MonitoringEntryListRowPartialView extends LinearLayout implements Checkable {

    private static final String TAG = SmartBirdsApplication.TAG + ".MonEnLRPV";

    TextView typeView;
    TextView speciesView;
    TextView countView;
    TextView statusView;
    View moderatorReview;

    private boolean isChecked;

    private MonitoringEntry entry;
    private boolean alreadyInflated = false;

    public static MonitoringEntryListRowPartialView build(Context context) {
        MonitoringEntryListRowPartialView instance = new MonitoringEntryListRowPartialView(context);
        instance.onFinishInflate();
        return instance;
    }

    public MonitoringEntryListRowPartialView(Context context) {
        this(context, null);
    }

    public MonitoringEntryListRowPartialView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MonitoringEntryListRowPartialView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        if (!alreadyInflated) {
            alreadyInflated = true;
            inflate(getContext(), R.layout.partial_monitoring_entry_list_row, this);
            bind();
        }
        super.onFinishInflate();
    }

    protected void bind() {
        Log.d(TAG, "bind after views");
        typeView = findViewById(R.id.type);
        speciesView = findViewById(R.id.species);
        countView = findViewById(R.id.count);
        statusView = findViewById(R.id.status);
        moderatorReview = findViewById(R.id.moderator_review);

        if (entry != null) bind(entry);
    }

    public void bind(MonitoringEntry entry) {
        Log.d(TAG, "bind");
        this.entry = entry;
        if (entry == null) return;
        if (speciesView == null) return;
        Context context = getContext();

        if ("1".equals(entry.data.get(context.getString(R.string.tag_moderator_review)))) {
            moderatorReview.setVisibility(VISIBLE);
        } else {
            moderatorReview.setVisibility(GONE);
        }


        switch (entry.type) {
            case BIRDS:
            case HUMID:
                typeView.setText(entry.type == EntryType.BIRDS ? R.string.entry_type_birds : R.string.entry_type_humid);
                speciesView.setText(entry.data.get(context.getString(R.string.tag_species_scientific_name)));
                String tagBirdsCountType = context.getString(R.string.tag_count_type);
                String countsType = entry.data.get(tagBirdsCountType + ".en");
                String count;
                switch (countsType != null ? countsType.toLowerCase(Locale.ENGLISH) : "") {
                    case "exact number": // Exact count
                        count = entry.data.get(context.getString(R.string.tag_count));
                        break;
                    case "min.": // Min count
                        count = entry.data.get(context.getString(R.string.tag_min));
                        break;
                    case "max.": // Max count
                        count = entry.data.get(context.getString(R.string.tag_max));
                        break;
                    case "range": // Range count
                        count = entry.data.get(context.getString(R.string.tag_min)) + " - " + entry.data.get(context.getString(R.string.tag_max));
                        break;
                    case "unspecified number": // Unspecified
                    default:
                        count = "";
                        break;
                }
                countView.setText(entry.data.get(tagBirdsCountType) + " " + count + " " + entry.data.get(context.getString(R.string.tag_count_unit)).toLowerCase(Locale.getDefault()));
                break;
            case CBM:
                typeView.setText(R.string.entry_type_cbm);
                speciesView.setText(entry.data.get(context.getString(R.string.tag_observed_bird)));
                countView.setText(entry.data.get(context.getString(R.string.tag_count_subject)));
                break;
            case HERPTILE:
                typeView.setText(R.string.entry_type_herptile);
                speciesView.setText(entry.data.get(context.getString(R.string.tag_species_scientific_name)));
                countView.setText(entry.data.get(context.getString(R.string.tag_count)));
                break;
            case MAMMAL:
                typeView.setText(R.string.entry_type_mammal);
                speciesView.setText(entry.data.get(context.getString(R.string.tag_species_scientific_name)));
                countView.setText(entry.data.get(context.getString(R.string.tag_count)));
                break;
            case CICONIA:
                typeView.setText(R.string.entry_type_ciconia);
                speciesView.setText(entry.data.get(context.getString(R.string.tag_substrate_type)));
                countView.setText(entry.data.get(context.getString(R.string.tag_number_juveniles_in_nest)));
                break;
            case INVERTEBRATES:
                typeView.setText(R.string.entry_type_invertebrates);
                speciesView.setText(entry.data.get(context.getString(R.string.tag_species_scientific_name)));
                countView.setText(entry.data.get(context.getString(R.string.tag_count)));
                break;
            case PLANTS:
                typeView.setText(R.string.entry_type_plants);
                speciesView.setText(entry.data.get(context.getString(R.string.tag_species_scientific_name)));
                countView.setText(entry.data.get(context.getString(R.string.tag_count)));
                break;
            case THREATS:
                fillThreatTypeText();
                fillThreatSpeciesView();
                countView.setText(entry.data.get(context.getString(R.string.tag_count)));
                break;
            case PYLONS:
                typeView.setText(R.string.entry_type_pylons);
                speciesView.setText(entry.data.get(context.getString(R.string.tag_pylons_pylon_type)));
                countView.setText("");
                break;
            case PYLONS_CASUALTIES:
                typeView.setText(R.string.entry_type_pylons_casualties);
                speciesView.setText(entry.data.get(context.getString(R.string.tag_species_scientific_name)));
                countView.setText(entry.data.get(context.getString(R.string.tag_count)));
                break;
            case BIRDS_MIGRATIONS:
                typeView.setText(R.string.entry_type_birds_migrations);
                speciesView.setText(entry.data.get(context.getString(R.string.tag_species_scientific_name)));
                countView.setText(entry.data.get(context.getString(R.string.tag_count)));
                break;
            case FISH:
                typeView.setText(R.string.entry_type_fish);
                speciesView.setText(entry.data.get(context.getString(R.string.tag_species_scientific_name)));
                countView.setText(entry.data.get(context.getString(R.string.tag_count)));
                break;
            case BATS:
                typeView.setText(R.string.entry_type_bats);
                speciesView.setText(entry.data.get(context.getString(R.string.tag_species_scientific_name)));
                countView.setText(entry.data.get(context.getString(R.string.tag_count)));
                break;
            case BEARS:
                typeView.setText(R.string.entry_type_bears);
                speciesView.setText(entry.data.get(context.getString(R.string.tag_species_scientific_name)));
                countView.setText(entry.data.get(context.getString(R.string.tag_count)));
                break;

        }
    }

    private void fillThreatTypeText() {
        FormsConfig.ThreatsPrimaryType primaryType = null;
        try {
            primaryType = FormsConfig.ThreatsPrimaryType.valueOf(entry.data.get(getContext().getString(R.string.tag_primary_type)));
        } catch (IllegalArgumentException e) {
            Reporting.logException(e);
        }

        if (primaryType != null) {
            typeView.setText(primaryType.getLabelId());
        } else {
            typeView.setText(entry.data.get(getContext().getString(R.string.tag_primary_type)));
        }
    }

    private void fillThreatSpeciesView() {
        try {
            FormsConfig.ThreatsPrimaryType primaryType = FormsConfig.ThreatsPrimaryType.valueOf(entry.data.get(getContext().getString(R.string.tag_primary_type)));
            if (FormsConfig.ThreatsPrimaryType.threat.equals(primaryType)) {
                speciesView.setText(entry.data.get(getContext().getString(R.string.tag_category)));
            } else {
                FormsConfig.ThreatsPoisonedType poisonedType = FormsConfig.ThreatsPoisonedType.valueOf(entry.data.get(getContext().getString(R.string.tag_poisoned_type)));
                speciesView.setText(poisonedType.getLabelId());
            }
        } catch (IllegalArgumentException e) {
            Reporting.logException(e);
        }
    }

    @Override
    public void setChecked(boolean checked) {
        Log.d(TAG, String.format(Locale.ENGLISH, "setChecked: %s", checked));
        isChecked = checked;
        if (isChecked) {
            setBackgroundResource(R.color.backgroundSelected);
        } else {
            setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override
    public boolean isChecked() {
        return isChecked;
    }

    @Override
    public void toggle() {
        setChecked(!isChecked);
    }
}
