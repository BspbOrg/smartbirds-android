package org.bspb.smartbirds.pro.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import org.bspb.smartbirds.pro.R
import org.bspb.smartbirds.pro.enums.EntryType

open class ViewMonitoringEntryActivity : BaseActivity() {

    companion object {
        fun newIntent(context: Context, entryId: Long, entryType: EntryType?): Intent {
            return Intent(context, ViewMonitoringEntryActivity::class.java).apply {
                putExtra("entryId", entryId)
                entryType?.let {
                    putExtra("entryType", it.name)
                }
            }
        }
    }

    protected var entryId: Long = 0
    protected lateinit var entryType: EntryType

    protected var fragment: Fragment? = null

    override fun getInsetContainerId(): Int = R.id.container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form)

        intent.extras?.let {
            entryId = it.getLong("entryId", 0)
            entryType = EntryType.valueOf(it.getString("entryType", ""))
        }

        setupFragment()
    }

    protected open fun setupFragment() {
        fragment = supportFragmentManager.findFragmentById(R.id.container)
        if (entryId <= 0) {
            finish()
            return
        }
        if (fragment == null) {
            fragment = entryType.loadFragment(entryId, true)
            supportFragmentManager.beginTransaction().replace(R.id.container, fragment!!).commit()
        }
    }
}