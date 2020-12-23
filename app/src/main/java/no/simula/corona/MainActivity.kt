package no.simula.corona

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.microsoft.appcenter.analytics.Analytics
import kotlinx.android.synthetic.main.activity_main.*
import no.simula.corona.events.BluetoothEvent
import no.simula.corona.events.GPSEvent
import no.simula.corona.ui.main.MainFragmentHost
import no.simula.corona.ui.main.MainFragmentConnection
import no.simula.corona.ui.register.RegisterActivity
import org.greenrobot.eventbus.EventBus
import timber.log.Timber

const val TEST_AGE_VERIFICATION = false


class MainActivity : AppCompatActivity(), MainFragmentHost, ServiceListener {
    private var mService: DataCollectorService? = null
    private var mainFragmentConnection: MainFragmentConnection? = null

    /**
     * Interface for monitoring the state of an application service.
     */
    private val mConnection = object : ServiceConnection {

        /**
         * Called when a connection to the Service has been established, with the IBinder of the
         * communication channel to the Service.
         *
         * @param[name] The concrete component name of the service that has been connected.
         * @param[binder] The IBinder of the Service's communication channel, which you can now make
         * calls on.
         */
        override fun onServiceConnected(
            name: ComponentName?,
            binder: IBinder?
        ) {
            if (binder != null) {
                val b = binder as CollectorBinder
                mService = b.getService()
                mService?.serviceListener = this@MainActivity
                autoStartCheck()
            }
            setButtonState() // Set bluetooth/GPS button states
        }

        /**
         * Called when a connection to the Service has been lost. This typically happens when the
         * process hosting the service has crashed or been killed. This does not remove the
         * ServiceConnection itself -- this binding to the service will remain active, and you will
         * receive a call to onServiceConnected(ComponentName, IBinder) when the Service is next
         * running.
         *
         * @param[name] The concrete component name of the service that has been connected.
         */
        override fun onServiceDisconnected(name: ComponentName?) {
            mService?.serviceListener = null
            mService = null
            setButtonState() // Set bluetooth/GPS button states
        }

        /**
         * Checks if DataCollectorService should be automatically started
         */
        private fun autoStartCheck() {
            // if MainActivity is about to end to nothing
            if (this@MainActivity.isFinishing) {
                return
            }
            autoStart()
        }
    }

    /**
     * From AppCompatActivity. Perform initialization.
     *
     * @param[savedInstanceState] Bundle containing the saved instance state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // set layout

        // From AppCompatActivity. Set a Toolbar to act as the ActionBar for this activity
        // window. Here, the toolbar is defined in @see[activity_main.xml]
        setSupportActionBar(toolbar)

        // View for bottom navigation
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        // Controller for main screen content
        val navController = findNavController(R.id.nav_host_fragment)

        // Set of Views that are activated by navigation buttons
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_settings, // Settings page
                R.id.navigation_home, // Monitoring page
                R.id.navigation_info // Info page
            )
        )

        // Link navController to appBarConfiguration
        setupActionBarWithNavController(navController, appBarConfiguration)
        // link navView to navController
        navView.setupWithNavController(navController)
        // navGraph is a collection of NavDestination nodes fetchable by ID.
        val navGraph = navController.navInflater.inflate(R.navigation.main_nav)
        // Set initial destination for navGraph to monitoring page
        navGraph.startDestination = R.id.navigation_home

        Timber.e("My device id is: %s", Utils.getProvisionDeviceId(this))

        if (BuildConfig.DEBUG && TEST_AGE_VERIFICATION) {
            Utils.deleteDate(this)
        }
    }

    /**
     * Change main screen content to Settings page.
     */
    override fun gotoSettings() {
        val navController = findNavController(R.id.nav_host_fragment)
        navController.navigate(R.id.navigation_settings)
    }

