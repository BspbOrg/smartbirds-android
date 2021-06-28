package org.bspb.smartbirds.pro.ui.views

import android.text.TextUtils
import androidx.annotation.VisibleForTesting
import org.bspb.smartbirds.pro.backend.dto.Nomenclature
import org.bspb.smartbirds.pro.ui.utils.Configuration

class NomenclatureItem {

    @VisibleForTesting
    var nomenclature: Nomenclature? = null
    var label: String? = null

    constructor(label: String) {
        nomenclature = null
        this.label = prepare(label)
    }

    constructor(nomenclature: Nomenclature) {
        this.nomenclature = nomenclature
        label = prepare(nomenclature.localeLabel)
    }

    private fun prepare(label: String): String? {
        return if (TextUtils.isEmpty(label)) "" else TextUtils.join(
            "\n", label.trim { it <= ' ' }.split(
                Configuration.MULTIPLE_CHOICE_SPLITTER.toRegex()
            ).toTypedArray()
        )
    }


    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as NomenclatureItem
        return label == that.label
    }

    override fun hashCode(): Int {
        return label.hashCode()
    }

    override fun toString(): String {
        return label ?: ""
    }

}