package org.bspb.smartbirds.pro.ui.fragment

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.reflect.TypeToken
import org.bspb.smartbirds.pro.R
import org.bspb.smartbirds.pro.adapter.DownloadsAdapter
import org.bspb.smartbirds.pro.backend.Backend
import org.bspb.smartbirds.pro.backend.dto.DownloadsItem
import org.bspb.smartbirds.pro.backend.dto.DownloadsResponse
import org.bspb.smartbirds.pro.databinding.FragmentDownloadsBinding
import org.bspb.smartbirds.pro.prefs.DownloadsPrefs
import org.bspb.smartbirds.pro.tools.SBGsonParser
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DownloadsFragment : Fragment() {

    private val backend: Backend by lazy { Backend.getInstance() }
    private lateinit var prefs: DownloadsPrefs
    private lateinit var adapter: DownloadsAdapter
    private lateinit var locale: String

    private lateinit var binding: FragmentDownloadsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        initPrefs()
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState) ?: inflater.inflate(
            R.layout.fragment_downloads,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initPrefs() {
        prefs = DownloadsPrefs(requireContext())
    }

    private fun initViews() {
        locale = context?.getString(R.string.locale).toString()
        binding = FragmentDownloadsBinding.bind(requireView())

        val layoutManager = LinearLayoutManager(context)
        binding.emptyView.visibility = View.GONE
        binding.downloadsListView.layoutManager = layoutManager

        adapter = DownloadsAdapter(locale)
        binding.downloadsListView.adapter = adapter

        val dividerItemDecoration = DividerItemDecoration(context, layoutManager.orientation)
        binding.downloadsListView.addItemDecoration(dividerItemDecoration)

        val cachedDownloads = prefs.getDownloads()
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


        downloads?.forEach {
            it.title?.apply {
                if (this.hasValue(locale)) {
                    availableDownloads.add(it)
                }
            }
        }

        if (availableDownloads.size > 0) {
            binding.downloadsListView.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
        } else {
            binding.downloadsListView.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
        }

        adapter.downloads = availableDownloads
    }

    private fun fetchDownloads() {
        binding.progressBar.visibility = View.VISIBLE

        val call = backend.api().getDownloads(getString(R.string.downloads_url))
        call.enqueue(object : Callback<DownloadsResponse> {
            override fun onResponse(
                call: Call<DownloadsResponse>,
                response: Response<DownloadsResponse>
            ) {
                if (context == null) {
                    return
                }
                binding.progressBar.visibility = View.GONE
                response.apply {
                    if (isSuccessful) {
                        body()?.downloads?.apply {
                            prefs.setDownloads(SBGsonParser.createParser().toJson(this))
                            listDownloads(this)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<DownloadsResponse>, t: Throwable) {
                if (context == null) {
                    return
                }
                binding.progressBar.visibility = View.GONE
                t.printStackTrace()
            }
        })
    }

}