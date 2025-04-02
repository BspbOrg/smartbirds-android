package org.bspb.smartbirds.pro.forms.convert;

import android.content.Context;

import org.bspb.smartbirds.pro.R;

/**
 * Created by dani on 04.01.18.
 */

public class HerptileConverter extends Converter {

    public HerptileConverter(Context context) {
        super(context);

        addSpecies(R.string.tag_species_scientific_name, "species");
        addSingle(R.string.tag_sex, "sex");
        addSingle(R.string.tag_age, "age");
        addSingle(R.string.tag_habitat, "habitat");
        addMulti(R.string.tag_threats_other, "threatsHerptiles");
        addMulti(R.string.tag_findings, "findings");
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
        addBool(R.string.tag_confidential, "confidential");
        addMulti(R.string.tag_threats, "threats");
        addBool(R.string.tag_moderator_review, "moderatorReview");
        add(R.string.tag_traps_count, "trapsCount");
    }

}
