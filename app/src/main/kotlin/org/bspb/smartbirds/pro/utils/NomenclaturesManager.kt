package org.bspb.smartbirds.pro.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers
import org.bspb.smartbirds.pro.R
import org.bspb.smartbirds.pro.backend.Backend
import org.bspb.smartbirds.pro.backend.dto.Nomenclature
import org.bspb.smartbirds.pro.db.SmartBirdsDatabase
import org.bspb.smartbirds.pro.db.model.NomenclatureModel
import org.bspb.smartbirds.pro.db.model.NomenclatureUsesCount
import org.bspb.smartbirds.pro.events.EEventBus
import org.bspb.smartbirds.pro.events.NomenclaturesReadyEvent
import org.bspb.smartbirds.pro.tools.AlphanumComparator
import org.bspb.smartbirds.pro.tools.Reporting
import org.bspb.smartbirds.pro.tools.SBGsonParser
import java.io.IOException
import java.util.Collections

class NomenclaturesManager private constructor(val context: Context) {

    enum class Downloading {
        LOCATIONS, NOMENCLATURES
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: NomenclaturesManager? = null

        var isDownloading: MutableSet<Downloading> = HashSet()

        fun getInstance(): NomenclaturesManager {
            checkNotNull(INSTANCE) { "NomenclaturesManager instance is null. init(Context context) must be called before getting the instance." }
            return INSTANCE!!
        }

        fun init(context: Context) {
            if (INSTANCE != null) {
                return
            }

            synchronized(this) {
                INSTANCE = NomenclaturesManager(context)
                INSTANCE!!.loadNomenclatures()
                INSTANCE
            }
        }
    }

    private val data = mutableMapOf<String, MutableList<Nomenclature>>()
    private val locale = context.getString(R.string.locale)
    private var loading = false
    private val db = SmartBirdsDatabase.getInstance()
    private val backend: Backend = Backend.getInstance()
    private val scope: SBScope by lazy { SBScope() }

    private val comparator: Comparator<in Nomenclature> =
        Comparator { o1: Nomenclature, o2: Nomenclature ->
            if (o1.label.labelId.equals(o2.label.labelId, ignoreCase = true)) {
                return@Comparator 0
            }
            AlphanumComparator.compareStrings(o1.label.get(locale), o2.label.get(locale))
        }

    @VisibleForTesting
    fun loadNomenclatures() {
        scope.sbLaunch(Dispatchers.IO) {
            loading = true
            data.clear()
            val dbNomenclatures = db.nomenclatureDao().getAll()
            dbNomenclatures.forEach { nomenclature ->
                nomenclature.data?.let { nomenclatureData ->
                    val list: MutableList<Nomenclature>?
                    if (!data.containsKey(nomenclature.type)) {
                        list = mutableListOf()
                        data[nomenclature.type!!] = list
                    } else {
                        list = data[nomenclature.type]
                    }
                    list!!.add(Nomenclature.fromData(nomenclatureData, locale))
                }
            }

            // sort nomenclatures
            for (nomenclatures in data.values) {
                Collections.sort(nomenclatures, comparator)
            }

            loading = false
            EEventBus.getInstance().postSticky(NomenclaturesReadyEvent())
        }
    }

    fun getNomenclature(key: String): List<Nomenclature> {
        val formattedKey = key.replaceFirst("^form_".toRegex(), "")
        if (!data.containsKey(formattedKey)) {
            check(!loading) { "Still loading" }
            throw IllegalArgumentException("Unknown nomenclature $formattedKey")
        }
        return data[formattedKey]!!
    }

    fun isLoading() = loading

    fun getRecentNomenclatures(key: String): LiveData<List<Nomenclature>> {
        return liveData {
//            emit(listOf())

            var type = key.replaceFirst("^form_".toRegex(), "")
            val nomenclatures: List<Nomenclature> = getNomenclature(type)
            val recentItems = mutableListOf<Nomenclature>()

            val dbNomenclatures =
                db.nomenclatureUsesCountDao().findByType(type)
            var idx: Int
            dbNomenclatures.forEach { nomenclature ->
                nomenclature.data?.let { data ->
                    val temp = Nomenclature.fromData(data, locale)
                    idx = Collections.binarySearch(nomenclatures, temp, comparator)
                    if (idx >= 0) {
                        recentItems.add(nomenclatures[idx])
                    }
                }
            }
            emit(recentItems)
        }
    }

    fun addRecentNomenclature(nomenclature: Nomenclature) {
        scope.sbLaunch(Dispatchers.IO) {
            var recentNomenclature =
                db.nomenclatureUsesCountDao().findByLabel(nomenclature.label.labelId)
            if (recentNomenclature == null) {
                recentNomenclature = NomenclatureUsesCount(
                    0,
                    nomenclature.type,
                    nomenclature.label.labelId,
                    1,
                    SBGsonParser.createParser().toJson(nomenclature)
                )
                db.nomenclatureUsesCountDao().insert(recentNomenclature)
            } else {
                recentNomenclature.count = recentNomenclature.count?.plus(1)
                db.nomenclatureUsesCountDao().update(recentNomenclature)
            }
        }

    }

    suspend fun updateNomenclatures() {
        isDownloading.add(Downloading.NOMENCLATURES)
        try {
            try {
                var limit = 500
                var offset = 0
                val nomenclatures = mutableListOf<NomenclatureModel>()

                while (true) {
                    val response = backend.api().nomenclatures(limit, offset).execute()
                    if (!response.isSuccessful) throw IOException("Server error: " + response.code() + " - " + response.message())
                    if (response.body()!!.data.isEmpty()) break
                    offset += response.body()!!.data.size
                    response.body()!!.data.forEach {
                        nomenclatures.add(it.convertToEntity())
                    }
                }

                if (nomenclatures.isNotEmpty()) {
                    limit = 500
                    offset = 0
                    while (true) {
                        val response = backend.api().species(limit, offset).execute()
                        if (!response.isSuccessful) throw IOException("Server error: " + response.code() + " - " + response.message())
                        if (response.body()!!.data.isEmpty()) break
                        offset += response.body()!!.data.size
                        response.body()!!.data.forEach {
                            nomenclatures.add(it.convertSpeciesToEntity(context))
                        }
                    }
                    SmartBirdsDatabase.getInstance().nomenclatureDao()
                        .updateNomenclaturesAndClearOld(nomenclatures)
                }

                limit = 500
                offset = 0
                while (true) {
                    val response = backend.api().pois(limit, offset).execute()
                    if (!response.isSuccessful) throw IOException("Server error: " + response.code() + " - " + response.message())
                    if (response.body()!!.data.isEmpty()) break
                    offset += response.body()!!.data.size
                    response.body()!!.data.forEach {
                        nomenclatures.add(it.convertToEntity())
                    }
                }
                SmartBirdsDatabase.getInstance().nomenclatureDao()
                    .updateNomenclaturesAndClearOld(nomenclatures)

                loadNomenclatures()
            } catch (t: Throwable) {
                Reporting.logException(t)
                showToast("Could not download nomenclatures. Try again.")
            }
        } finally {
            isDownloading.remove(Downloading.NOMENCLATURES)
        }
    }

    private fun showToast(message: String?) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                context.applicationContext,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

}