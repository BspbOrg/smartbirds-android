package org.bspb.smartbirds.pro.forms.convert

import android.content.Context
import org.bspb.smartbirds.pro.R

class BearsConverter(context: Context?) : Converter(context) {
    init {
        // Bats main form
        addBool(R.string.tag_confidential, "confidential")
        addBool(R.string.tag_moderator_review, "moderatorReview")
        add(R.string.tag_remarks_type, "speciesNotes")
        addMulti(R.string.tag_threats, "threats")

        addSpecies(R.string.tag_species_scientific_name, "species")
        add(R.string.tag_count, "count", "0")
        addSingle(R.string.tag_sex, "sex")
        addSingle(R.string.tag_age, "age")
        addMulti(R.string.tag_excrement_content, "excrementContent")
        addSingle(R.string.tag_excrement_consistence, "excrementConsistence")
        add(R.string.tag_marking_height, "markingHeight")
        addSingle(R.string.tag_den, "den")
        addMulti(R.string.tag_habitat, "habitat")
        addMulti(R.string.tag_threats_other, "threatsBears")
        addMulti(R.string.tag_findings, "findings")

        // Additional fields
        add(R.string.tag_footprint_front_paw_width, "footprintFrontPawWidth")
        add(R.string.tag_footprint_front_paw_length, "footprintFrontPawLength")
        add(R.string.tag_footprint_hind_paw_width, "footprintHindPawWidth")
        add(R.string.tag_footprint_hind_paw_length, "footprintHindPawLength")
    }
}
