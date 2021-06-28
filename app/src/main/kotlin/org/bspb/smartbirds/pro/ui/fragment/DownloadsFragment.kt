package org.bspb.smartbirds.pro.ui.fragment

import android.text.TextUtils
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.fragment_downloads.*
import org.androidannotations.annotations.AfterViews
import org.androidannotations.annotations.Bean
import org.androidannotations.annotations.EFragment
import org.androidannotations.annotations.sharedpreferences.Pref
import org.bspb.smartbirds.pro.R
import org.bspb.smartbirds.pro.adapter.DownloadsAdapter
import org.bspb.smartbirds.pro.backend.Backend
import org.bspb.smartbirds.pro.backend.dto.DownloadsItem
import org.bspb.smartbirds.pro.backend.dto.DownloadsResponse
import org.bspb.smartbirds.pro.prefs.DownloadsPrefs_
import org.bspb.smartbirds.pro.tools.SBGsonParser
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@EFragment(R.layout.fragment_downloads)
open class DownloadsFragment : Fragment() {

    @Bean
    protected lateinit var backend: Backend

    @Pref
    protected lateinit var prefs: DownloadsPrefs_

    private lateinit var adapter: DownloadsAdapter
    private lateinit var locale: String

    @AfterViews
    fun initViews() {
        locale = context?.getString(R.string.locale).toString()

        val layoutManager = LinearLayoutManager(context)
        emptyView.visibility = View.GONE
        downloadsListView.layoutManager = layoutManager

        adapter = DownloadsAdapter(locale)
        downloadsListView.adapter = adapter

        val dividerItemDecoration = DividerItemDecoration(context, layoutManager.orientation)
        downloadsListView.addItemDecoration(dividerItemDecoration)

        val cachedDownloads = prefs.downloads().get()
        var downloads: List<DownloadsItem>? = null
        if (!TextUtils.isEmpty(cachedDownloads)) {
            val listType = object : TypeToken<List<DownloadsItem?>?>() {}.type
            downloads =
                SBGsonParser.createParser().fromJson<List<DownloadsItem>>(cachedDownloads, listType)
        }

        fetchDownloads()

        listDownloads(downloads)
    }

    private fun listDownloads(downloads: List<DownloadsItem>?) {
        val availableDownloads = ArrayList<DownloadsItem>()

        downloads?.iterator()?.forEach {
            it.title?.apply {
                if (this.hasValue(locale)) {
                    availableDownloads.add(it)
                }
            }
        }

        if (availableDownloads.size > 0) {
            downloadsListView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        } else {
            downloadsListView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        }

        adapter.downloads = availableDownloads
    }

    private fun fetchDownloads() {
        progressBar.visibility = View.VISIBLE

        var call = backend.api().getDownloads(getString(R.string.downloads_url))
        call.enqueue(object : Callback<DownloadsResponse> {
            override fun onResponse(
                call: Call<DownloadsResponse>,
                response: Response<DownloadsResponse>
            ) {
                if (context == null) {
                    return
                }
                progressBar.visibility = View.GONE
                response.apply {
                    if (isSuccessful) {
                        body()?.downloads?.apply {
                            prefs.downloads().put(SBGsonParser.createParser().toJson(this))
                            listDownloads(this)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<DownloadsResponse>, t: Throwable) {
                if (context == null) {
                    return
                }
                progressBar.visibility = View.GONE
                t.printStackTrace()
            }
        })
    }

}