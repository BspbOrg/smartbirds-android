package org.bspb.smartbirds.pro.ui.views

import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Filterable
import org.bspb.smartbirds.pro.R
import org.bspb.smartbirds.pro.SmartBirdsApplication
import org.bspb.smartbirds.pro.backend.dto.Nomenclature
import org.bspb.smartbirds.pro.ui.utils.Configuration
import org.bspb.smartbirds.pro.ui.utils.NomenclaturesBean
import org.bspb.smartbirds.pro.ui.utils.NomenclaturesBean_
import org.bspb.smartbirds.pro.ui.utils.SmartArrayAdapter
import org.bspb.smartbirds.pro.utils.showAlert

class NomenclatureChooser(
    private val context: Context,
    private val nomenclatureType: String,
    private val hint: String
) : TextWatcher {

    companion object {
        private val TAG =
            SmartBirdsApplication.TAG + "." + NomenclatureChooser::class.java.simpleName
    }

    private var needFilter: Boolean = false
    private var onItemSelected: ((item: NomenclatureItem?) -> Unit)? = null

    private var nomenclatures: NomenclaturesBean = NomenclaturesBean_.getInstance_(context)
    private var adapter: SmartArrayAdapter<NomenclatureItem> =
        SmartArrayAdapter(context, R.layout.item_dialog_single_choice, ArrayList())
    private var lastSelectedItem: NomenclatureItem? = null

    fun showPopup(onSelect: (nomenclatureItem: NomenclatureItem?) -> Unit) {
        if (!loadData()) {
            context.showAlert(
                R.string.nomenclatures_not_ready_title,
                R.string.nomenclatures_not_ready_message
            )
            return
        }

        onItemSelected = onSelect
        needFilter = adapter.count >= Configuration.ITEM_COUNT_FOR_FILTER

        var searchView: View? = null

        if (needFilter) {
            val inflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            searchView = inflater.inflate(R.layout.nomenclature_dialog_filter, null)
            val searchEdit = searchView.findViewById<EditText>(R.id.dialog_search_edit)
            searchEdit.hint = hint
            searchEdit.addTextChangedListener(this)
        }
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
            .setTitle(hint)
            .setSingleChoiceItems(
                adapter,
                AdapterView.INVALID_POSITION
            ) { dialog, which ->
                lastSelectedItem = adapter.getItem(which)
                onItemSelected?.invoke(adapter.getItem(which))
                dialog.dismiss()
            }
            .setCancelable(true)
            .setNegativeButton(android.R.string.cancel, null)
        if (needFilter) {
            builder.setCustomTitle(searchView)
        }

        var dialog: AlertDialog = builder.create()

        dialog.show()

        if (needFilter) {
            dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        }

        if (needFilter) dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
    }

    private fun loadData(): Boolean {
        if (nomenclatures.isLoading) {
            Log.d(TAG, "nomenclatures not loaded, waiting to load...")
            return false
        }
        val values: List<Nomenclature> = nomenclatures.getNomenclature(nomenclatureType)
        adapter.clear()
        for (value in values) {
            if (TextUtils.isEmpty(value.label.get(context.getString(R.string.locale)))) continue
            adapter.add(NomenclatureItem(value))
        }
        adapter.notifyDataSetChanged()
        if (adapter.count == 1) {
            onItemSelected?.invoke(adapter.getItem(0))
        }

        return true
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

    }

    override fun afterTextChanged(s: Editable?) {
        (adapter as Filterable).filter.filter(s)
    }
}