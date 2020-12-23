package no.simula.corona.ui.main

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.content.DialogInterface.BUTTON_NEGATIVE
import android.content.DialogInterface.BUTTON_POSITIVE
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import kotlinx.android.synthetic.main.fragment_settings.*
import no.simula.corona.*
import no.simula.corona.data.DatabaseProvider
import no.simula.corona.data.DatabaseUtils
import no.simula.corona.heartbeat.Heartbeat
import no.simula.corona.ui.about.AboutActivity
import no.simula.corona.ui.dialogs.CoronaDialog
import no.simula.corona.ui.register.PhoneVerificationActivity
import org.json.JSONObject
import timber.log.Timber


class SettingsFragment : ControlFragmentBase(), CompoundButton.OnCheckedChangeListener {
    companion object {
        const val deleted = "deleted"
    }

    private val REQUEST_BLUETOOTH_ON = 21
    private val PHONE_VERIFICATION_REQUEST = 123
    private lateinit var mQueue: RequestQueue

    private var secretClicks = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mQueue = Volley.newRequestQueue(context)
        val root = inflater.inflate(R.layout.fragment_settings, container, false)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setClickableLinks()

        var phoneNumber = getString(R.string.not_registered)
        if (context != null)
            phoneNumber = Utils.getPhoneNumber(context!!)

        if (phoneNumber.isEmpty()) {
            phone_number.text = getString(R.string.not_registered)
        } else {
            phone_number.text = phoneNumber
        }

        app_version.text = "v${BuildConfig.VERSION_NAME}"

        delete_data_button.setOnClickListener {
            deleteData()
        }

        logout_button.setOnClickListener { logOut() }

        secretClicks = 0
        account.setOnClickListener {
            if (context != null && secretClicks == 4) {
                val clipboard =
                    context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(null, Utils.getDeviceId(context!!))
                clipboard.setPrimaryClip(clip)

                Toast.makeText(context!!, "Copied device id", Toast.LENGTH_SHORT).show()

                secretClicks = 0
            } else {
                secretClicks += 1
            }
        }

        setUIState()

        privacy_policy_link.setOnClickListener {
            val intent = Intent(context, ConsentActivity::class.java)
            intent.putExtra(ConsentActivity.VIEW_MODE, true)

            startActivity(intent)
        }

