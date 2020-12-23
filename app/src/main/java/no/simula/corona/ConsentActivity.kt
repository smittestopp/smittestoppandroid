package no.simula.corona

import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import kotlinx.android.synthetic.main.activity_consent.*
import java.util.*


/**
 * This class implements @see[AppCompatActivity] and handles the behaviour for the consent request.
 * The user is presented with the app's privacy policy and has to either accept or decline it.
 */
class ConsentActivity : AppCompatActivity() {
    companion object {
        const val VIEW_MODE = "view-mode"
    }

    private var mIsViewMode = false

    /**
     * From @see[AppCompatActivity]. Sets the activity's view and adds listeners for the accept and
     * decline buttons.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consent)

        privacy_policy.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                progressBar.visibility = View.GONE
                actionsLayout.visibility = View.VISIBLE
            }

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                super.shouldOverrideUrlLoading(view, url)
                view!!.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                return true
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                super.shouldOverrideUrlLoading(view, request)
                view!!.context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(request?.url.toString())
                    )
                )
                return true
            }
        }
        privacy_policy.setBackgroundColor(Color.parseColor("#F1EFEA"))
        //setting text color

        privacy_policy.loadUrl(if (Utils.isNorwegianLanguageDefault()) "file:///android_asset/consent.html" else "file:///android_asset/consent-eng.html")

        mIsViewMode = intent.getBooleanExtra(VIEW_MODE, false)
        if (mIsViewMode) {
            confirmConsent.visibility = View.GONE
            denyConsent.text = getString(R.string.close)
        }

        confirmConsent.setOnClickListener { setConfirmConsent() }
        denyConsent.setOnClickListener { setDenyConsent() }
    }

    /**
     * The user has accepted the privacy policy. Set the activity's result code to RESULT_OK and
     * finish the activity.
     */
    private fun setConfirmConsent() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    /**
     * The user has declined the privacy policy. Finish the activity.
     */
    private fun setDenyConsent() {
        finish()
    }
}
