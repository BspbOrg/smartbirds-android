package org.bspb.smartbirds.pro.viewmodel

import androidx.lifecycle.ViewModel
import org.bspb.smartbirds.pro.content.MonitoringEntry
import org.bspb.smartbirds.pro.repository.FormRepository
import org.bspb.smartbirds.pro.room.Form
import org.bspb.smartbirds.pro.utils.MonitoringManagerNew

class BaseEntryViewModel : ViewModel() {

    private val formRepository = FormRepository()

    suspend fun loadEntry(id: Long): MonitoringEntry? {
        var formEntry: Form? = formRepository.findById(id) ?: return null
        return MonitoringManagerNew.entryFromDb(formEntry!!)
        return null
    }
}