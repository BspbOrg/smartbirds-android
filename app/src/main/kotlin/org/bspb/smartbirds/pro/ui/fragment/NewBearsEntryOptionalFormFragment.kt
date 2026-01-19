package org.bspb.smartbirds.pro.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.bspb.smartbirds.pro.R

class NewBearsEntryOptionalFormFragment : BaseFormFragment() {

    companion object {
        fun newInstance(isNewEntry: Boolean, readOnly: Boolean): NewBearsEntryOptionalFormFragment {
            return NewBearsEntryOptionalFormFragment().apply {
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
            R.layout.fragment_monitoring_form_new_bears_optional_entry,
            container,
            false
        )
    }


}
