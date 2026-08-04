package org.bspb.smartbirds.pro.backend

import android.util.Log
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.bspb.smartbirds.pro.BuildConfig
import org.bspb.smartbirds.pro.SmartBirdsApplication
import org.bspb.smartbirds.pro.tools.SBGsonParser
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieHandler
import java.net.CookieManager
import java.util.LinkedList
import java.util.concurrent.TimeUnit

class Backend private constructor() {
    companion object {
        val INSTANCE: Backend? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { Backend() }

        fun getInstance(): Backend {
            return INSTANCE!!
        }

        const val TAG: String = SmartBirdsApplication.TAG + ".Backend"

        const val HTTP_STATUS_BAD_REQUEST: Int = 400
        const val HTTP_STATUS_UNAUTHORIZED: Int = 401
        const val HTTP_STATUS_FORBIDDEN: Int = 403
        const val HTTP_STATUS_NOT_FOUND: Int = 404

        var backendBaseUrl: String = BuildConfig.BACKEND_BASE_URL
    }

    private var interceptors: List<Interceptor> = LinkedList<Interceptor>()
    private var networkInterceptors: List<Interceptor> = LinkedList<Interceptor>()

    private var client: OkHttpClient? = null
    private var retrofit: Retrofit? = null
    private var api: SmartBirdsApi? = null
    private val gson: Gson by lazy { SBGsonParser.createParser() }

    init {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE)
        addNetworkInterceptor(logging)
    }

    fun addInterceptor(interceptor: Interceptor?) {
        interceptor?.let { (interceptors as? MutableList)?.add(it) }
    }

    private fun addNetworkInterceptor(interceptor: Interceptor?) {
        interceptor?.let { (networkInterceptors as? MutableList)?.add(it) }
    }

    private fun getClient(): OkHttpClient {
        if (client == null) {
            // Add the interceptor to OkHttpClient
            val builder = OkHttpClient.Builder()
            builder.interceptors().addAll(interceptors)
            builder.networkInterceptors().addAll(networkInterceptors)
            builder.readTimeout(5, TimeUnit.MINUTES)
            builder.cookieJar(JavaNetCookieJar(if (CookieHandler.getDefault() != null) CookieHandler.getDefault() else CookieManager()))
            client = builder.build()
            // replace with okhttp instances to throw unmodifiable exception
            interceptors = client!!.interceptors
            networkInterceptors = client!!.networkInterceptors
        }
        return client!!
    }

    private fun getRetrofit(): Retrofit {
        if (retrofit == null) {
            Log.d(TAG, String.format("Backend base url: %s", backendBaseUrl))
            retrofit = Retrofit.Builder()
                .baseUrl(backendBaseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(getClient())
                .build()
        }
        return retrofit!!
    }

    fun api(): SmartBirdsApi {
        return if (api == null) getRetrofit().create(SmartBirdsApi::class.java).also {
            api = it
        } else api!!
    }
}