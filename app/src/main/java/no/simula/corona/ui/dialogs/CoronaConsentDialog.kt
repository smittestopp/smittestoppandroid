package no.simula.corona.ui.dialogs

import android.R
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment


class CoronaConsentDialog : DialogFragment() {

    var listener: DialogInterface.OnClickListener? = null

    override fun  onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title: Int = getArguments()?.getInt("title")!!
        val message: Int = getArguments()?.getInt("message")!!

        return AlertDialog.Builder(getActivity())
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(R.string.yes, listener)
            .setNegativeButton(R.string.no, listener)
            .create()
    }

    companion object {
        fun newInstance(title: Int, message:Int): CoronaConsentDialog {
            val frag = CoronaConsentDialog()
            val args = Bundle()
            args.putInt("title", title)
            args.putInt("message", message)
            frag.setArguments(args)
            return frag
        }
    }

}