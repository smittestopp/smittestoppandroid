package no.simula.corona.ui.onboarding

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.android.volley.VolleyError
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import kotlinx.android.synthetic.main.activity_onboarding.*
import no.simula.corona.*
import no.simula.corona.heartbeat.Heartbeat

import no.simula.corona.ui.BaseActivity
import no.simula.corona.ui.dialogs.CoronaDialogInfo
import no.simula.corona.ui.register.PhoneVerificationActivity
import org.json.JSONObject
import timber.log.Timber

class OnboardingActivity : BaseActivity(), FragmentHost {


    companion object {
        const val TOTAL_PAGES = 5
        private val REQUEST_CONSENT = 101
        private val REQUEST_PHONE_VERIFICATION = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        pageIndicator.totalDots = TOTAL_PAGES
        setOnBoardingFragment(OnboardingPage.THANK_YOU)
    }

    override fun onCallOfAction(nextPage: OnboardingPage) {
        Timber.e("onCallOfAction $nextPage")

        when (nextPage) {
            OnboardingPage.PRIVACY_CONSENT -> gotoPrivacy()
            OnboardingPage.REGISTER -> startAuthentication()
            else -> setOnBoardingFragment(nextPage)
        }
    }

    private fun startAuthentication() {
        if (Utils.isNetworkAvailable(this)) {
            authenticate()
        } else {
            Toast.makeText(this, getString(R.string.internet_error), Toast.LENGTH_LONG).show()
        }
    }

    private fun gotoPrivacy() {
        val intent = Intent(this, ConsentActivity::class.java)
        startActivityForResult(intent, REQUEST_CONSENT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CONSENT -> {
                if (resultCode == Activity.RESULT_OK) {
                    Timber.i("Confirmed consent")
                    Analytics.trackEvent("Confirmed consent")
                    Utils.setConsent(this, true)
                    setOnBoardingFragment(OnboardingPage.AGE)
                } else {
                    Analytics.trackEvent("Denied consent")
                }
            }

            REQUEST_PHONE_VERIFICATION -> {
                if (resultCode == Activity.RESULT_OK) {
                    showSpinner()

                    val token = data?.getStringExtra(PhoneVerificationActivity.TOKEN)

                    IoTHubDevice.getInstance(this.application).performProvisioning(token!!, object :
                        IoTHubDevice.ProvisioningListener {
                        override fun onProvisionComplete(json: JSONObject?, saved: Boolean) {
                            Analytics.trackEvent("User provisioned")
                            Heartbeat.register(applicationContext)

                            if (isFinishing) {
                                return // activity is already killed
                            }

                            gotoMainScreen()
                        }

                        override fun onProvisionFailed(error: VolleyError) {

                            Analytics.trackEvent("User failed provisioning")

                            if (isFinishing) {
                                return // activity is already killed
                            }

                            var dlg = CoronaDialogInfo.newInstance(
                                R.string.app_name,
                                R.string.some_thing_went_wrong
                            )
                            dlg.listener = object : DialogInterface.OnClickListener {
                                override fun onClick(dialog: DialogInterface?, which: Int) {
                                    gotoMainScreen()
                                }
                            }

                            Crashes.trackError(error)
                            gotoMainScreen()
                        }
                    })
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    return
                } else {
                    gotoMainScreen()
                }
            }
        }
    }


    private fun authenticate() {
        val intent = Intent(this, PhoneVerificationActivity::class.java)
        startActivityForResult(intent, REQUEST_PHONE_VERIFICATION)
    }

    private fun showSpinner() {
        fragmentHost.visibility = View.GONE
        pageIndicator.visibility = View.GONE
        spinner.visibility = View.VISIBLE
    }

    private fun setOnBoardingFragment(page: OnboardingPage) {
        val fragment = OnboardingFragmentFactory.getFragment(this, page)
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragmentHost, fragment)
        fragmentTransaction.commit()

        pageIndicator.setSelected(page.pack())
    }

}
