package org.bspb.smartbirds.pro.sync

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import org.bspb.smartbirds.pro.SmartBirdsApplication
import org.bspb.smartbirds.pro.backend.AuthenticationInterceptor
import org.bspb.smartbirds.pro.backend.Backend
import org.bspb.smartbirds.pro.backend.LoginResultEvent
import org.bspb.smartbirds.pro.backend.dto.CheckSessionRequest
import org.bspb.smartbirds.pro.backend.dto.LoginRequest
import org.bspb.smartbirds.pro.backend.dto.LoginResponse
import org.bspb.smartbirds.pro.events.EEventBus
import org.bspb.smartbirds.pro.events.LoginStateEvent
import org.bspb.smartbirds.pro.events.LogoutEvent
import org.bspb.smartbirds.pro.events.UserDataEvent
import org.bspb.smartbirds.pro.service.SyncService
import org.bspb.smartbirds.pro.tools.Reporting
import org.bspb.smartbirds.pro.tools.SBGsonParser
import org.bspb.smartbirds.pro.utils.SBScope
import java.io.IOException


open class AuthenticationManager(private var context: Context) {

    companion object {
        private const val TAG = SmartBirdsApplication.TAG + ".AuthenticationSvc"
        var isDownloading = false
    }

    protected val backend: Backend by lazy { Backend.getInstance() }
    protected val bus: EEventBus by lazy { EEventBus.getInstance() }
    protected var scope = SBScope()

    open fun logout() {
        scope.sbLaunch(Dispatchers.IO) {
            AuthenticationInterceptor.getInstance().clearAuthorization()
            bus.removeStickyEvent(LoginResultEvent::class.java)
            bus.postSticky(LogoutEvent())
        }
    }

    open fun login(email: String?, password: String?, gdprConsent: Boolean?) {
        scope.sbLaunch(Dispatchers.IO) {
            bus.postSticky(LoginStateEvent(true))
            try {
                val result = doLogin(email, password, gdprConsent)
                bus.postSticky(result)
                bus.postSticky(UserDataEvent(result.user))
            } finally {
                bus.postSticky(LoginStateEvent(false))
            }
        }
    }

    private fun doLogin(
        email: String?,
        password: String?,
        gdprConsent: Boolean?
    ): LoginResultEvent {
        val response = try {
            backend.api().login(LoginRequest(email, password, gdprConsent)).execute()
        } catch (e: IOException) {
            Reporting.logException(e)
            return LoginResultEvent(LoginResultEvent.Status.CONNECTIVITY)
        }

        if (response == null) {
            return LoginResultEvent(LoginResultEvent.Status.ERROR)
        }

        if (!response.isSuccessful) {
            var errorResponse = ""
            try {
                errorResponse = response.errorBody()!!.string()
                Log.i(TAG, String.format("Login error %d: %s", response.code(), errorResponse))
            } catch (e: IOException) {
                Reporting.logException(e)
            }
            val loginResponse =
                SBGsonParser.createParser().fromJson(errorResponse, LoginResponse::class.java)
            var errorMessage: String? = null
            if (loginResponse != null) {
                errorMessage = loginResponse.error
            }
            return when (response.code()) {
                Backend.HTTP_STATUS_BAD_REQUEST -> LoginResultEvent(
                    LoginResultEvent.Status.ERROR,
                    errorMessage
                )

                Backend.HTTP_STATUS_UNAUTHORIZED -> {
                    if (loginResponse != null && loginResponse.token == null) {
                        if (LoginResponse.REQUIRE_GDPR == loginResponse.require) {
                            return LoginResultEvent(
                                LoginResultEvent.Status.MISSING_GDPR,
                                loginResponse.error
                            )
                        }
                    }
                    LoginResultEvent(LoginResultEvent.Status.BAD_PASSWORD)
                }

                else -> LoginResultEvent(LoginResultEvent.Status.CONNECTIVITY)
            }
        }

        AuthenticationInterceptor.getInstance()
            .setAuthorization(response.body()!!.token, email, password)

        context.startService(SyncService.initialSyncIntent(context))

        return LoginResultEvent(response.body()!!.user)
    }

    @WorkerThread
    fun checkSession() {
        try {
            isDownloading = true
            val response = backend.api().checkSession(CheckSessionRequest()).execute()
            if (response.isSuccessful) {
                bus.postSticky(UserDataEvent(response.body()?.user))
            }
        } catch (t: Throwable) {
            Reporting.logException(t)
        } finally {
            isDownloading = false
        }
    }
}