        opensource_licenses.setOnClickListener {
            startActivity(Intent(this.activity, AboutActivity::class.java))
        }

    }

    private fun deleteData() {
        val dlg = CoronaDialog.newInstance(R.string.are_you_sure_delete, R.string.delete_data)

        dlg.listener = DialogInterface.OnClickListener { dialog, which ->
            when (which) {
                BUTTON_POSITIVE ->
                    deleteAction()
            }
        }

        dlg.show(childFragmentManager, "dlg_delete")


    }

    private fun deleteAction() {
        // swap button text with spinner
        Analytics.trackEvent("Start Delete Everything")
        delete_data_button.text = getString(R.string.deleting)
        val intent = Intent(activity, PhoneVerificationActivity::class.java)
        startActivityForResult(intent, PHONE_VERIFICATION_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PHONE_VERIFICATION_REQUEST -> {
                if (resultCode == Activity.RESULT_OK) {
                    val token = data?.getStringExtra(PhoneVerificationActivity.TOKEN)
                    if (token != null)
                        verificationSuccess(token)
                    else {
                        Analytics.trackEvent("Got null token")
                        verificationError()
                    }
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    delete_data_button.text = getString(R.string.settings_delete_data)
                    return
                } else {
                    Analytics.trackEvent("Get token error")
                    verificationError()
                }

                return
            }
            REQUEST_BLUETOOTH_ON -> {
                // we don't need to update the ui
                // if checkBluetooth changes the service state, the change
                // will be passed back here through the event bus
                checkBluetooth()
            }
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        when (buttonView?.id) {
            R.id.gpsSwitch -> {
                checkGps()
            }
            R.id.bluetoothSwith -> {
                checkBluetooth()
            }
        }
    }

    private fun checkGps() {
        if (!hasLocationPermission()) {
            /**
             * force the switch false since we do not have permissions yet
             *
             * enableLocationUpdates will trigger a permission dialog,
             * if the dialog is cancelled, no event will propagate back.
             *
             * If permissions are given and the service starts an event
             * will trigger a call to setUIState which will set the
             * switch to the correct state
             */
            gpsSwitch.isChecked = false
        }

        if (gpsLocationEnabled) {
            callback?.enableLocationUpdates(!location)
        } else if (!gpsLocationEnabled && !location) {
            // no gps and trying to turn on gps monitoring
            gpsSwitch.isChecked = false

            var toast = Toast.makeText(
                context!!,
                getString(R.string.turn_location_services),
                Toast.LENGTH_LONG
            )
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        }
    }

    private fun checkBluetooth() {
        if (bluetoothAdapterState) {
            callback?.enableBluetoothUpdates(!bluetooth)
        } else if (!bluetoothAdapterState && !bluetooth) {
            // no adapter and trying to turn on bluetooth
            bluetoothSwith.isChecked = false
            askToEnableBluetooth()
        }
    }

    private fun hasLocationPermission(): Boolean {
        if (context == null) {
            return false
        }

        return Utils.hasPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    override fun setUIState() {
        if (activity?.isFinishing == true) {
            return
        }

        bluetoothSwith.setOnCheckedChangeListener(null)
        gpsSwitch.setOnCheckedChangeListener(null)

        bluetoothSwith.isChecked = bluetooth
        gpsSwitch.isChecked = location

        bluetoothSwith.setOnCheckedChangeListener(this)
        gpsSwitch.setOnCheckedChangeListener(this)

    }

    private fun verificationSuccess(token: String) {
        val request = object : JsonObjectRequest(
            Method.POST, BuildConfig.DELETION_URL, null,
            Response.Listener<JSONObject> { response ->
                deleteSuccess(response)
            },
            Response.ErrorListener { error ->
                deleteError(error)
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = mutableMapOf<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }

        // 10 seconds timeout
        // do 0 retries
        // 0.0 backoff
        val policy = DefaultRetryPolicy(10000, 0, 0.0f)
        request.retryPolicy = policy
        mQueue.add(request)

        Timber.e("Verification OK -> Delete")
    }

    private fun verificationError() {
        Timber.e("Verification not OK -> No Delete")
        Toast.makeText(context, R.string.verification_error_try_later, Toast.LENGTH_LONG).show()
        delete_data_button.text = getString(R.string.settings_delete_data)
    }

    private fun deleteSuccess(response: JSONObject) {
        // delete local db
        // delete shared prefs
        // goto onboarding
        if (response.has("Status") && response.getString("Status") == "Success") {
            deleteEverything()
        } else {
            Timber.e(response.toString())
            Toast.makeText(context, R.string.delete_request_failed, Toast.LENGTH_LONG).show()
            delete_data_button.text = getString(R.string.settings_delete_data)
        }
    }

    private fun deleteError(e: VolleyError) {
        // swap button spinner with text
        // show some error message

        Timber.e(e)

        Crashes.trackError(
            e,
            mutableMapOf<String, String>().apply { set("where", "deleteEverythingError") },
            null
        )

        if (context == null || this.activity?.isFinishing == true) {
            return
        }

        Toast.makeText(context, R.string.delete_request_failed, Toast.LENGTH_LONG).show()
        delete_data_button.text = getString(R.string.settings_delete_data)
    }

    private fun deleteEverything() {
        if (context == null)
            return

        Heartbeat.unregister(activity!!.applicationContext)
        Utils.setConsent(context!!, false)

        callback?.enableLocationUpdates(false)
        callback?.enableBluetoothUpdates(false)

        deleteDataInDb()

        Utils.deleteLocalData(context!!)

        gotoSplash()
    }

    private fun logOut() {
        if (context == null)
            return

        val dialog = CoronaDialog.newInstance(R.string.log_out, R.string.are_sure_signout)

        dialog.listener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                BUTTON_POSITIVE ->
                    doLogout()
            }
        }

        dialog.show(childFragmentManager, "dialog_signout")
    }

    private fun doLogout() {
        callback?.enableLocationUpdates(false)
        callback?.enableBluetoothUpdates(false)

        deleteDataInDb()

        Heartbeat.unregister(activity!!.applicationContext)
        Utils.removeCredentials(context!!)


        gotoSplash()
    }

    private fun deleteDataInDb() {
        val database = DatabaseProvider.open(context!!)

        database.deleteEverythingGPS()
        database.deleteEverythingBluetooth()

        DatabaseUtils.info(database) //post-deletion

        database.close()
    }

    private fun gotoSplash() {
        val intent = Intent(activity, SplashActivity::class.java)
        intent.putExtra(SettingsFragment.deleted, true)
        startActivity(intent)
        activity?.finishAffinity()
    }

    private fun askToEnableBluetooth() {
        val dialog = CoronaDialog.newInstance(R.string.app_name, R.string.ask_turn_on_bluetooth)

        dialog.listener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                BUTTON_POSITIVE -> enableBluetooth()
                BUTTON_NEGATIVE -> showNoBluetoothToast()
            }
        }

        dialog.show(childFragmentManager, "dialog_bt_on")
    }

    private fun enableBluetooth() {
        startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_BLUETOOTH_ON)
    }

    private fun showNoBluetoothToast() {
        Toast.makeText(context!!, getString(R.string.turn_bluetooth), Toast.LENGTH_LONG)
            .apply {
                setGravity(Gravity.CENTER, 0, 0)
                show()
            }
    }

    private fun setClickableLinks() {
        // set clickable links on settings page
        support_email.movementMethod = LinkMovementMethod.getInstance()
    }
}