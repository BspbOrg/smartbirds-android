package org.bspb.smartbirds.pro.backend.dto;

import android.text.TextUtils;

import org.bspb.smartbirds.pro.ui.utils.Configuration;

import java.util.Locale;

public class SpeciesLabel extends Label {

    public SpeciesLabel(Label label) {
        this.values = label.values;
    }

    public String get(String locale) {
        String localeLabel = super.get(locale);
        String laLabel = values.get("la");
        return !TextUtils.isEmpty(laLabel) ? laLabel + Configuration.MULTIPLE_CHOICE_DELIMITER + localeLabel : localeLabel;
    }

    @Override
    public String getLabelId() {
        return values.containsKey("la") ? values.get("la") : super.getLabelId();
    }
}
