package org.bspb.smartbirds.pro.forms.convert;

import android.content.Context;

import org.bspb.smartbirds.pro.R;

public class InvertebratesConverter extends Converter {

    public InvertebratesConverter(Context context) {
        super(context);

        addSpecies(R.string.tag_species_scientific_name, "species");
        addSingle(R.string.tag_sex, "sex");
        addSingle(R.string.tag_age, "age");
        addSingle(R.string.tag_habitat, "habitat");
        addMulti(R.string.tag_threats_other, "threatsInvertebrates");
        addMulti(R.string.tag_findings, "findings");
        add(R.string.tag_count, "count");
        add(R.string.tag_marking, "marking");
        add(R.string.tag_remarks_type, "speciesNotes");
        addBool(R.string.tag_confidential, "confidential");
        addMulti(R.string.tag_threats, "threats");
        addBool(R.string.tag_moderator_review, "moderatorReview");
    }

}