    /**
     * From AppCompatActivity. Dispatch onStart() to all fragments.
     */
    override fun onStart() {
        super.onStart()
        // bind @see[DataCollectorService] to mConnection
        bindService(
            Intent(this, DataCollectorService::class.java), mConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    /**
     * From AppCompatActivity. Dispatch onStop() to all fragments.
     */
    override fun onStop() {
        if (mService != null) {
            // unbind @see[DataCollectorService] from mConnection
            unbindService(mConnection)
        }
        super.onStop()
    }

    /**
     * From AppCompatActivity. Callback for the result from requesting permissions. This method is
     * invoked for every call on requestPermissions(android.app.Activity, String[], int).Handle
     * permission requests. Smittestopp explictly requests permission only for location (GPS) on
     * app start.
     *
     * @param[requestCode] Request code passed in requestPermissions(android.app.Activity, String[],
     *                     int) that identifies type of permission
     * @param[permissions] The requested permissions.
     * @param[grantResults] The grant results for the corresponding permissions which is either
     *                      PERMISSION_GRANTED or PERMISSION_DENIED.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            // request Code is equal to FINE_LOCATION_PERMISSION
            Utils.FINE_LOCATION_PERMISSION -> {
                val props = mutableMapOf<String, String>()
                // map permissions and grant results to props
                when (permissions.size) {
                    2 -> {
                        props["permissions"] = "${permissions[0]}, ${permissions[1]}"
                        props["granted"] = "${grantResults[0]}, ${grantResults[1]}"
                    }
                    1 -> {
                        props["permissions"] = permissions[0]
                        props["granted"] = "${grantResults[0]}}"
                    }
                    else -> {
                        props["permissions"] = "none"
                    }
                }

                // If props is not empty, permissions have been granted. Track these in Appcenter
                // Analytics
                if (props.isNotEmpty()) {
                    Analytics.trackEvent("Permission Response", props)
                }

                // Fine location is a required permission for the app
                val required = permissions.indexOf(Manifest.permission.ACCESS_FINE_LOCATION)

                // If required permission has been granted, start post permission granted routine
                if (required >= 0 && grantResults[required] == PackageManager.PERMISSION_GRANTED) {
                    autoStartPostPermissionGrant() // post permission granted routine
                }
                // The next time trying to start the tracker don't immediately request permissions
                else {
                    Utils.setDontAskForLocation(this, true)
                }
            }
            // Received anything other than a FINE_LOCATION_PERMISSION request
            else -> {
                // Ignore it.
            }
        }
    }


    /**
     * From AppCompatActivity. Requests permissions to be granted to this application. When we
     * don't have the requested permissions the user will be presented with UI for accepting them.
     * After the user has accepted or rejected the requested permissions a callback reporting
     * whether the permissions were granted or not is created and the results of permission
     * requests are delivered to its onRequestPermissionsResult(int, String[], int[]) method.
     *
     * Requesting a permission does not guarantee it will be granted.
     */
    private fun requestPermissions() {
        // If Preference dontAskAgain has been set in Utils
        if (Utils.dontAskForLocationPermission(this)) {
            // If running M (API Level 23) or above we can ask for permissions again
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Provide the user with a UI that explains the rationale of requesting access for
                // location
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    // Ask a second time
                    showRationaleDialog(askAgain = true) {
                        Utils.setDontAskForLocation(this, false)
                        requestPermissions()
                    }

                    return
                }
            }

            // We have already asked for permissions twice (if >= M (Lvl 23)) and user has denied
            // permissions. Don't ask again and go to settings.
            showRationaleDialog(askAgain = false) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }
            return
        }

