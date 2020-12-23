package no.simula.corona.ui.register

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import com.microsoft.appcenter.crashes.Crashes
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import no.simula.corona.BuildConfig
import no.simula.corona.R
import no.simula.corona.Utils
import timber.log.Timber


class PhoneVerificationActivity : AppCompatActivity(),
    IPublicClientApplication.ApplicationCreatedListener {

    companion object {
        const val TOKEN = "token"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_verification)
        Analytics.trackEvent("Verify Phone Number")

        setResult(Activity.RESULT_CANCELED)
        SingleAccountPublicClientApplication.create(
            applicationContext,
            BuildConfig.CLIENT_ID,
            BuildConfig.AUTHORITY,
            BuildConfig.REDIRECT_URI,
            this
        )
    }

    override fun onCreated(application: IPublicClientApplication?) {
        val scope = arrayOf(BuildConfig.SCOPES)
        Analytics.trackEvent("Start Acquire Token")

        var longLivingApp = this@PhoneVerificationActivity.application //

        application?.acquireToken(this, scope, object : AuthenticationCallback {
            override fun onSuccess(result: IAuthenticationResult?) {
                Analytics.trackEvent("Acquired token")
                if (result != null) {
                    Utils.storePhoneNumber(
                        longLivingApp,
                        result.accessToken
                    ) // You never know if activity context is valid after async task returns so use appcontext
                    val data = Intent()
                    data.putExtra(TOKEN, result.accessToken)

                    Utils.saveToken(longLivingApp, result.accessToken)

                    setResult(Activity.RESULT_OK, data)
                }
                finish()
                overridePendingTransition(0, 0)
            }

            override fun onCancel() {
                Analytics.trackEvent("Acquire Token Cancelled")

                setResult(Activity.RESULT_CANCELED)
                finish()
                overridePendingTransition(0, 0)
            }

            override fun onError(exception: MsalException?) {
                Timber.d(exception.toString())
                val props =
                    mutableMapOf<String, String>().apply { put("where", "PhoneVerAcquireToken") }
                Crashes.trackError(exception, props, null)

                finish()
                overridePendingTransition(0, 0)
            }
        })
    }

    override fun onError(exception: MsalException?) {
        Timber.d(exception.toString())
        Crashes.trackError(
            exception,
            mutableMapOf<String, String>().apply { set("where", "createMsAuthAppError") },
            null
        )
        finish()
        overridePendingTransition(0, 0)
    }
}
