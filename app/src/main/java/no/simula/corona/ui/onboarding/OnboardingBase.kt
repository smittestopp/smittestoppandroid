package no.simula.corona.ui.onboarding

import android.content.Context
import no.simula.corona.ui.BaseFragment

open class OnboardingBase: BaseFragment() {
    private var mHost: FragmentHost? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if(context is FragmentHost){
            mHost = context
        }
    }

    protected fun continueOnboarding(nextPage: OnboardingPage) {
        mHost?.onCallOfAction(nextPage)
    }
}