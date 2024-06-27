package org.bspb.smartbirds.pro.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import org.androidannotations.annotations.EFragment
import org.androidannotations.annotations.FragmentById
import org.androidannotations.annotations.ViewById
import org.bspb.smartbirds.pro.R
import org.bspb.smartbirds.pro.enums.EntryType
import org.bspb.smartbirds.pro.prefs.CommonPrefs
import org.bspb.smartbirds.pro.ui.views.SwitchFormInput
import java.util.Date

@EFragment(R.layout.fragment_monitoring_form_new_pylons_entry)
open class NewPylonsEntryFormFragment : BaseEntryFragment() {

    @JvmField
    @FragmentById(value = R.id.pictures_fragment, childFragment = true)
    protected var picturesFragment: NewEntryPicturesFragment? = null


    @JvmField
    @ViewById(R.id.form_pylons_confidential)
    protected var confidential: SwitchFormInput? = null

    @JvmField
    @ViewById(R.id.form_pylons_pylon_insulated)
    protected var pylonInsulated: SwitchFormInput? = null

    @JvmField
    @ViewById(R.id.form_pylons_damaged_insulation)
    protected var damagedInsulation: SwitchFormInput? = null

    protected lateinit var commonPrefs: CommonPrefs

    override fun onResume() {
        super.onResume()
        if (isNewEntry) {
            confidential!!.isChecked = commonPrefs.getConfidentialRecord()
        }
    }

    override fun onPause() {
        super.onPause()
        commonPrefs.setConfidentialRecord(confidential!!.isChecked)
    }

    override fun getEntryType(): EntryType? {
        return EntryType.PYLONS
    }

    override fun serialize(entryTime: Date?): HashMap<String, String> {
        val data = super.serialize(entryTime)
        data.putAll(picturesFragment!!.serialize())
        return data
    }

    override fun deserialize(data: HashMap<String?, String?>) {
        super.deserialize(data)
        // In some cases picturesFragment is still null. Try to find it by id
        if (picturesFragment == null) {
            picturesFragment =
                childFragmentManager.findFragmentById(R.id.pictures_fragment) as NewEntryPicturesFragment?
        }
        if (picturesFragment != null) {
            picturesFragment!!.doDeserialize(monitoringCode, data)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (picturesFragment == null) {
            picturesFragment =
                childFragmentManager.findFragmentById(R.id.pictures_fragment) as NewEntryPicturesFragment?
        }
        commonPrefs = CommonPrefs(requireContext())
        super.onViewCreated(view, savedInstanceState)
    }

    override fun initViews() {
        super.initViews()
        pylonInsulated?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                damagedInsulation?.isEnabled = true
            } else {
                damagedInsulation?.apply {
                    setChecked(false)
                    isEnabled = false
                }
            }
        }
    }

    class Builder : BaseEntryFragment.Builder {
        override fun build(lat: Double, lon: Double, geolocationAccuracy: Double): Fragment? {
            return NewPylonsEntryFormFragment_.builder().lat(lat).lon(lon)
                .geolocationAccuracy(geolocationAccuracy)
                .build()
        }

        override fun load(id: Long, readOnly: Boolean): Fragment? {
            return NewPylonsEntryFormFragment_.builder().entryId(id).readOnly(readOnly).build()
        }
    }
}