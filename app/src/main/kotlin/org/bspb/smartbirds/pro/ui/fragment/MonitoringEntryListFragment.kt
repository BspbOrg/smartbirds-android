package org.bspb.smartbirds.pro.ui.fragment

import android.app.AlertDialog
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AbsListView
import android.widget.AbsListView.MultiChoiceModeListener
import android.widget.CursorAdapter
import android.widget.ListView
import androidx.fragment.app.ListFragment
import org.androidannotations.annotations.*
import org.bspb.smartbirds.pro.R
import org.bspb.smartbirds.pro.SmartBirdsApplication
import org.bspb.smartbirds.pro.adapter.ModelCursorAdapter
import org.bspb.smartbirds.pro.adapter.ModelCursorFactory
import org.bspb.smartbirds.pro.beans.MonitoringCursorEntries
import org.bspb.smartbirds.pro.content.Monitoring
import org.bspb.smartbirds.pro.content.MonitoringEntry
import org.bspb.smartbirds.pro.content.MonitoringManager
import org.bspb.smartbirds.pro.enums.EntryType
import org.bspb.smartbirds.pro.service.DataOpsService_
import org.bspb.smartbirds.pro.ui.partial.MonitoringEntryListRowPartialView
import org.bspb.smartbirds.pro.ui.partial.MonitoringEntryListRowPartialView_
import java.util.*

@EFragment
open class MonitoringEntryListFragment : ListFragment(), MonitoringCursorEntries.Listener {
    protected var adapter: CursorAdapter? = null

    @Bean
    protected lateinit var entries: MonitoringCursorEntries

    @Bean
    protected lateinit var monitoringManager: MonitoringManager

    protected var code: String? = null
    protected var monitoring: Monitoring? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        adapter = object : ModelCursorAdapter<MonitoringEntry?>(activity, R.layout.partial_monitoring_entry_list_row, if (entries != null) entries!!.cursor else null, ModelCursorFactory { cursor -> MonitoringManager.entryFromCursor(cursor) }) {
            override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
                return MonitoringEntryListRowPartialView_.build(context)
            }

            override fun bindView(view: View?, context: Context?, model: MonitoringEntry?) {
                require(view is MonitoringEntryListRowPartialView) { "Must use " + MonitoringEntryListRowPartialView::class.java.simpleName }
                view.bind(model)
            }
        }
        listAdapter = adapter
    }

    @AfterInject
    protected fun setupLoader() {
        code?.let {
            monitoring = monitoringManager.getMonitoring(it)
        }
        entries.setMonitoringCode(code)
        entries.setListener(this)
        if (adapter != null) adapter!!.swapCursor(entries.cursor)
    }

    @AfterViews
    protected fun setupListview() {
        val lv = listView
        setEmptyText(getText(R.string.emptyList))
        if (monitoring?.status == Monitoring.Status.uploaded) {
            return
        }

        lv.choiceMode = AbsListView.CHOICE_MODE_MULTIPLE_MODAL
        lv.setMultiChoiceModeListener(object : MultiChoiceModeListener {
            override fun onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long, checked: Boolean) {
                Log.d(TAG, String.format(Locale.ENGLISH, "onItemCheckedStateChanged: %d %s", position, checked))
            }

            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                Log.d(TAG, String.format(Locale.ENGLISH, "onCreateActionMode: %s", mode))
                activity?.menuInflater?.inflate(R.menu.listselection, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                return false
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                Log.d(TAG, String.format(Locale.ENGLISH, "onActionItemClicked: %s", item))
                when (item.itemId) {
                    R.id.action_select_all -> {
                        var i = 0
                        while (i < lv.count) {
                            lv.setItemChecked(i, true)
                            i++
                        }
                        return true
                    }
                    R.id.action_delete -> {
                        val selectedItems = lv.checkedItemIds
                        val builder = AlertDialog.Builder(activity)
                        builder.setMessage(getString(R.string.confirm_delete_n, selectedItems.size))
                        builder.setPositiveButton(android.R.string.ok) { dialog, which ->
                            mode.finish()
                            monitoringManager.deleteEntries(selectedItems)
                            DataOpsService_.intent(activity).generateMonitoringFiles(code).start()
                        }
                        builder.setNegativeButton(android.R.string.cancel, null)
                        val dialog = builder.create()
                        dialog.show()
                        return true
                    }
                }
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                Log.d(TAG, String.format(Locale.ENGLISH, "onDestroyActionMode: %s", mode))
            }
        })
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        super.onListItemClick(l, v, position, id)
        val entry = listAdapter?.getItem(position) as MonitoringEntry
        if (activity is Listener) {
            (activity as Listener).onMonitoringEntrySelected(entry.id, entry.type)
        }
    }

    @FragmentArg
    open fun setMonitoringCode(monitoringCode: String?) {
        this.code = monitoringCode
        this.code?.let {
            if (this::entries.isInitialized) {
                entries.setMonitoringCode(it)
            }
            if (this::monitoringManager.isInitialized) {
                monitoring = monitoringManager.getMonitoring(it)
            }
        }

    }

    override fun onMonitoringEntriesChanged(entries: MonitoringCursorEntries) {
        adapter?.run {
            swapCursor(entries.cursor)
        }
    }

    interface Listener {
        fun onMonitoringEntrySelected(id: Long, entryType: EntryType?)
    }

    companion object {
        private const val TAG = SmartBirdsApplication.TAG + ".FFormLst"
    }
}