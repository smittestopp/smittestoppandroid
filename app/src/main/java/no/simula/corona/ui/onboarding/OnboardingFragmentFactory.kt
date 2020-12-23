package no.simula.corona.ui.onboarding

import android.content.Context
import androidx.fragment.app.Fragment
import no.simula.corona.R
import timber.log.Timber


enum class OnboardingPage {
    THANK_YOU,
    ABOUT,
    PRIVACY_OVERVIEW,
    PRIVACY_CONSENT,
    AGE,
    MESSAGE_INFO,
    REGISTER;

    fun pack(): Int {
        return ordinal
    }

    companion object {
        fun unpack(page: Int?): OnboardingPage {
            val item = page ?: 0
            val pages = values()
            if (item < 0 || item >= pages.size) {
                Timber.w("No onboarding page for ordinal $item")
                return THANK_YOU
            }

            return pages[item]
        }
    }
}

object OnboardingFragmentFactory {
    fun getFragment(context: Context, position: OnboardingPage) : Fragment {
        return when (position) {
            OnboardingPage.THANK_YOU -> {
                OnboardingFragment.newInstance(
                    OnboardingPage.ABOUT,
                    context.getString(R.string.title1),
                    R.string.onboarding1,
                    R.drawable.ic_onboarding_team,
                    context.getString(R.string.next)
                )
            }
            OnboardingPage.ABOUT -> {
                OnboardingFragment.newInstance(
                    OnboardingPage.PRIVACY_OVERVIEW,
                    context.getString(R.string.title2),
                    R.string.onboarding2,
                    R.drawable.ic_onboarding_track_virus,
                    context.getString(R.string.next)
                )
            }
            OnboardingPage.PRIVACY_OVERVIEW -> {
                PrivavcyOnboardingFragment.newInstance(
                    OnboardingPage.PRIVACY_CONSENT,
                    context.getString(R.string.title3),
                    context.getString(R.string.read_privavcy)
                )
            }
            OnboardingPage.AGE -> {
                AgeVerificationFragment.newInstance(OnboardingPage.MESSAGE_INFO)
            }
            OnboardingPage.MESSAGE_INFO -> {
                OnboardingFragment.newInstance(
                    OnboardingPage.REGISTER,
                    context.getString(R.string.title4),
                    R.string.onboarding4,
                    R.drawable.ic_onboarding_mobile_sms,
                    context.getString(R.string.register)
                )
            }
            OnboardingPage.REGISTER -> {
                Timber.e("RegisterActivity cannot be started as an OnboardingFragment")
                getFragment(context, OnboardingPage.MESSAGE_INFO)
            }
            OnboardingPage.PRIVACY_CONSENT -> {
                Timber.e("ConsentActivity cannot be started as an OnboardingFragment")
                getFragment(context, OnboardingPage.PRIVACY_OVERVIEW)
            }
        }
    }
}