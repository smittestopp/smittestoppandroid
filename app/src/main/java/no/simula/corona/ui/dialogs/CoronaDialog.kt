package no.simula.corona.ui.dialogs

import android.R
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment


class CoronaDialog : DialogFragment() {

    var listener: DialogInterface.OnClickListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title: Int = getArguments()?.getInt("title")!!
        val message: Int = getArguments()?.getInt("message")!!

        return AlertDialog.Builder(getActivity())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.ok, listener)
            .setNegativeButton(R.string.cancel, listener)
            .create()
    }

    companion object {
        fun newInstance(title: Int, message: Int): CoronaDialog {
            val frag = CoronaDialog()
            val args = Bundle()
            args.putInt("title", title)
            args.putInt("message", message)
            frag.setArguments(args)
            return frag
        }
    }

}