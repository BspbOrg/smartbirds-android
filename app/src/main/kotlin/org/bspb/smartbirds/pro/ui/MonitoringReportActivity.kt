package org.bspb.smartbirds.pro.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.commit
import org.bspb.smartbirds.pro.R
import org.bspb.smartbirds.pro.ui.fragment.MonitoringReportFragment

class MonitoringReportActivity : BaseActivity() {

    override fun getInsetContainerId(): Int = R.id.fragment_container

    companion object {
        const val EXTRA_MONITORING_CODE = "monitoringCode"

        fun newIntent(context: Context, monitoringCode: String): Intent {
            val intent = Intent(context, MonitoringReportActivity::class.java)
            intent.putExtra(EXTRA_MONITORING_CODE, monitoringCode)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_fragment)
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(
                    R.id.fragment_container, MonitoringReportFragment.newInstance(
                        intent.getStringExtra(
                            EXTRA_MONITORING_CODE
                        )!!
                    )
                )
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            onBackPressed()
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

}