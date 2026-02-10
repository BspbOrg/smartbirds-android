package org.bspb.smartbirds.pro.ui

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.core.app.TaskStackBuilder
import org.bspb.smartbirds.pro.R
import org.bspb.smartbirds.pro.prefs.SmartBirdsPrefs
import org.bspb.smartbirds.pro.service.DataService

open class SplashScreenActivity : BaseActivity(), Runnable {

    protected lateinit var content: View

    protected lateinit var prefs: SmartBirdsPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportRequestWindowFeature(1)
        setContentView(R.layout.activity_splash_screen)

        prefs = SmartBirdsPrefs(this)
        content = findViewById(android.R.id.content)
    }

    override fun getInsetContainerId(): Int = android.R.id.content

    override fun onResume() {
        super.onResume()
        content.postDelayed(this, SPLASH_DURATION.toLong())
    }

    override fun onPause() {
        content.removeCallbacks(this)
        super.onPause()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setContentView(R.layout.activity_splash_screen)
    }

    override fun run() {
        if (prefs.getRunningMonitoring()) {
            startService(DataService.intent(this))
            TaskStackBuilder.create(this@SplashScreenActivity)
                .addNextIntentWithParentStack(
                    MonitoringActivity.newIntent(this@SplashScreenActivity)
                )
                .startActivities()
        } else {
            startActivity(Intent(this@SplashScreenActivity, MainActivity::class.java))
        }
        finish()
    }

    companion object {
        // Duration in milliseconds
        private const val SPLASH_DURATION = 3000
    }
}
