package org.bspb.smartbirds.pro.ui.utils;

import android.content.Context;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by groupsky on 22.03.16.
 */
public class Configuration {

    public static final String STORAGE_VERSION_CODE = "4";

    public static final String MULTIPLE_CHOICE_DELIMITER = " | ";
    public static final String MULTIPLE_CHOICE_SPLITTER = " *\\| *";
    // Max distance to location for auto population of common form location
    public static final float MAX_DISTANCE_LOCATION_METERS = 50000;
    // Max distance to zone beyond that will display a warning
    public static final float MAX_DISTANCE_TO_ZONE_METERS = 1000;
    public static DateFormat STORAGE_DATE_TIME_FORMAT;
    public static DateFormat STORAGE_TIME_FORMAT;
    public static DateFormat STORAGE_DATE_FORMAT;

    public static final String LOCAL_PROJECTS_KML_FILE = "white_stork.kml";
    public static final String FALLBACK_LANGUAGE = "en";
    public static final String NOMENCLATURE_ID_LANGUAGE = "en";

    /**
     * The minimum number of items to display a filter
     */
    public static final int ITEM_COUNT_FOR_FILTER = 20;
    /**
     * How many recently used values to display, only applicable if @ITEM_COUNT_FOR_FILTER is matched
     */
    public static final int MAX_RECENT_USED_VALUES = 10;
    public static final int IMAGE_DOWNSCALE_SIZE = 1024;

    public static void init(Context context) {
        STORAGE_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH);
        STORAGE_TIME_FORMAT = new SimpleDateFormat("kk:mm:ss", Locale.ENGLISH);
        STORAGE_DATE_TIME_FORMAT = new SimpleDateFormat("dd.MM.yyyy kk:mm:ss", Locale.ENGLISH);
    }

    public static Date parseDateTime(String date, String time) throws ParseException {
        return STORAGE_DATE_TIME_FORMAT.parse(date + " " + time);
    }
}
