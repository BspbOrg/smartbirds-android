package org.bspb.smartbirds.pro.forms.convert;

import android.content.Context;

import org.bspb.smartbirds.pro.R;

/**
 * Created by groupsky on 28.09.16.
 */

public class HerpConverter extends Converter {

    public HerpConverter(Context context) {
        super(context);

        addSpecies(R.string.tag_species_scientific_name, "species");
        addSingle(R.string.tag_sex, "sex");
        addSingle(R.string.tag_age, "age");
        addSingle(R.string.tag_habitat, "habitat");
        addMulti(R.string.tag_threats_other, "threatsHerps");
        add(R.string.tag_count, "count");
        add(R.string.tag_marking, "marking");
        add(R.string.tag_distance_from_axis, "axisDistance");
        add(R.string.tag_weight_g, "weight");
        add(R.string.tag_scl, "sCLL");
        add(R.string.tag_mpl, "mPLLcdC");
        add(R.string.tag_mcw, "mCWA");
        add(R.string.tag_lcap, "hLcapPl");
        add(R.string.tag_t_substrate, "tempSubstrat");
        add(R.string.tag_t_air, "tempAir");
        add(R.string.tag_t_cloaca, "tempCloaca");
        add(R.string.tag_sq_ventr, "sqVentr");
        add(R.string.tag_sq_caud, "sqCaud");
        add(R.string.tag_sq_dors, "sqDors");
        add(R.string.tag_remarks_type, "speciesNotes");
    }

}
