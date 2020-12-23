package no.simula.corona.ui

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.toast_layout.view.*
import no.simula.corona.R

open class BaseFragment : Fragment(){


    fun showStatusToast(message: String, short: Boolean = false) {

        val toast = Toast(context)
        val inflater =
            context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.toast_layout, null)
        toast.setView(view);
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.duration = if (short) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
        view.textView.text = message

        toast.show()
    }

    fun showErrorToast(message: String) {
        val toast = Toast(context)
        val inflater =
            context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.toast_layout, null)
        toast.setView(view);
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.duration = Toast.LENGTH_LONG

        view.textView.text = message
        view.setBackgroundResource(R.drawable.background_filled_red_round)

        toast.show()
    }
}