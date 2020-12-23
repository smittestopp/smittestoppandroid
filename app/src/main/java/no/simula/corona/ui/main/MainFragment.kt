package no.simula.corona.ui.main

import android.app.AlertDialog
import android.bluetooth.BluetoothManager
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.getColor
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import kotlinx.android.synthetic.main.fragment_main.*
import no.simula.corona.R
import no.simula.corona.Utils
import no.simula.corona.data.AgeVerifier
import no.simula.corona.ui.dialogs.AgeVerificationDialog
import timber.log.Timber


class MainFragment : ControlFragmentBase(), MainFragmentConnection {
    private val mVerifier = AgeVerifier()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_main, container, false)
        return root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        enableTracking.setOnClickListener {
            if (!location && !bluetooth) {
                callback?.gotoSettings()
            }
        }

        imageGps.setOnClickListener {
            //callback?.enableLocationUpdates(!location)
            if (!location) {
                callback?.gotoSettings()
            }
        }

        imageBluetooth.setOnClickListener {
            //callback?.enableBluetoothUpdates(!bluetooth)
            if (!bluetooth) {
                callback?.gotoSettings()
            }
        }

        buttonShare.setOnClickListener {
            sharePlainText(this.activity, getString(R.string.app_name))
        }

        buttonSettings.setOnClickListener {
            callback?.gotoSettings()
        }

        buttonRegisterAge.setOnClickListener {
            showRegisterAgeDialog()
        }

        init()
    }

    private fun isUpdating(): Boolean {
        return bluetooth || location
    }

    private fun init() {
        onServiceEnable(isUpdating())
        onFeature(callback?.isLocationWorking() == true, callback?.isBluetoothWorking() == true)

        setTextMessages()
    }

    override fun setUIState() {
        onServiceEnable(isUpdating())
        onFeature(location, bluetooth)

        setTextMessages()
    }

    override fun onServiceEnable(enabled: Boolean) {

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        callback?.onHomeFragmentAttach(this)
    }

    override fun onDetach() {
        super.onDetach()
        callback?.onHomeFragmentAttach(null)
    }

    override fun onFeature(gps: Boolean, bluetooth: Boolean) {

        onFeatureBT(bluetooth)
        onFeatureGPS(gps)
    }

    private fun onFeatureBT(bluetooth: Boolean) {
        try {

            val bluetoothManager =
                this.activity?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

            val enable = bluetooth && (bluetoothManager?.adapter?.isEnabled == true)

            imageBluetooth.setImageResource(
                if (!enable)
                    R.drawable.ic_bluetooth_off
                else
                    R.drawable.ic_bluetooth
            )

            imageBluetooth.contentDescription = if (enable)
                getText(R.string.bluetooth_on)
            else
                getText(R.string.bluetooth_off)

            imageBluetooth.isClickable = !enable

        } catch (ex: Exception) {
            Timber.e(ex)
        }
    }

    private fun onFeatureGPS(gps: Boolean) {

        try {
            imageGps.setImageResource(if (gps == false) R.drawable.ic_location_off else R.drawable.ic_location)
            imageGps.contentDescription =
                if (gps) getText(R.string.gps_on) else getText(R.string.gps_off)
            imageGps.isClickable = !gps
        } catch (ex: Exception) {
            Timber.e(ex)
        }
    }

    private fun setTextMessages() {
        try {

            buttonSettings.visibility = View.INVISIBLE
            buttonShare.visibility = View.INVISIBLE
            buttonRegisterAge.visibility = View.INVISIBLE
            textShare.visibility = View.INVISIBLE
            ageRequestWhy.visibility = View.GONE
            textShare.text = getString(R.string.share_app)
            enableTracking.setImageResource(R.drawable.ic_monitor_active)
            enableTracking.contentDescription = getString(R.string.desc_icon)
            enableTracking.isClickable = false

            if (bluetooth && location) {
                textViewMainStatus.text = getString(R.string.fully_active)
                textViewMainStatus.setTextColor(getColor(this.context!!, R.color.green))
                textViewTitle.visibility = View.VISIBLE
                textViewTitle.text = getString(R.string.thanks_for_help)
                textViewStatus.text = getString(R.string.message_when_activated)
                buttonShare.visibility = View.VISIBLE
                textShare.visibility = View.VISIBLE

                enableTracking.setImageResource(R.drawable.ic_monitor_fully_activated)
                enableTracking.isClickable = false
                enableTracking.contentDescription = getString(R.string.desc_icon)
            } else if (bluetooth && !location) {
                textViewMainStatus.text = getString(R.string.partially_activated)
                textViewMainStatus.setTextColor(getColor(this.context!!, R.color.redDark))
                textViewTitle.visibility = View.INVISIBLE
                textViewStatus.text = getString(R.string.gps_not_in_use)
                buttonSettings.visibility = View.VISIBLE

                enableTracking.setImageResource(R.drawable.ic_monitor_partially_activated)
                enableTracking.isClickable = true
                enableTracking.contentDescription = getString(R.string.go_to_settings)
            } else if (!bluetooth && location) {
                textViewMainStatus.text = getString(R.string.partially_activated)
                textViewMainStatus.setTextColor(getColor(this.context!!, R.color.redDark))
                textViewTitle.visibility = View.INVISIBLE
                textViewStatus.text = getString(R.string.bt_not_in_use)
                buttonSettings.visibility = View.VISIBLE

                enableTracking.setImageResource(R.drawable.ic_monitor_partially_activated)
                enableTracking.isClickable = true
                enableTracking.contentDescription = getString(R.string.go_to_settings)
            } else {
                textViewMainStatus.text = getString(R.string.deactivated)
                textViewMainStatus.setTextColor(getColor(this.context!!, R.color.redDark))
                textViewTitle.visibility = View.INVISIBLE
                textViewStatus.text = getString(R.string.gps_bt_not_in_use)
                buttonSettings.visibility = View.VISIBLE

                enableTracking.setImageResource(R.drawable.ic_monitor_deactivated)
                enableTracking.isClickable = true
                enableTracking.contentDescription = getString(R.string.go_to_settings)
            }

            checkIfAgeVerified()
        } catch (ex: Exception) {
            Timber.e(ex)
        }
    }

    private fun showRegisterAgeDialog() {
        Analytics.trackEvent(
            "Show age verification dialog",
            EventProperties().apply { set("where", "Main") })

        AgeVerificationDialog.create(context!!) { ageValid, txt ->
            if (ageValid) {
                Analytics.trackEvent(
                    "Age verified",
                    EventProperties().apply { set("where", "Main") })

                Utils.setDate(context!!, txt)
                showStatusToast(getString(R.string.age_ok), short = true)
                setTextMessages()
            } else {
                val error =
                    "${getString(R.string.selected_date)} $txt. ${getString(R.string.too_young)}"
                showErrorToast(error)
            }
        }.show()
    }

    private fun checkIfAgeVerified() {
        val age = Utils.getDate(context!!)
        if (age.isNotEmpty()) {
            buttonRegisterAge.visibility = View.INVISIBLE
            return
        }

        Analytics.trackEvent(
            "Show age verification",
            EventProperties().apply { set("where", "Main") })

        buttonSettings.visibility = View.INVISIBLE
        buttonShare.visibility = View.INVISIBLE
        buttonRegisterAge.visibility = View.VISIBLE

        textShare.visibility = View.VISIBLE
        textShare.text = getString(R.string.register_age_required)

        ageRequestWhy.visibility = View.VISIBLE
        ageRequestWhy.setOnClickListener {
            Analytics.trackEvent(
                "Show age verification reason",
                EventProperties().apply { set("where", "Main") })

            with(AlertDialog.Builder(context!!)) {
                setTitle(R.string.age_why_title)
                setMessage(R.string.age_why_description)
                setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                show()
            }
        }
    }
}