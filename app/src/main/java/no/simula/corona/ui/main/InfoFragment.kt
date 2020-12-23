package no.simula.corona.ui.main

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_info.*
import no.simula.corona.R

class InfoFragment : MainBaseFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_info, container, false)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setClickableLinks(view)
    }

    private fun setClickableLinks(view: View) {
        // set clickable links on info page
        fhi_website.movementMethod = LinkMovementMethod.getInstance()
        general_advice_public.movementMethod = LinkMovementMethod.getInstance()
        about_smittestopp.movementMethod = LinkMovementMethod.getInstance()
        self_reporting.movementMethod = LinkMovementMethod.getInstance()
    }
}