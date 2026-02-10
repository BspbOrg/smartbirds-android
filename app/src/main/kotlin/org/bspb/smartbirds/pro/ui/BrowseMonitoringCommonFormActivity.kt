package org.bspb.smartbirds.pro.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import org.bspb.smartbirds.pro.R
import org.bspb.smartbirds.pro.SmartBirdsApplication
import org.bspb.smartbirds.pro.service.DataService
import org.bspb.smartbirds.pro.ui.fragment.BrowseMonitoringCommonFormFragment

open class BrowseMonitoringCommonFormActivity : BaseActivity() {
    companion object {
        private const val TAG = SmartBirdsApplication.TAG + ".EditMonitoring"

        fun newIntent(context: Context, monitoringCode: String?): Intent {
            return Intent(context, BrowseMonitoringCommonFormActivity::class.java).apply {
                monitoringCode?.let {
                    putExtra("monitoringCode", it)
                }
            }
        }
    }

    private var formFragmentBrowse: BrowseMonitoringCommonFormFragment? = null

    protected lateinit var monitoringCode: String

    override fun getInsetContainerId(): Int = R.id.container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_monitoring)

        intent.extras?.let {
            monitoringCode = it.getString("monitoringCode") ?: ""
        }

        createFragment()

        startService(DataService.intent(this))
    }

    private fun createFragment() {
        formFragmentBrowse =
            supportFragmentManager.findFragmentById(R.id.container) as BrowseMonitoringCommonFormFragment?
        if (formFragmentBrowse == null) {
            formFragmentBrowse =
                BrowseMonitoringCommonFormFragment.newInstance(monitoringCode)
            formFragmentBrowse?.let {
                supportFragmentManager.beginTransaction()
                    .add(R.id.container, it)
                    .commit()
            }
        }
        setTitle(R.string.title_activity_edit)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.monitoring_edit_common_form, menu)
        menu?.findItem(R.id.action_submit)?.title = getString(R.string.menu_monitoring_save)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.action_submit) {
            save()
            return true
        } else if (itemId == android.R.id.home) {
            cancel()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun save() {
        if (formFragmentBrowse!!.validate()) {
            formFragmentBrowse!!.save()
            setResult(RESULT_OK)
            finish()
        }
    }

    fun cancel() {
        setResult(RESULT_CANCELED)
        finish()
    }
}