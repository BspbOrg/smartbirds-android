package org.bspb.smartbirds.pro.backend;

import android.content.Context;
import android.support.annotation.StringRes;
import android.text.TextUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import org.bspb.smartbirds.pro.R;
import org.bspb.smartbirds.pro.ui.utils.NomenclaturesBean_;
import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;

import static org.bspb.smartbirds.pro.ui.utils.Configuration.MULTIPLE_CHOICE_SPLITTER;
import static org.bspb.smartbirds.pro.ui.utils.Configuration.STORAGE_DATE_FORMAT;
import static org.bspb.smartbirds.pro.ui.utils.Configuration.STORAGE_TIME_FORMAT;

/**
 * Created by groupsky on 26.09.16.
 */

public class Converter {

    private static List<Convert> commonMapping = new ArrayList<>();
    private static List<Convert> birdsMapping = new ArrayList<>();

    private static void add(Context context, List<Convert> list, @StringRes int csvFieldName, String jsonFieldName) {
        add(context, list, csvFieldName, jsonFieldName, null);
    }

    private static void add(Context context, List<Convert> list, @StringRes int csvFieldName, String jsonFieldName, String defaultValue) {
        list.add(new SimpleConverter(context.getString(csvFieldName), jsonFieldName, defaultValue));
    }

    private static void addSingle(Context context, List<Convert> list, @StringRes int csvFieldName, String jsonFieldName) {
        list.add(new SingleChoiceConverter(context, context.getString(csvFieldName), jsonFieldName));
    }

    private static void addMulti(Context context, List<Convert> list, @StringRes int csvFieldName, String jsonFieldName) {
        list.add(new MultipleChoiceConverter(context, context.getString(csvFieldName), jsonFieldName));
    }

    private static void addBool(Context context, List<Convert> list, @StringRes int csvFieldName, String jsonFieldName) {
        list.add(new BooleanConverter(context.getString(csvFieldName), jsonFieldName));
    }

    private static void addSpecies(Context context, List<Convert> list, @StringRes int csvFieldName, String jsonFieldName) {
        list.add(new SpeciesConverter(context, context.getString(csvFieldName), jsonFieldName));
    }

    private static void addDateTime(Context context, List<Convert> list, @StringRes int csvDateFieldName, @StringRes int csvTimeFieldName, String jsonFieldName) {
        list.add(new DateTimeConverter(context.getString(csvDateFieldName), context.getString(csvTimeFieldName), jsonFieldName));
    }

