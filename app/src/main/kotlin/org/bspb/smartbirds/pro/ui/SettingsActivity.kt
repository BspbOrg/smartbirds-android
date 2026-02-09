package org.bspb.smartbirds.pro.ui

import android.os.Bundle
import android.view.MenuItem
import org.bspb.smartbirds.pro.R
import org.bspb.smartbirds.pro.ui.fragment.SettingsFragment

class SettingsActivity : BaseActivity() {

    override fun getInsetContainerId(): Int = R.id.fragment_container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_fragment)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, SettingsFragment())
                .commit()
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