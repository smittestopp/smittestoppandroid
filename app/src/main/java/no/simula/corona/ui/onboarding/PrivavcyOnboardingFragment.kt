package no.simula.corona.ui.onboarding

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_onboarding_privavcy.*
import no.simula.corona.R
import no.simula.corona.ui.BaseFragment

class PrivavcyOnboardingFragment : OnboardingBase() {

    companion object {
        val EXTRA_MAIN_TEXT = "mainText"
        val EXTRA_PAGE_NO = "pageNo"
        val BUTTON_TEXT_RES_ID = "buttonTextRId"


        fun newInstance(
            nextPage: OnboardingPage, title: String,
            buttonText: String
        ): PrivavcyOnboardingFragment {
            val fragment = PrivavcyOnboardingFragment()
            val args = Bundle()
            args.putString(EXTRA_MAIN_TEXT, title)
            args.putInt(EXTRA_PAGE_NO, nextPage.pack())
            args.putString(BUTTON_TEXT_RES_ID, buttonText)
            fragment.arguments = args
            return fragment
        }
    }

    var mNextPage: OnboardingPage = OnboardingPage.THANK_YOU

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_onboarding_privavcy, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var mainText = arguments?.getString(EXTRA_MAIN_TEXT)
        var buttonText = arguments?.getString(BUTTON_TEXT_RES_ID)

        mNextPage = OnboardingPage.unpack(arguments?.getInt(EXTRA_PAGE_NO))

        this.textViewWelcome.text = mainText


        this.buttonCallOfAction.setText(buttonText)

        handleClicks()

    }

    private fun handleClicks() {
        this.buttonCallOfAction.setOnClickListener {
            continueOnboarding(mNextPage)
        }
    }
}