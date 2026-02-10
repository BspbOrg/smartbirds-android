package org.bspb.smartbirds.pro.ui

import android.os.Bundle
import androidx.fragment.app.commit
import org.bspb.smartbirds.pro.R
import org.bspb.smartbirds.pro.ui.fragment.DownloadsFragment

class DownloadsActivity : BaseActivity() {

    override fun getInsetContainerId(): Int = R.id.fragment_container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_fragment)

        title = getString(R.string.title_downloads)

        if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
            supportFragmentManager.commit(false) {
                add(R.id.fragment_container, DownloadsFragment())
            }
        }
    }

}