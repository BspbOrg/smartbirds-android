package org.bspb.smartbirds.pro.ui.fragment

import android.Manifest.permission
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.text.Html
import android.text.Html.ImageGetter
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import org.androidannotations.annotations.*
import org.androidannotations.annotations.sharedpreferences.Pref
import org.bspb.smartbirds.pro.R
import org.bspb.smartbirds.pro.SmartBirdsApplication
import org.bspb.smartbirds.pro.content.Monitoring
import org.bspb.smartbirds.pro.content.MonitoringManager
import org.bspb.smartbirds.pro.events.*
import org.bspb.smartbirds.pro.prefs.MonitoringPrefs_
import org.bspb.smartbirds.pro.prefs.SmartBirdsPrefs_
import org.bspb.smartbirds.pro.service.*
import org.bspb.smartbirds.pro.tools.Reporting
import org.bspb.smartbirds.pro.ui.MonitoringListActivity_
import org.bspb.smartbirds.pro.ui.StatsActivity_
import java.util.*

@EFragment(R.layout.fragment_main)
@OptionsMenu(R.menu.menu_main)
open class MainFragment : Fragment() {

    private val REQUEST_LOCATION = 0
    private val REQUEST_STORAGE = 1

    private var exportDialog: AlertDialog? = null
    private var loading: LoadingDialog? = null

    @ViewById(R.id.btn_battery_optimization)
    protected lateinit var btnBatteryOptimization: ImageButton

    @ViewById(R.id.btn_upload)
    protected lateinit var btnUpload: Button

    @ViewById(R.id.btn_start_birds)
    protected lateinit var btnStartBirds: Button

    @ViewById(R.id.btn_resume_birds)
    protected lateinit var btnResumeBirds: Button

    @ViewById(R.id.btn_cancel_birds)
    protected lateinit var btnCancelBirds: Button

    @Pref
    protected lateinit var prefs: SmartBirdsPrefs_

    @Pref
    protected lateinit var monitoringPrefs: MonitoringPrefs_

    @Bean
    protected lateinit var monitoringManager: MonitoringManager

    @Bean
    protected lateinit var bus: EEventBus

    override fun onStart() {
        super.onStart()
        bus.registerSticky(this)
        if (UploadService.isUploading) {
            onEvent(StartingUpload())
        } else {
            onEvent(UploadCompleted())
        }
        if (NomenclatureService.isDownloading.isNotEmpty() || ZoneService.isDownloading) {
            onEvent(StartingDownload())
        } else {
            onEvent(DownloadCompleted())
        }
    }

    override fun onResume() {
        super.onResume()
        checkBatteryOptimization()
        showNotSyncedCount()
    }

