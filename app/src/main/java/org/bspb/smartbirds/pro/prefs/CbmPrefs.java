package org.bspb.smartbirds.pro.prefs;

import org.androidannotations.annotations.sharedpreferences.SharedPref;

import java.util.List;

/**
 * Created by dani on 14-11-18.
 */
@SharedPref
public interface CbmPrefs {

    String cbmZone();

    String speciesQuick1();

    String speciesQuick2();

    String speciesQuick3();

    String speciesQuick4();

    String speciesQuick5();

    String speciesQuick6();

}
