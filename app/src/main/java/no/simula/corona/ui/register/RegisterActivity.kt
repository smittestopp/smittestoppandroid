package no.simula.corona.ui.register

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentTransaction
import com.android.volley.VolleyError
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import kotlinx.android.synthetic.main.activity_register.*
import no.simula.corona.*
import no.simula.corona.heartbeat.Heartbeat
import no.simula.corona.ui.BaseActivity
import no.simula.corona.ui.onboarding.FragmentHost
import no.simula.corona.ui.onboarding.OnboardingFragment
import no.simula.corona.ui.onboarding.OnboardingPage
import org.json.JSONObject

class RegisterActivity : BaseActivity(),
    FragmentHost {

    companion object {
        private val REQUEST_PHONE_VERIFICATION = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)


        if (savedInstanceState == null) {
            val unused = OnboardingPage.THANK_YOU

            val fragment =
                OnboardingFragment.newInstance(
                    unused,
                    getString(R.string.title4),
                    R.string.onboarding4,
                    R.drawable.ic_onboarding_mobile_sms,
                    getString(R.string.register)
                )

            val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
            ft.add(R.id.container, fragment).commit()
        }
    }

    override fun onCallOfAction(page: OnboardingPage) {
        authenticate()
    }

    private fun authenticate() {
        val intent = Intent(this, PhoneVerificationActivity::class.java)
        startActivityForResult(
            intent,
            REQUEST_PHONE_VERIFICATION
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_PHONE_VERIFICATION -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        val token = data?.getStringExtra(PhoneVerificationActivity.TOKEN)
                        provision(token)
                    }
                    Activity.RESULT_CANCELED -> {
                        return
                    }
                    else -> {
                        gotoMainScreen()
                    }
                }
            }
        }
    }

    private fun provision(token: String?) {
        if (token == null) {
            return
        }

        showSpinner()

        IoTHubDevice.getInstance(this.application).performProvisioning(token, object :
            IoTHubDevice.ProvisioningListener {
            override fun onProvisionComplete(json: JSONObject?, saved: Boolean) {
                Heartbeat.register(applicationContext)
                Analytics.trackEvent("User provisioned")
                gotoMainScreen()
            }

            override fun onProvisionFailed(error: VolleyError) {
                Crashes.trackError(
                    error,
                    mutableMapOf<String, String>().apply {
                        set("where", "PostPhoneVerProvision")
                    }, null
                )

                gotoMainScreen()
            }
        })
    }

    private fun showSpinner() {
        container.visibility = View.GONE
        spinner.visibility = View.VISIBLE
    }
}