package org.bspb.smartbirds.pro.ui.fragment;

import androidx.fragment.app.Fragment;

import android.os.Bundle;

import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;
import org.bspb.smartbirds.pro.R;
import org.bspb.smartbirds.pro.enums.EntryType;
import org.bspb.smartbirds.pro.ui.utils.FormUtils;
import org.bspb.smartbirds.pro.ui.views.FormBirdsList;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

/**
 * Created by groupsky on 19.12.16.
 */

@EFragment(R.layout.fragment_monitoring_form_humid_birds)
public class NewHumidBirdsEntryFragment extends BaseEntryFragment {

    @ViewById(R.id.form_birds_list)
    FormBirdsList birdsList;

    FormUtils.FormModel fakeForm = new FormUtils.FormModel();

    @Override
    protected EntryType getEntryType() {
        return EntryType.HUMID;
    }

    @Override
    protected boolean ensureForm() {
        if (form == null) {
            throw new IllegalStateException("Should not be called with null form!");
        }
        // sometimes birdsList is null
        return birdsList != null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        form = fakeForm;
        try {
            super.onSaveInstanceState(outState);
        } finally {
            form = null;
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        form = fakeForm;
        try {
            super.onRestoreInstanceState(savedInstanceState);
        } finally {
            form = null;
        }
    }

    @Override
    protected boolean isValid() {
        for (FormUtils.FormModel model : birdsList.getModels()) {
            if (!model.validateFields())
                return false;
        }
        return true;
    }

    @Override
    protected void doDeserialize(String monitoringCode, HashMap<String, String> data) {
        form = fakeForm;
        try {
            super.doDeserialize(monitoringCode, data);
        } finally {
            form = null;
        }
    }

    @Override
    protected void deserialize(HashMap<String, String> data) {
        form = fakeForm;
        try {
            super.deserialize(data);
            ArrayList<HashMap<String, String>> models = new ArrayList<>();
            models.add(data);
            birdsList.deserialize(models);
        } finally {
            form = null;
        }
    }

    @Override
    protected void submitData() {
        ArrayList<FormUtils.FormModel> models = birdsList.getModels();
        Calendar entryTime = new GregorianCalendar();
        if (getEntryTimestamp() != null) {
            entryTime.setTime(getEntryTimestamp());
        }
        entryTime.add(Calendar.SECOND, -models.size());
        try {
            for (FormUtils.FormModel model : models) {
                form = model;
                entryTime.add(Calendar.SECOND, 1);
                submitData(serialize(entryTime.getTime()));
            }
        } finally {
            form = null;
        }
    }

    @Override
    public boolean isDirty() {
        ArrayList<FormUtils.FormModel> models = birdsList.getModels();
        if (models.size() != 1) return false;
        form = models.get(0);
        try {
            return super.isDirty();
        } finally {
            form = null;
        }
    }

    public static class Builder implements BaseEntryFragment.Builder {

        @Override
        public Fragment build(double lat, double lon, double geolocationAccuracy) {
            return NewHumidBirdsEntryFragment_.builder().lat(lat).lon(lon).geolocationAccuracy(geolocationAccuracy).build();
        }

        @Override
        public Fragment load(long id, boolean readOnly) {
            return NewHumidBirdsEntryFragment_.builder().entryId(id).readOnly(readOnly).build();
        }
    }
}