    public static void init(Context context) {
        // common form
        add(context, commonMapping, R.string.tag_location, "location");
        add(context, commonMapping, R.string.tag_lat, "latitude");
        add(context, commonMapping, R.string.tag_lon, "longitude");
        addDateTime(context, commonMapping, R.string.entry_date, R.string.entry_time, "observationDateTime");
        add(context, commonMapping, R.string.monitoring_id, "monitoringCode");
        addDateTime(context, commonMapping, R.string.tag_begin_date, R.string.tag_begin_time, "startDateTime");
        addDateTime(context, commonMapping, R.string.tag_end_date, R.string.tag_end_time, "endDateTime");
        add(context, commonMapping, R.string.tag_observers, "observers");
        addSingle(context, commonMapping, R.string.tag_rain, "rain");
        add(context, commonMapping, R.string.tag_temperature, "temperature");
        addSingle(context, commonMapping, R.string.tag_wind_direction, "windDirection");
        addSingle(context, commonMapping, R.string.tag_wind_strength, "windSpeed");
        addSingle(context, commonMapping, R.string.tag_cloudiness, "cloudiness");
        add(context, commonMapping, R.string.tag_clouds_type, "cloudsType");
        add(context, commonMapping, R.string.tag_visibility_km, "visibility");
        add(context, commonMapping, R.string.tag_weather_other, "mto");
        add(context, commonMapping, R.string.tag_notes, "notes");
        addMulti(context, commonMapping, R.string.tag_threats, "threats");

        // birds
        birdsMapping.addAll(commonMapping);
        addSingle(context, birdsMapping, R.string.tag_source, "source");
        addSpecies(context, birdsMapping, R.string.tag_species_scientific_name, "species");
        addBool(context, birdsMapping, R.string.tag_confidential, "confidential");
        addSingle(context, birdsMapping, R.string.tag_count_unit, "countUnit");
        addSingle(context, birdsMapping, R.string.tag_count_type, "typeUnit");
        addSingle(context, birdsMapping, R.string.tag_nest_type, "typeNesting");
        add(context, birdsMapping, R.string.tag_count, "count", "0");
        add(context, birdsMapping, R.string.tag_min, "countMin", "0");
        add(context, birdsMapping, R.string.tag_max, "countMax", "0");
        addSingle(context, birdsMapping, R.string.tag_sex, "sex");
        addSingle(context, birdsMapping, R.string.tag_age, "age");
        addSingle(context, birdsMapping, R.string.tag_marking, "marking");
        addSingle(context, birdsMapping, R.string.tag_bird_status, "speciesStatus");
        addMulti(context, birdsMapping, R.string.tag_behavior, "behaviour");
        addSingle(context, birdsMapping, R.string.tag_dead_specimen_reason, "deadIndividualCauses");
        addSingle(context, birdsMapping, R.string.tag_substrate, "substrate");
        add(context, birdsMapping, R.string.tag_tree, "tree");
        add(context, birdsMapping, R.string.tag_tree_height, "treeHeight");
        addSingle(context, birdsMapping, R.string.tag_tree_location, "treeLocation");
        addSingle(context, birdsMapping, R.string.tag_nest_height, "nestHeight");
        addSingle(context, birdsMapping, R.string.tag_nest_location, "nestLocation");
        addSingle(context, birdsMapping, R.string.tag_tree_location, "treeLocation");
        addBool(context, birdsMapping, R.string.tag_incubation, "brooding");
        add(context, birdsMapping, R.string.tag_number_of_eggs, "eggsCount");
        add(context, birdsMapping, R.string.tag_number_of_pull, "countNestling");
        add(context, birdsMapping, R.string.tag_number_of_fledglings, "countFledgling");
        add(context, birdsMapping, R.string.tag_number_fledged_juveniles, "countSuccessfullyLeftNest");
        addBool(context, birdsMapping, R.string.tag_nest_guarding, "nestProtected");
        addSingle(context, birdsMapping, R.string.tag_age_female, "ageFemale");
        addSingle(context, birdsMapping, R.string.tag_age_male, "ageMale");
        addSingle(context, birdsMapping, R.string.tag_breeding_success, "nestingSuccess");
        add(context, birdsMapping, R.string.tag_land_uses_300m, "landuse300mRadius");
        add(context, birdsMapping, R.string.tag_remarks_type, "speciesNotes");
    }

    public static JsonObject convertBirds(List<String> header, String[] row) throws Exception {
        HashMap<String, String> csv = new HashMap<>();
        Iterator<String> it = header.iterator();
        String columnName;
        for (int idx = 0; it.hasNext() && idx < row.length; idx++) {
            columnName = it.next();
            csv.put(columnName, row[idx]);
        }

        JsonObject result = new JsonObject();
        HashSet<String> usedCsvColumns = new HashSet<>();
        for (Convert converter : birdsMapping) {
            converter.convert(csv, result, usedCsvColumns);
        }

        return result;
    }

    interface Convert {
        void convert(Map<String, String> csv, JsonObject json, Set<String> usedCsvFields) throws Exception;
    }

    private static class SimpleConverter implements Convert {
        final String csvField;
        final String jsonField;
        final String defaultValue;

        SimpleConverter(String csvField, String jsonField, String defaultValue) {
            this.csvField = csvField;
            this.jsonField = jsonField;
            this.defaultValue = defaultValue;
        }

        @Override
        public void convert(Map<String, String> csv, JsonObject json, Set<String> usedCsvFields) throws Exception {
            String value = csv.get(csvField);
            if (TextUtils.isEmpty(value)) value = defaultValue;
            json.addProperty(jsonField, value);
            usedCsvFields.add(csvField);
        }
    }

    private static class BooleanConverter implements Convert {
        final String csvField;
        final String jsonField;

        BooleanConverter(String csvField, String jsonField) {
            this.csvField = csvField;
            this.jsonField = jsonField;
        }

        @Override
        public void convert(Map<String, String> csv, JsonObject json, Set<String> usedCsvFields) throws Exception {
            String value = csv.get(csvField);
            if (!TextUtils.isEmpty(value)) {
                json.addProperty(jsonField, "1".equals(value));
            } else {
                json.add(jsonField, JsonNull.INSTANCE);
            }
            usedCsvFields.add(csvField);
        }
    }

