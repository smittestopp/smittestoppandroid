package no.simula.corona.ui.dialogs

import android.app.AlertDialog
import android.content.Context
import android.view.View
import kotlinx.android.synthetic.main.dialog_date_spinner.view.*
import kotlinx.android.synthetic.main.fragment_age_verification.*
import no.simula.corona.R
import no.simula.corona.Utils
import no.simula.corona.data.AgeVerifier
import timber.log.Timber
import java.lang.Exception

class AgeVerificationDialog private constructor(
    private val context: Context,
    private val onSelected: (Boolean, String) -> Unit
) {
    companion object {
        fun create(context: Context, onSelected: (Boolean, String) -> Unit): AgeVerificationDialog {
            return AgeVerificationDialog(context, onSelected)
        }
    }

    private val mVerifier = AgeVerifier()


    fun show() {
        val view = View.inflate(context, R.layout.dialog_date_spinner, null)

        if (mVerifier.hasSelection()) {
            view.picker.init(mVerifier.year(), mVerifier.month(), mVerifier.day(), null)
        }

        with(AlertDialog.Builder(context)) {
            setTitle(R.string.age_dialog_title)
            setView(view)
            setPositiveButton("OK") { _, _ ->
                with(view.picker) {
                    try {
                        dateSelected(dayOfMonth, month, year)
                    } catch (ex: Exception) {
                        Timber.e(ex)
                    }
                }
            }
            setNegativeButton(R.string.close) { dialog, _ -> dialog.dismiss() }
            show()
        }
    }

    private fun dateSelected(day: Int, month: Int, year: Int) {
        mVerifier.select(year, month, day)
        onSelected(mVerifier.isOldEnough(), mVerifier.formatDate())
    }
}