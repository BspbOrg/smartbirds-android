package org.bspb.smartbirds.pro.ui.utils;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import org.bspb.smartbirds.pro.SmartBirdsApplication;
import org.bspb.smartbirds.pro.ui.exception.ViewValidationException;
import org.bspb.smartbirds.pro.ui.views.SupportReadOnly;
import org.bspb.smartbirds.pro.ui.views.SupportRequiredView;
import org.bspb.smartbirds.pro.ui.views.SupportStorage;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by groupsky on 14-9-29.
 */

public class FormUtils {

    static final String TAG = SmartBirdsApplication.TAG + ".FormUtils";

    public static FormModel traverseForm(View view) {
        FormModel form = new FormModel();
        initModelFromViewHierarchy(view, form);
        return form;
    }

    private static void initModelFromViewHierarchy(View view, FormModel form) {
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                initModelFromViewHierarchy(vg.getChildAt(i), form);
            }
        }

        Object tag = view.getTag();
        if (tag instanceof String) {
            String field = (String) tag;
            if (!field.isEmpty()) {
                form.fields.put(field, new FormField(view, field));
            }
        }
    }

    public static final class FormField {
        public View view;
        public String field;

        @Override
        public String toString() {
            return "FormField{" +
                    "view=" + view.getClass().getSimpleName() + "(" + view.getId() + ")" +
                    ", field='" + field + '\'' +
                    '}';
        }

        public FormField(View view, String field) {
            this.view = view;
            this.field = field;
        }

        public void serialize(Map<String, String> storage) {
            FormUtils.serialize(storage, field, view);
        }

        public void deserialize(Map<String, String> storage) {
            FormUtils.deserialize(storage, field, view);
        }
    }

    public static final class FormModel {
        public HashMap<String, FormField> fields = new HashMap<String, FormField>();

        @Override
        public String toString() {
            return "FormModel{" +
                    "fields=" + fields +
                    '}';
        }

        public HashMap<String, String> serialize() {
            HashMap<String, String> values = new HashMap<String, String>();
            for (String key : fields.keySet()) {
                FormField field = fields.get(key);
                field.serialize(values);
            }
            return values;
        }

        public void deserialize(HashMap<String, String> values) {
            if (values == null) return;
            for (String key : fields.keySet()) {
                if (!values.containsKey(key)) continue;
                FormField field = fields.get(key);
                field.deserialize(values);
            }
        }

        public boolean validateFields() {
            boolean res = true;
            for (FormField field : fields.values()) {
                field.view.clearFocus();
                if (field.view instanceof SupportRequiredView) {
                    try {
                        ((SupportRequiredView) field.view).checkRequired();
                    } catch (ViewValidationException e) {
                        res = false;
                    }
                }
            }


            return res;

        }
    }

    public static void serialize(Map<String, String> storage, String field, View view) {
        // Check if view is readonly - readonly views should persist even when disabled
        boolean isReadOnly = (view instanceof SupportReadOnly) && ((SupportReadOnly) view).isReadOnly();

        if (!view.isEnabled() && !isReadOnly) {
            storage.put(field, "");
            return;
        }
        if (view instanceof SupportStorage) {
            SupportStorage storageView = (SupportStorage) view;
            storageView.serializeToStorage(storage, field);
            return;
        }
        String value;
        if (view instanceof EditText) {
            EditText editText = (EditText) view;
            value = editText.getText().toString();
        } else if (view instanceof CheckBox) {
            CheckBox checkBox = (CheckBox) view;
            value = checkBox.isChecked() ? "1" : "0";
        } else if (view instanceof TextView) {
            TextView textView = (TextView) view;
            value = textView.getText().toString();
        } else {
            Log.w(TAG, "unsupported view " + view.getClass());
            value = "";
        }
        storage.put(field, value);
    }

    public static void deserialize(Map<String, String> storage, String field, View view) {
        if (view instanceof SupportStorage) {
            SupportStorage storageView = (SupportStorage) view;
            storageView.restoreFromStorage(storage, field);
            return;
        }

        String value = storage.get(field);
        if (view instanceof EditText) {
            EditText editText = (EditText) view;
            editText.setText(value);
        } else if (view instanceof CheckBox) {
            CheckBox checkBox = (CheckBox) view;
            checkBox.setChecked("1".equals(value));
        } else if (view instanceof TextView) {
            TextView textView = (TextView) view;
            textView.setText(value);
        } else
            Log.w(TAG, "unsupported view " + view.getClass());
    }

}