    override fun onStop() {
        bus.unregister(this)
        super.onStop()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_LOCATION, REQUEST_STORAGE -> {
                var granted = true
                for (grantResult in grantResults) if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    granted = false
                    break
                }
                if (!granted) {
                    Toast.makeText(activity, R.string.permissions_required, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @AfterViews
    open fun setupMonitoringButtons() {
        if (prefs.pausedMonitoring().get()) {
            btnStartBirds.visibility = View.GONE
            btnResumeBirds.visibility = View.VISIBLE
            btnCancelBirds.visibility = View.VISIBLE
        } else {
            btnStartBirds.visibility = View.VISIBLE
            btnResumeBirds.visibility = View.GONE
            btnCancelBirds.visibility = View.GONE
        }
    }

    @Click(R.id.btn_start_birds)
    open fun startBirdsClicked() {
        if (!permissionsGranted()) return
        bus.postSticky(StartMonitoringEvent())
    }

    @Click(R.id.btn_resume_birds)
    open fun resumeBirdsClicked() {
        bus.postSticky(ResumeMonitoringEvent())
    }

    @Click(R.id.btn_cancel_birds)
    open fun cancelBirdsClicked() {
        confirmCancel()
    }

    @Click(R.id.btn_upload)
    open fun uploadBtnClicked() {
        SyncService_.intent(activity).sync().start()
    }

    @Click(R.id.btn_battery_optimization)
    open fun showBatteryOptimizationDialog() {
        AlertDialog.Builder(activity)
                .setTitle(getString(R.string.battery_optimization_title))
                .setMessage(getString(R.string.battery_optimization_message))
                .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                .show()
    }

    @OptionsItem(R.id.menu_export)
    open fun exportBtnClicked() {
        exportDialog = ProgressDialog.show(activity, getString(R.string.export_dialog_title), getString(R.string.export_dialog_text), true)
        ExportService_.intent(activity).prepareForExport().start()
    }

    @OptionsItem(R.id.menu_browse)
    open fun browseBtnClicked() {
        MonitoringListActivity_.intent(this).start()
    }

    @OptionsItem(R.id.menu_help)
    open fun helpBtnClicked() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(getString(R.string.help_url))
        startActivity(intent)
    }

    @OptionsItem(R.id.menu_information)
    open fun infoBtnClicked() {
        val density = resources.displayMetrics.density
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(getString(R.string.info_dialog_title))
        val view = TextView(activity)
        view.setPadding((10 * density).toInt(), (10 * density).toInt(), (10 * density).toInt(), (10 * density).toInt())
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        view.movementMethod = LinkMovementMethod.getInstance()
        view.text = Html.fromHtml(getString(R.string.info_text), ImageGetter { s ->
            var drawable: Drawable? = null
            when (s) {
                "logo_bspb" -> drawable = resources.getDrawable(R.drawable.logo_bspb)
                "logo_mtel" -> drawable = resources.getDrawable(R.drawable.logo_mtel)
                "life_NEW" -> drawable = resources.getDrawable(R.drawable.logo_life)
                "natura2000_NEW" -> drawable = resources.getDrawable(R.drawable.logo_natura_2000)
            }
            if (drawable == null) {
                Reporting.logException(IllegalArgumentException("Unknown image: $s"))
            } else {
                drawable.setBounds(0, 0, (drawable.intrinsicWidth * density).toInt(),
                        (drawable.intrinsicHeight * density).toInt())
            }
            drawable!!
        }, null)
        builder.setView(view)
        builder.create().show()
    }

    @OptionsItem(R.id.menu_statistics)
    open fun showStats() {
        StatsActivity_.intent(activity).start()
    }

    //    @LongClick(R.id.btn_export)
    open fun displayDescription(v: View): Boolean {
        if (TextUtils.isEmpty(v.contentDescription)) return false
        val screenPos = IntArray(2)
        val displayFrame = Rect()
        v.getLocationOnScreen(screenPos)
        v.getWindowVisibleDisplayFrame(displayFrame)
        val context = v.context
        val width = v.width
        val height = v.height
        val midy = screenPos[1] + height / 2
        var referenceX = screenPos[0] + width / 2
        if (ViewCompat.getLayoutDirection(v) == ViewCompat.LAYOUT_DIRECTION_LTR) {
            val screenWidth = context.resources.displayMetrics.widthPixels
            referenceX = screenWidth - referenceX // mirror
        }
        val cheatSheet = Toast.makeText(context, v.contentDescription, Toast.LENGTH_SHORT)
        if (midy < displayFrame.height()) {
            // Show along the top; follow action buttons
            cheatSheet.setGravity(Gravity.TOP or GravityCompat.END, referenceX, height)
        } else {
            // Show along the bottom center
            cheatSheet.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, height)
        }
        cheatSheet.show()
        return true
    }

    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context!!.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (pm.isIgnoringBatteryOptimizations(context!!.packageName)) {
                btnBatteryOptimization.visibility = View.GONE
            } else {
                if (!prefs.isBatteryOptimizationDialogShown.get()) {
                    showBatteryOptimizationDialog()
                    prefs.isBatteryOptimizationDialogShown.put(true)
                }
                btnBatteryOptimization.visibility = View.VISIBLE
            }
        } else {
            btnBatteryOptimization.visibility = View.GONE
        }
    }

    @Background
    open fun showNotSyncedCount() {
        val notSyncedCount: Int = monitoringManager.countMonitoringsForStatus(Monitoring.Status.finished)
        displayNotSyncedCount(notSyncedCount)
    }

    @UiThread
    open fun displayNotSyncedCount(notSyncedCount: Int) {
        try {
            btnUpload.text = "${getString(R.string.main_screen_btn_upload)} : $notSyncedCount"
        } catch (t: Throwable) {
            // IllegalStateException: not attached to Activity
            Reporting.logException(t)
        }
    }

    @UiThread
    open fun onEvent(event: StartingUpload?) {
        showProgressDialog(getString(R.string.upload_dialog_title), getString(R.string.upload_dialog_text))
    }

    @UiThread
    open fun onEvent(event: DownloadCompleted?) {
        if (!(UploadService.isUploading || ZoneService.isDownloading || NomenclatureService.isDownloading.isNotEmpty())) {
            hideProgressDialog()
        }
        showNotSyncedCount()
    }

    @UiThread
    open fun onEvent(event: StartingDownload?) {
        showProgressDialog(getString(R.string.download_dialog_title), getString(R.string.download_dialog_text))
    }

    @UiThread
    open fun onEvent(event: UploadCompleted?) {
        if (!(!NomenclatureService.isDownloading.isEmpty() || ZoneService.isDownloading)) {
            hideProgressDialog()
        }
        showNotSyncedCount()
    }

    @UiThread
    open fun onEvent(event: ExportPreparedEvent) {
        exportDialog?.cancel()
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/zip"
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.export_subject))
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.export_text, Date().toString()))
        intent.putExtra(Intent.EXTRA_STREAM, event.uri)
        startActivity(Intent.createChooser(intent, getString(R.string.export_app_chooser)))
    }

    @UiThread
    open fun onEvent(event: ExportFailedEvent?) {
        exportDialog?.cancel()
        Toast.makeText(activity, getString(R.string.export_failed_error), Toast.LENGTH_LONG).show()
    }

    @UiThread
    open fun onEvent(event: MonitoringPausedEvent?) {
        setupMonitoringButtons()
        bus.removeStickyEvent(MonitoringPausedEvent::class.java)
    }

    @UiThread
    open fun onEvent(event: MonitoringCanceledEvent?) {
        setupMonitoringButtons()
        bus.removeStickyEvent(MonitoringCanceledEvent::class.java)
    }

    @UiThread
    open fun onEvent(event: MonitoringFinishedEvent?) {
        setupMonitoringButtons()
        bus.removeStickyEvent(MonitoringFinishedEvent::class.java)
    }

    private fun permissionsGranted(): Boolean {
        return locationPermissionsGranted() && storagePermissionsGranted()
    }

    private fun locationPermissionsGranted(): Boolean {
        if (ContextCompat.checkSelfPermission(activity!!, permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(activity!!, permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false
        }
        if (shouldShowRequestPermissionRationale(permission.ACCESS_FINE_LOCATION)) {
            try {
                Snackbar.make(btnStartBirds, R.string.monitoring_permission_rationale, Snackbar.LENGTH_INDEFINITE)
                        .setAction(android.R.string.ok) { requestPermissions(arrayOf(permission.ACCESS_FINE_LOCATION, permission.ACCESS_COARSE_LOCATION), REQUEST_LOCATION) }.show()
                return false
            } catch (t: Throwable) {
                // we get IAE because we don't extend the Theme.AppCompat, but that messes up styling of the fields
                Reporting.logException(t)
            }
        }
        requestPermissions(arrayOf(permission.ACCESS_FINE_LOCATION, permission.ACCESS_COARSE_LOCATION), REQUEST_LOCATION)
        Toast.makeText(activity, R.string.monitoring_permission_rationale, Toast.LENGTH_SHORT).show()
        return false
    }

    private fun storagePermissionsGranted(): Boolean {
        if (ContextCompat.checkSelfPermission(activity!!, permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(activity!!, permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false
        }
        if (shouldShowRequestPermissionRationale(permission.WRITE_EXTERNAL_STORAGE)) {
            try {
                Snackbar.make(btnStartBirds, R.string.storage_permission_rationale, Snackbar.LENGTH_INDEFINITE)
                        .setAction(android.R.string.ok) { requestPermissions(arrayOf(permission.WRITE_EXTERNAL_STORAGE, permission.READ_EXTERNAL_STORAGE), REQUEST_STORAGE) }.show()
                return false
            } catch (t: Throwable) {
                // we get IAE because we don't extend the Theme.AppCompat, but that messes up styling of the fields
                Reporting.logException(t)
            }
        }
        requestPermissions(arrayOf(permission.WRITE_EXTERNAL_STORAGE, permission.READ_EXTERNAL_STORAGE), REQUEST_STORAGE)
        Toast.makeText(activity, R.string.storage_permission_rationale, Toast.LENGTH_SHORT).show()
        return false
    }

    private fun confirmCancel() {
        //Ask the user if they want to quit
        AlertDialog.Builder(activity)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.cancel_monitoring)
                .setMessage(R.string.really_cancel_monitoring)
                .setPositiveButton(android.R.string.yes) { _, _ -> doCancel() }
                .setNegativeButton(android.R.string.no, null)
                .show()
    }

    private fun doCancel() {
        monitoringPrefs.edit().clear().apply()
        activity!!.getSharedPreferences(SmartBirdsApplication.PREFS_MONITORING_POINTS, Context.MODE_PRIVATE).edit().clear().apply()
        bus.postSticky(CancelMonitoringEvent())
    }

    private fun showProgressDialog(title: String, message: String) {

        if (loading == null) {
            loading = fragmentManager!!.findFragmentByTag("progress") as LoadingDialog?
        }

        if (loading != null) {
            loading!!.updateTexts(message)
            return
        }


        val loadingDialog: LoadingDialog = LoadingDialog.newInstance(message)
        loadingDialog.show(fragmentManager!!, "progress")
        loading = loadingDialog
    }

    private fun hideProgressDialog() {
        loading?.dismiss()
        loading = null
    }


}