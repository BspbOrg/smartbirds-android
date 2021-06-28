package org.bspb.smartbirds.pro.utils

import android.content.Context
import android.content.DialogInterface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog

fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)
}

fun debugLog(message: String?) {
    Log.d("++++++", message)
}

fun Context.showAlert(
    title: Int,
    message: Int,
    positiveButtonClickListener: DialogInterface.OnClickListener? = null,
    negativeButtonClickListener: DialogInterface.OnClickListener? = null,
) {
    showAlert(
        getString(title),
        getString(message),
        positiveButtonClickListener,
        negativeButtonClickListener
    )
}


fun Context.showAlert(
    title: CharSequence,
    message: CharSequence,
    positiveButtonClickListener: DialogInterface.OnClickListener? = null,
    negativeButtonClickListener: DialogInterface.OnClickListener? = null,
) {
    val builder = AlertDialog.Builder(this)
    builder.setTitle(title)
    builder.setMessage(message)
    builder.setPositiveButton(android.R.string.ok, positiveButtonClickListener)
    builder.setNegativeButton(android.R.string.cancel, negativeButtonClickListener)
    builder.show()
}