package org.bspb.smartbirds.pro.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.bspb.smartbirds.pro.R
import org.bspb.smartbirds.pro.enums.EntryType
import org.bspb.smartbirds.pro.prefs.CommonPrefs
import org.bspb.smartbirds.pro.ui.views.SwitchFormInput
import java.util.Date

class NewPylonsEntryFormFragment : BaseEntryFragment() {

    private var picturesFragment: NewEntryPicturesFragment? = null
    private var confidential: SwitchFormInput? = null
    private var pylonInsulated: SwitchFormInput? = null
    private var damagedInsulation: SwitchFormInput? = null
    private lateinit var commonPrefs: CommonPrefs

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState) ?: inflater.inflate(
            R.layout.fragment_monitoring_form_new_pylons_entry,
            container,
            false
        )
    }

    override fun onBeforeCreate(savedInstanceState: Bundle?) {
        super.onBeforeCreate(savedInstanceState)
        commonPrefs = CommonPrefs(requireContext())
    }

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

    override fun serialize(): HashMap<String, String> {
        val data = super.serialize()
        data.putAll(picturesFragment!!.serialize())
        return data
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
        super.onViewCreated(view, savedInstanceState)
    }

    override fun initViews() {
        super.initViews()
        confidential = view?.findViewById(R.id.form_pylons_confidential)
        pylonInsulated = view?.findViewById(R.id.form_pylons_pylon_insulated)
        damagedInsulation = view?.findViewById(R.id.form_pylons_damaged_insulation)

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
        override fun build(lat: Double, lon: Double, geolocationAccuracy: Double): Fragment {
            return NewPylonsEntryFormFragment().apply {
                arguments = Bundle().apply {
                    putDouble(ARG_LAT, lat)
                    putDouble(ARG_LON, lon)
                    putDouble(ARG_GEOLOCATION_ACCURACY, geolocationAccuracy)
                }
            }
        }

        override fun load(id: Long, readOnly: Boolean): Fragment {
            return NewPylonsEntryFormFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_ENTRY_ID, id)
                    putBoolean(ARG_READ_ONLY, readOnly)
                }
            }
        }
    }
}