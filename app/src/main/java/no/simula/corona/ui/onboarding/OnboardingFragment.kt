package no.simula.corona.ui.onboarding

import android.content.Context
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_onboarding.*
import kotlinx.android.synthetic.main.fragment_onboarding.divider0
import no.simula.corona.R
import no.simula.corona.ui.BaseFragment

class OnboardingFragment : OnboardingBase() {

    companion object {
        val EXTRA_MAIN_TEXT = "mainText"
        val EXTRA_SUB_TEXT = "subText"
        val IMAGE_RES_ID = "imageResId"
        val EXTRA_PAGE_NO = "pageNo"
        val BUTTON_TEXT_RES_ID = "buttonTextRId"

        fun newInstance(
            nextPage: OnboardingPage, title: String,
            subtext: Int, imageUrl: Int,
            buttonText: String
        ): OnboardingFragment {
            val fragment = OnboardingFragment()
            val args = Bundle()
            args.putString(EXTRA_MAIN_TEXT, title)
            args.putInt(EXTRA_PAGE_NO, nextPage.pack())

            args.putInt(EXTRA_SUB_TEXT, subtext)
            args.putString(BUTTON_TEXT_RES_ID, buttonText)
            args.putInt(IMAGE_RES_ID, imageUrl)

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
        return inflater.inflate(R.layout.fragment_onboarding, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setClickableLinks(view)

        val mainText = arguments?.getString(EXTRA_MAIN_TEXT)
        val subText = arguments?.getInt(EXTRA_SUB_TEXT)
        val imageRes = arguments?.getInt(IMAGE_RES_ID)
        val buttonText = arguments?.getString(BUTTON_TEXT_RES_ID)

        mNextPage = OnboardingPage.unpack(arguments?.getInt(EXTRA_PAGE_NO))

        this.textViewWelcome.text = mainText
        this.textViewWelcomeDetails.text = if (subText != null) getText(subText) else " "

        this.divider0.setImageResource(imageRes!!)

        this.buttonCallOfAction.text = buttonText

        handleClicks()

    }

    private fun handleClicks() {
        this.buttonCallOfAction.setOnClickListener {
            continueOnboarding(mNextPage)
        }
    }

    private fun setClickableLinks(view: View) {
        // set clickable links on settings page
        textViewWelcomeDetails.movementMethod = LinkMovementMethod.getInstance()
    }
}