        // If running Q (API Level 29) or above we need to add background location permission that
        // let's the user keep track of background location access
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Utils.requestPermission(
                this, arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ),
                Utils.FINE_LOCATION_PERMISSION
            )
        }
        // If running another version we just need to request fine location permissions
        else {
            Utils.requestPermission(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                Utils.FINE_LOCATION_PERMISSION
            )
        }
    }

    /**
     * UI rationale dialog presented to the user when asking for permission for the second time.
     *
     * @param[askAgain] True if asking the user for the second time, false otherwise
     * @param[positiveCallback] Callback for permission request
     */
    private fun showRationaleDialog(askAgain: Boolean, positiveCallback: () -> Unit) {
        Timber.i("Show rationale")

        // If we are asking for permission for a second time, show ask_permission string,
        // otherwise direct user to settings
        val positiveText = if (askAgain) {
            R.string.ask_permission
        } else {
            R.string.open_settings
        }

        // If we are asking for permission for a second time, show need_location_message string,
        // otherwise show need_location_message_settings
        val description = if (askAgain) {
            R.string.need_location_message
        } else {
            R.string.need_location_message_settings
        }

        // Create rationale dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.need_location_permission)
            .setMessage(description)
            .setPositiveButton(positiveText) { _, _ -> positiveCallback() }
            .setNegativeButton(R.string.dont_ask_permission) { _, _ ->
                // we have asked for a second time, so we can't ask again
                Utils.setDontAskForLocation(
                    this,
                    true
                )
            }
            .create()
        dialog.show()
    }

    /**
     * From MainFragmentHost. Checks if location permissions are in place and if user enabled
     * location tracking. If so, calls startLocationUpdates in @see[DataCollectorService].
     */
    override fun enableLocationUpdates(enable: Boolean) {
        // check if user enabled location
        Utils.setDidUserStartGPSService(this, enable)

        // check if permissions are in place
        if (checkPermissionsAndUserConsent()) {
            if (enable) {
                DataCollectorService.startLocationUpdates(this)
            } else {
                DataCollectorService.stopLocationUpdates((this))
            }
        }
    }

    /**
     * From MainFragmentHost. Checks if Bluetooth permissions are in place and if user enabled
     * Bluetooth. If so, calls startLocationUpdates in @see[DataCollectorService].
     */
    override fun enableBluetoothUpdates(enable: Boolean) {
        // check if user enables Bluetooth
        Utils.setDidUserStartBTService(this, enable)

        // check if permissions are in place
        if (checkPermissionsAndUserConsent()) {
            if (enable) {
                DataCollectorService.startBluetoothUpdates(this)
            } else {
                DataCollectorService.stopBluetoothUpdates(this)
            }
        }
    }

    /**
     * Check if user has given required permissions and consent
     */
    private fun checkPermissionsAndUserConsent(): Boolean {
        // Check if mService is initiated
        if (mService == null) {
            Timber.e("Service is null, cannot toggle state")
            return false
        }

        // User has not given consent and mService is running location and/or Bluetooth
        if (!Utils.hasGivenConsent(this) && mService?.isUpdating() == false) {
            // Cannot start without consent
            // Can stop if the consent is withdrawn after starting it
            Toast.makeText(
                this,
                "Cannot start location monitoring before consenting",
                Toast.LENGTH_LONG
            ).show()
            return false
        }

        // User has not been authenticated yet
        if (!Utils.isAuthenticated(this)) {
            registerFirst() // request authentication by user
            return false
        }

        // mService is running location and/or Bluetooth and permissions have not been granted
        if (mService?.isUpdating() == false &&
            !Utils.hasPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        ) {
            requestPermissions() // request required permissions
            return false
        }

        return true
    }

    /**
     * Start RegistrationActivity.
     */
    private fun registerFirst() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    /**
     * Set location and Bluetooth buttons according to enabled/disabled features.
     */
    private fun setButtonState() {
        if (mService != null) {
            // Set location button state according to location status in mService
            EventBus.getDefault().post(GPSEvent(mService!!.isLocationUpdating()))
            // Set Bluetooth button state according to Bluetooth status in mService
            EventBus.getDefault().post(BluetoothEvent(mService!!.isBluetoothUpdating()))
        }
    }

    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    /**
     * From MainFragmentHost. Check if service is running.
     */
    override fun isServiceRunning(): Boolean? {
        return mService?.isUpdating()
    }

    /**
     * From MainFragmentHost. Attach @see[MainFragmentConnection] to this activity.
     *
     * @param[mainFragment]
     */
    override fun onHomeFragmentAttach(mainFragment: MainFragmentConnection?) {
        mainFragmentConnection = mainFragment
    }

    /**
     * Check if location is updating in mService.
     */
    override fun isLocationWorking(): Boolean? {
        return mService?.isLocationUpdating()
    }

    /**
     * Check if Bluetooth is updating in mService.
     */
    override fun isBluetoothWorking(): Boolean? {
        return mService?.isBluetoothUpdating()
    }

    /**
     * Autostart services if user has previously had services running. These preferences are stored
     * in @see[Utils.Preferences].
     */
    private fun autoStart() {
        val isFirstLandingOnScreen = Utils.isFirstland(this)

        // If the user lands on main screen for the first time request permissions
        if (isFirstLandingOnScreen) {
            Utils.markFirstLand(this)
            requestPermissions()
            return
        }

        // User has given consent, but mService is not active
        if (Utils.hasGivenConsent(this@MainActivity) && mService?.isUpdating() == false) {
            // Permissions have been granted
            if (Utils.hasPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // User has previously had Bluetooth running
                if (Utils.didUserStartBluetooth(this@MainActivity)) {
                    // Start Bluetooth updates
                    DataCollectorService.startBluetoothUpdates(this@MainActivity)
                }
                // User has previously had location running
                if (Utils.didUserStartGPS(this@MainActivity)) {
                    // Start location updates
                    DataCollectorService.startLocationUpdates(this@MainActivity)
                }
            }
        }
    }

    /**
     * Autostart services after permissions have been granted
     */
    private fun autoStartPostPermissionGrant() {
        autoStart() // autostart servies
    }

    private fun gotoSplash() {
        if (isFinishing) {
            return
        }

        val intent = Intent(this, SplashActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    /**
     * Data was deleted. Force user to restart
     */
    override fun onIotDeviceDeleted() {

        runOnUiThread {
            if (isFinishing == false) {

                AlertDialog.Builder(this).apply {
                    setTitle(R.string.app_name)
                    setMessage(R.string.data_deleted_request)
                    setPositiveButton(android.R.string.ok) { _, _ -> gotoSplash() }
                    setOnDismissListener { gotoSplash() }
                    create()
                }.show()
            }
        }
    }

    override fun onBluetoothUpdateStarted() {}
    override fun onBluetoothUpdateStopped() {}
    override fun onLocationUpdateStarted() {}
    override fun onLocationUpdateStopped() {}
    override fun onServiceDestroyed() {}
}
