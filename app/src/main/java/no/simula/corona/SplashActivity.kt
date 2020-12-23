package no.simula.corona

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import com.android.volley.VolleyError
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import com.microsoft.appcenter.crashes.Crashes
import no.simula.corona.heartbeat.Heartbeat
import no.simula.corona.security.Device
import no.simula.corona.ui.BaseActivity
import no.simula.corona.ui.onboarding.OnboardingActivity
import no.simula.corona.ui.register.PhoneVerificationActivity
import no.simula.corona.notification.AppNotificationManager
import org.json.JSONObject
import timber.log.Timber

class SplashActivity : BaseActivity() {
    companion object {
        const val DELETED_REMOTE = "remote-delete"
    }

    private val ERROR_CODE_EMULATOR = 1
    private val ERROR_CODE_ROOTED = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        if (Device.isEmulator()) {
            showDeviceErrorAndExit(ERROR_CODE_EMULATOR /*error code emulator*/)
            return
        }

        if (Device.isRooted(this)) {
            showDeviceErrorAndExit(ERROR_CODE_ROOTED /*error code rooted device*/)
            return
        }

        AppNotificationManager(this).cancelAuthNotificationIfAny()

        if (intent.getBooleanExtra(DELETED_REMOTE, false)) {
            showDeletedDescription()
            return
        }

        // no leak issue -> references are released after 1sec
        val messageQueue: Handler = @SuppressLint("HandlerLeak")
        object : Handler() {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                if (!isFinishing) {
                    start()
                }
            }
        }

        val wait = if (Utils.isFirstland(this)) 2000L else 1000L
        messageQueue.sendEmptyMessageDelayed(0, wait)

        Heartbeat.unregister(this)
        Heartbeat.register(this)
    }

    private fun showDeletedDescription() {
        AlertDialog.Builder(this).apply {
            setTitle(R.string.app_name)
            setMessage(R.string.data_deleted_request)
            setPositiveButton(android.R.string.ok) { _, _ -> start() }
            setOnDismissListener { start() }
            create()
        }.show()
    }

    private fun showDeviceErrorAndExit(errorCode :Int) {
        AlertDialog.Builder(this).apply {
            setTitle(R.string.app_name)
            setMessage("This app cannot run on this system. Error code: $errorCode")
            setPositiveButton(R.string.ok) { _, _ -> finishAffinity()}
            setOnDismissListener { finishAffinity()}
            create()
        }.show()
    }

    private fun start() {
        if (Utils.hasGivenConsent(this)) {
            if (Utils.isAuthenticated(this)) {
                gotoMainScreen()
            } else if (Utils.getDate(this).isEmpty()) {
                gotoAgeVerification()
            } else {
                authenticate()
            }
        } else {
            startOnboarding()
        }
    }

    private fun startOnboarding() {
        val intent = Intent(this, OnboardingActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun authenticate() {
        if (Utils.needReAuthentication(this)) {
            val b = AlertDialog.Builder(this).apply {
                setTitle(R.string.app_name)
                setMessage(R.string.provision_failed)
                setPositiveButton(R.string.ok) { _, _ -> gotoRegister() }
                setOnDismissListener { gotoRegister() }
                create()
            }.show()
        } else {
            gotoRegister()
        }
    }
}


