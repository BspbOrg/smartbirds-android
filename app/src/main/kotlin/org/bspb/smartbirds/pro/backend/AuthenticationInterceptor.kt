package org.bspb.smartbirds.pro.backend

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.util.Log
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.bspb.smartbirds.pro.BuildConfig
import org.bspb.smartbirds.pro.SmartBirdsApplication
import org.bspb.smartbirds.pro.backend.dto.LoginRequest
import org.bspb.smartbirds.pro.events.EEventBus
import org.bspb.smartbirds.pro.events.UserDataEvent
import org.bspb.smartbirds.pro.prefs.UserPrefs
import org.bspb.smartbirds.pro.sync.AuthenticationManager
import org.bspb.smartbirds.pro.tools.Reporting
import java.io.IOException

class AuthenticationInterceptor private constructor() : Interceptor {
    companion object {
        const val TAG: String = SmartBirdsApplication.TAG + ".AuthIntrcptr"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: AuthenticationInterceptor? = null

        fun getInstance(): AuthenticationInterceptor {
            checkNotNull(INSTANCE) { "AuthenticatorInterceptor instance is null. init() must be called before getting the instance." }
            return INSTANCE!!
        }

        fun init(context: Context) {
            if (INSTANCE != null) {
                return
            }

            synchronized(this) {
                INSTANCE = AuthenticationInterceptor()
                INSTANCE!!.context = context
                INSTANCE!!.prefs = UserPrefs(context)
                INSTANCE!!.authenticationManager = AuthenticationManager(context)
                INSTANCE!!.loadAuthorization()
                INSTANCE
            }
        }
    }

    private lateinit var context: Context
    private lateinit var prefs: UserPrefs
    private val backend: Backend by lazy { Backend.getInstance() }
    private val bus: EEventBus by lazy { EEventBus.getInstance() }
    private lateinit var authenticationManager: AuthenticationManager
    private var authorization: String? = null

    private fun loadAuthorization() {
        authorization = prefs.getAuthToken()
    }

    fun clearAuthorization() {
        authorization = null
        prefs.clear()
        prefs.setAuthenticated(false)
        SecureCookieStore.clear()
    }

    fun setAuthorization(authorization: String?, username: String?, password: String?) {
        this.authorization = authorization
        prefs.setAuthToken(authorization)
        prefs.setAuthenticated(!TextUtils.isEmpty(authorization))
        prefs.setUsername(username)
        prefs.setPassword(password)
    }

    @Synchronized
    fun tryRelogin(failedAuth: String?): Boolean {
        try {
            if (!TextUtils.equals(authorization, failedAuth)) return !TextUtils.isEmpty(
                authorization
            )
            val loginResponse =
                backend.api().login(LoginRequest(prefs.getUsername(), prefs.getPassword()))
                    .execute()
            if (loginResponse.isSuccessful) {
                authorization = loginResponse.body()!!.token
                prefs.setAuthToken(authorization)
                bus.postSticky(UserDataEvent(loginResponse.body()!!.user))
                return true
            } else {
                authorization = null
                return false
            }
        } catch (t: Throwable) {
            Reporting.logException(t)
            return false
        }
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!BuildConfig.BACKEND_BASE_URL.contains(chain.request().url.host) || chain.request()
                .url.encodedPath.contains("session")
        ) {
            Log.d(TAG, "no auth required")
            return chain.proceed(chain.request())
        }
        var retries = 0

        var response: Response? = null

        while (true) {
            val auth: String? = authorization
            if (TextUtils.isEmpty(auth)) {
                Log.d(TAG, "not authorized")
                return chain.proceed(chain.request())
            }
            val newRequest: Request =
                chain.request().newBuilder().addHeader("x-sb-csrf-token", auth!!).build()
            response?.close()
            response = chain.proceed(newRequest)
            if (!response.isSuccessful && response.code == 401) {
                Log.w(TAG, "Invalid authorization!")
                if (retries++ > 3 || !tryRelogin(auth)) {
                    clearAuthorization()
                    authenticationManager.logout()
                    return response
                }
            } else {
                return response
            }
        }
    }
}