    private static class DateTimeConverter implements Convert {

        static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz", Locale.ENGLISH);
        static final TimeZone tz = TimeZone.getTimeZone("UTC");

        static {
            df.setTimeZone(tz);
        }

        final String csvDateField;
        final String csvTimeField;
        final String jsonField;

        public DateTimeConverter(String csvDateField, String csvTimeField, String jsonField) {
            this.csvDateField = csvDateField;
            this.csvTimeField = csvTimeField;
            this.jsonField = jsonField;
        }

        @Override
        public void convert(Map<String, String> csv, JsonObject json, Set<String> usedCsvFields) throws Exception {
            String dateValue = csv.get(csvDateField);
            String timeValue = csv.get(csvTimeField);
            if (TextUtils.isEmpty(dateValue) && TextUtils.isEmpty(timeValue)) {
                json.add(jsonField, JsonNull.INSTANCE);
            } else {
                Date date = STORAGE_DATE_FORMAT.parse(dateValue);
                Date time = STORAGE_TIME_FORMAT.parse(timeValue);

                String output = df.format(new Date(date.getTime() + time.getTime()));

                int inset0 = 9;
                int inset1 = 6;

                String s0 = output.substring(0, output.length() - inset0);
                String s1 = output.substring(output.length() - inset1, output.length());

                String result = s0 + s1;

                result = result.replaceAll("UTC", "+00:00");

                json.addProperty(jsonField, result);
            }

            usedCsvFields.add(csvDateField);
            usedCsvFields.add(csvTimeField);
        }
    }

    private static class SingleChoiceConverter implements Convert {

        final String locale;
        final String csvField;
        final String jsonField;

        SingleChoiceConverter(Context context, String csvField, String jsonField) {
            this.locale = context.getString(R.string.locale);
            this.csvField = csvField;
            this.jsonField = jsonField;
        }

        @Override
        public void convert(Map<String, String> csv, JsonObject json, Set<String> usedCsvFields) throws Exception {
            String value = csv.get(csvField);
            if (TextUtils.isEmpty(value)) {
                json.add(jsonField, JsonNull.INSTANCE);
            } else {
                JsonObject label = new JsonObject();
                label.addProperty(locale, value);
                JsonObject field = new JsonObject();
                field.add("label", label);
                json.add(jsonField, field);
            }
            usedCsvFields.add(csvField);
        }
    }

    private static class SpeciesConverter implements Convert {

        final String locale;
        final String csvField;
        final String jsonField;

        SpeciesConverter(Context context, String csvField, String jsonField) {
            this.locale = context.getString(R.string.locale);
            this.csvField = csvField;
            this.jsonField = jsonField;
        }

        @Override
        public void convert(Map<String, String> csv, JsonObject json, Set<String> usedCsvFields) throws Exception {
            String value = csv.get(csvField);
            if (!TextUtils.isEmpty(value)) {
                String[] values = value.split(" *\\| *");
                json.addProperty(jsonField, values[0]);
            } else {
                json.add(jsonField, JsonNull.INSTANCE);
            }

            usedCsvFields.add(csvField);
        }
    }

    private static class MultipleChoiceConverter implements Convert {
        final String locale;
        final String csvField;
        final String jsonField;

        MultipleChoiceConverter(Context context, String csvField, String jsonField) {
            this.locale = context.getString(R.string.locale);
            this.csvField = csvField;
            this.jsonField = jsonField;
        }

        @Override
        public void convert(Map<String, String> csv, JsonObject json, Set<String> usedCsvFields) throws Exception {
            String value = csv.get(csvField);
            if (!TextUtils.isEmpty(value)) {
                String[] values = value.split(MULTIPLE_CHOICE_SPLITTER);

                JsonArray jsValues = new JsonArray();
                for (String val : values) {
                    JsonObject label = new JsonObject();
                    label.addProperty(locale, val);
                    JsonObject jsVal = new JsonObject();
                    jsVal.add("label", label);
                    jsValues.add(jsVal);
                }
                json.add(jsonField, jsValues);
            } else {
                json.add(jsonField, JsonNull.INSTANCE);
            }

            usedCsvFields.add(csvField);
        }
    }
}
