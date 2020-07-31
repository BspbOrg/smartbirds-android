package org.bspb.smartbirds.pro.ui.fragment

import android.content.Context
import android.database.Cursor
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import org.androidannotations.annotations.*
import org.androidannotations.annotations.sharedpreferences.Pref
import org.bspb.smartbirds.pro.R
import org.bspb.smartbirds.pro.SmartBirdsApplication
import org.bspb.smartbirds.pro.events.EEventBus
import org.bspb.smartbirds.pro.events.GetMonitoringCommonData
import org.bspb.smartbirds.pro.events.MonitoringCommonData
import org.bspb.smartbirds.pro.events.SetMonitoringCommonData
import org.bspb.smartbirds.pro.prefs.CommonPrefs_
import org.bspb.smartbirds.pro.prefs.UserPrefs_
import org.bspb.smartbirds.pro.service.DataService_
import org.bspb.smartbirds.pro.ui.utils.FormUtils
import org.bspb.smartbirds.pro.ui.utils.FormUtils.FormModel
import org.bspb.smartbirds.pro.ui.views.DateFormInput
import org.bspb.smartbirds.pro.ui.views.MultipleTextFormInput
import org.bspb.smartbirds.pro.ui.views.TimeFormInput
import java.util.*

@EFragment(R.layout.fragment_monitoring_form_common)
open class MonitoringCommonFormFragment : Fragment() {

    companion object {
        private const val TAG = SmartBirdsApplication.TAG + ".CommonForm"
    }

    @Bean
    protected lateinit var bus: EEventBus

    private var form: FormModel? = null

    @ViewById(R.id.form_common_start_date)
    protected lateinit var startDateView: DateFormInput

    @ViewById(R.id.form_common_start_time)
    protected lateinit var startTimeView: TimeFormInput

    @ViewById(R.id.form_common_end_date)
    protected lateinit var endDateView: DateFormInput

    @Pref
    protected lateinit var prefs: CommonPrefs_

    @Pref
    protected lateinit var userPrefs: UserPrefs_

    @ViewById(R.id.observers)
    protected lateinit var observers: MultipleTextFormInput

    @ViewById(R.id.location)
    protected lateinit var locationView: TextView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        DataService_.intent(context).start()
    }

    override fun onStart() {
        super.onStart()
        bus.register(this)
        bus.postSticky(GetMonitoringCommonData())
    }

    override fun onResume() {
        super.onResume()
        observers.setText(prefs.commonOtherObservers().get())
    }

    override fun onPause() {
        super.onPause()
        prefs.commonOtherObservers().put(observers.text.toString())
    }

    override fun onStop() {
        bus.unregister(this)
        super.onStop()
    }

    @AfterViews
    open fun loadSavedData() {
        form = FormUtils.traverseForm(view)
        startDateView.value = Calendar.getInstance()
        startTimeView.setValue(Calendar.getInstance())
        endDateView.value = Calendar.getInstance()
    }

    @OptionsItem(R.id.action_submit)
    fun save() {
        val data = form!!.serialize()
        data[getString(R.string.tag_user_id)] = userPrefs.userId().get()
        data[getString(R.string.tag_user_first_name)] = userPrefs.firstName().get()
        data[getString(R.string.tag_user_last_name)] = userPrefs.lastName().get()
        data[getString(R.string.tag_user_email)] = userPrefs.email().get()
        bus.post(SetMonitoringCommonData(data))
    }

    fun onEventMainThread(event: MonitoringCommonData) {
        if (event.data != null && event.data.isNotEmpty()) {
            form!!.deserialize(event.data)
        }
    }

    fun validate(): Boolean {
        return form!!.validateFields()
    }
}