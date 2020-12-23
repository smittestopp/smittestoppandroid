package no.simula.corona.ui.register

import android.os.Bundle
import no.simula.corona.R
import no.simula.corona.ui.BaseActivity
import no.simula.corona.ui.onboarding.AgeVerificationFragment
import no.simula.corona.ui.onboarding.FragmentHost
import no.simula.corona.ui.onboarding.OnboardingPage

class AgeVerification : BaseActivity(), FragmentHost {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_age_verification)

        if (savedInstanceState == null) {
            val unused = OnboardingPage.MESSAGE_INFO
            val fragment = AgeVerificationFragment.newInstance(unused)

            supportFragmentManager.beginTransaction().add(R.id.container, fragment).commit()
        }
    }

    override fun onCallOfAction(a: OnboardingPage) {
        gotoRegister()
    }
}
