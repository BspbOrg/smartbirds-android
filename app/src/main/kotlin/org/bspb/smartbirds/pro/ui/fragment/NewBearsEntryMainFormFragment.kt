package org.bspb.smartbirds.pro.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.bspb.smartbirds.pro.R
import org.bspb.smartbirds.pro.tools.Reporting
import org.bspb.smartbirds.pro.ui.views.NomenclatureItem
import org.bspb.smartbirds.pro.ui.views.SingleChoiceFormInput
import org.bspb.smartbirds.pro.ui.views.SwitchFormInput
import org.bspb.smartbirds.pro.utils.NomenclaturesManager

class NewBearsEntryMainFormFragment : BaseFormFragment() {

    private var picturesFragment: NewEntryPicturesFragment? = null
    private var confidential: SwitchFormInput? = null
    private var speciesInput: SingleChoiceFormInput? = null

    companion object {
        fun newInstance(isNewEntry: Boolean, readOnly: Boolean): NewBearsEntryMainFormFragment {
            return NewBearsEntryMainFormFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_IS_NEW_ENTRY, isNewEntry)
                    putBoolean(ARG_READ_ONLY, readOnly)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState) ?: inflater.inflate(
            R.layout.fragment_monitoring_form_new_bears_entry,
            container,
            false
        )
    }

    override fun initViews() {
        super.initViews()
        confidential = view?.findViewById(R.id.form_bears_confidential)
        speciesInput = view?.findViewById(R.id.form_bears_species)
    }

    override fun onResume() {
        super.onResume()
        if (isNewEntry) {
            confidential!!.isChecked = true
            // Set default species to "Ursus arctos" (Brown Bear)
            setDefaultSpecies()
        }
    }

    private fun setDefaultSpecies() {
        try {
            val nomenclaturesManager = NomenclaturesManager.getInstance()
            val speciesList = nomenclaturesManager.getNomenclature("species_mammals")
            val bearSpecies = speciesList.find { species ->
                species.label.labelId.equals("Ursus arctos", ignoreCase = true)
            }
            bearSpecies?.let { nomenclature ->
                nomenclature.localeLabel =
                    nomenclature.label.get(context?.getString(R.string.locale))
                speciesInput?.setSelectionIfAvailable(NomenclatureItem(nomenclature))
            }
        } catch (t: Throwable) {
            Reporting.logException(t)
        }
    }

    override fun serialize(): HashMap<String, String> {
        val data = super.serialize()
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
}
