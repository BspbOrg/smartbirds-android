package org.bspb.smartbirds.pro.sync

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import org.bspb.smartbirds.pro.backend.Backend
import org.bspb.smartbirds.pro.db.SmartBirdsDatabase
import org.bspb.smartbirds.pro.db.model.ZoneModel
import org.bspb.smartbirds.pro.tools.Reporting
import org.bspb.smartbirds.pro.tools.SBGsonParser
import java.io.IOException

open class ZonesManager(private var context: Context) {

    companion object {
        var isDownloading = false
    }

    protected val backend: Backend by lazy { Backend.getInstance() }

    open fun downloadZones() {
        isDownloading = true
        try {
            try {
                val zones = mutableListOf<ZoneModel>()
                val response = backend.api().listZones().execute()
                if (!response.isSuccessful) throw IOException("Server error: " + response.code() + " - " + response.message())
                for (zone in response.body()!!.data) {
                    zones.add(
                        ZoneModel(
                            zone.id,
                            zone.locationId.toInt(),
                            SBGsonParser.createParser().toJson(zone)
                        )
                    )
                }

                SmartBirdsDatabase.getInstance().zoneDao().updateZonesAndClearOld(zones)
            } catch (t: Throwable) {
                Reporting.logException(t)
                showToast("Could not download zones. Try again.")
            }
        } finally {
            isDownloading = false
        }
    }

    protected open fun showToast(message: String?) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                context.applicationContext,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

}