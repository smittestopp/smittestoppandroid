package no.simula.corona.ui.main

import android.content.Context
import android.content.Intent
import no.simula.corona.BuildConfig
import no.simula.corona.R
import no.simula.corona.ui.BaseFragment

open class MainBaseFragment : BaseFragment() {


    var callback: MainFragmentHost? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is MainFragmentHost) {
            callback = context
        }
    }


    open fun sharePlainText(
        context: Context?,
        title: String?
    ) {
        if (context == null) {
            return
        }
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"

        var content = getString(R.string.app_share_link)

        sharingIntent.putExtra(Intent.EXTRA_TEXT, content)
        if (sharingIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(Intent.createChooser(sharingIntent, title))
        }
    }
}