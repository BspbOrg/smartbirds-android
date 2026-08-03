package org.bspb.smartbirds.pro.service

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import org.bspb.smartbirds.pro.SmartBirdsApplication
import org.bspb.smartbirds.pro.events.EEventBus
import org.bspb.smartbirds.pro.ui.utils.NotificationUtils

class TrackingService : Service() {

    companion object {
        private const val TAG = SmartBirdsApplication.TAG + ".TrackingNew"
    }

    private var tracking = false
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private val eventBus: EEventBus by lazy { EEventBus.getInstance() }

    private val binder: IBinder = object : Binder() {
        val service: TrackingService = this@TrackingService
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.locations.forEach {
                if (tracking) {
                    eventBus.postSticky(it)
                }
            }
        }

    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (!tracking) {
            Log.v(TAG, "Tracking self-stopping")
            stopSelf()
        }

        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand...")
        startForeground(
            NotificationUtils.MONITORING_NOTIFICATION_ID,
            NotificationUtils.buildMonitoringNotification(this)
        )
        startTracking()
        return START_STICKY
    }

    override fun onDestroy() {
        Log.v(TAG, "Tracking destroying")
        stopTracking()
        super.onDestroy()
    }

    private fun startTracking() {
        Log.v(TAG, "Requesting location updates")
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (fusedLocationClient == null) {
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            }
            fusedLocationClient!!.requestLocationUpdates(
                createLocationRequest(),
                locationCallback,
                Looper.myLooper()!!
            )
        } else {
            throw IllegalStateException("No permission to retrieve location!")
        }
        tracking = true
    }

    private fun stopTracking() {
        fusedLocationClient?.removeLocationUpdates(locationCallback)
        tracking = false
    }

    private fun createLocationRequest(): LocationRequest = LocationRequest.create().apply {
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        interval = 1 * 1000
        fastestInterval = 1 * 1000
    }
}