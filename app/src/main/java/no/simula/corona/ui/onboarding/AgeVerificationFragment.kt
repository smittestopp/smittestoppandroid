package no.simula.corona.ui.onboarding

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import kotlinx.android.synthetic.main.fragment_age_verification.*
import no.simula.corona.R
import no.simula.corona.Utils
import no.simula.corona.ui.dialogs.AgeVerificationDialog


private const val PAGE = "param1"

class AgeVerificationFragment : OnboardingBase() {
    companion object {
        /**
         *
         * @param page Parameter 1.
         * @return A new instance of fragment AgeVerificationFragment.
         */
        @JvmStatic
        fun newInstance(nextPage: OnboardingPage) =
            AgeVerificationFragment().apply {
                arguments = Bundle().apply {
                    putInt(PAGE, nextPage.pack())
                }
            }
    }

    private var mNextPage = OnboardingPage.THANK_YOU
    private var mAgeDialog: AgeVerificationDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mNextPage = OnboardingPage.unpack(it.getInt(PAGE))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_age_verification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Analytics.trackEvent(
            "Show age verification",
            EventProperties().apply { set("where", "Onboarding") })

        textViewWelcome.requestFocus(View.FOCUS_DOWN)

        buttonCallOfAction.setOnClickListener {
            llMain.post {
                llMain.fullScroll(View.FOCUS_DOWN)
                focusAgePicker()
            }
        }

        age_picker.setOnClickListener {
            if (mAgeDialog == null) {
                mAgeDialog = AgeVerificationDialog.create(context!!) { ageValid, dob ->
                    age_picker?.text = dob
                    if (ageValid) {
                        Utils.setDate(context!!, dob)
                        setOldEnough()
                    } else {
                        setNotOldEnough(dob)
                    }

                }
            }
            mAgeDialog?.show()
        }

        why_collect.setOnClickListener {
            Analytics.trackEvent(
                "Show age verification reason",
                EventProperties().apply { set("where", "Onboarding") })

            with(AlertDialog.Builder(context!!)) {
                setTitle(R.string.age_why_title)
                setMessage(R.string.age_why_description)
                setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                show()
            }
        }
    }

    private fun focusAgePicker() {
        age_picker.performClick()
    }

    private fun setOldEnough() {
        Analytics.trackEvent(
            "Age verified",
            EventProperties().apply { set("where", "Onboarding") })

        showStatusToast(getString(R.string.age_ok), short = true)
        continueOnboarding(mNextPage)
    }

    private fun setNotOldEnough(date: String) {
        val error = "${getString(R.string.selected_date)} $date. ${getString(R.string.too_young)}"
        showErrorToast(error)
    }
}
