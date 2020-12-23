package no.simula.corona

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.android.volley.VolleyError
import com.google.android.gms.location.*
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import com.microsoft.appcenter.crashes.Crashes
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode
import com.microsoft.azure.sdk.iot.device.Message
import no.simula.corona.Utils.Companion.toUnixTime
import no.simula.corona.Utils.Companion.unixtime

import no.simula.corona.bluetooth.ScanListener

import no.simula.corona.bluetooth.*
import no.simula.corona.data.DatabaseContract
import no.simula.corona.data.DatabaseProvider
import no.simula.corona.data.model.BluetoothContactBuilder
import no.simula.corona.data.model.MeasurementBuilder
import no.simula.corona.events.BluetoothAdapterEvent
import no.simula.corona.events.BluetoothEvent
import no.simula.corona.events.GPSEvent
import no.simula.corona.events.LocationServiceEvent
import no.simula.corona.location.AppGpsManager
import no.simula.corona.heartbeat.Heartbeat
import no.simula.corona.heartbeat.RunningStatus
import no.simula.corona.notification.AppNotificationManager
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.ThreadLocalRandom


class CollectorBinder(private val collector: DataCollectorService) : Binder() {
    fun getService(): DataCollectorService {
        return collector
    }
}

class DataCollectorService : Service(), ScanListener {
    companion object {

        const val FROM_SERVICE = "from-service"
        const val SERVICE_COMMAND = "service-command"
        const val AUTO_START = "auto_start"

        const val COMMAND_START_LOCATION_UPDATES = 1
        const val COMMAND_STOP_LOCATION_UPDATES = 0
        const val COMMAND_START_BT_UPDATES = 2
        const val COMMAND_STOP_BT_UPDATES = 3

        const val COMMAND_START_ALL_USER_SELECTED = 4
        const val COMMAND_STOP_ALL_USER_SELECTED = 5
        const val COMMAND_STATUS_QUERY = 6


        fun queryServiceStatus(context: Context, isAppInBackground: Boolean) {
            Intent(context, DataCollectorService::class.java).apply {
                this.putExtra(SERVICE_COMMAND, COMMAND_STATUS_QUERY)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isAppInBackground) {
                    context.startForegroundService(this)
                } else {
                    context.startService(this)
                }
            }
        }

        fun startLocationUpdates(context: Context) {
            Intent(context, DataCollectorService::class.java).apply {
                this.putExtra(SERVICE_COMMAND, COMMAND_START_LOCATION_UPDATES)
                context.startService(this)
            }
        }

        fun stopLocationUpdates(context: Context) {
            Intent(context, DataCollectorService::class.java).apply {
                this.putExtra(SERVICE_COMMAND, COMMAND_STOP_LOCATION_UPDATES)
                context.startService(this)
            }
        }

        fun startBluetoothUpdates(context: Context) {
            Intent(context, DataCollectorService::class.java).apply {
                this.putExtra(SERVICE_COMMAND, COMMAND_START_BT_UPDATES)
                context.startService(this)
            }
        }

        fun stopBluetoothUpdates(context: Context) {
            Intent(context, DataCollectorService::class.java).apply {
                this.putExtra(SERVICE_COMMAND, COMMAND_STOP_BT_UPDATES)
                context.startService(this)
            }
        }

        fun startAllUserSelected(context: Context) {
            Intent(context, DataCollectorService::class.java).apply {
                this.putExtra(SERVICE_COMMAND, COMMAND_START_ALL_USER_SELECTED)
                context.startService(this)
            }
        }

        fun stopAllUserSelected(context: Context) {
            Intent(context, DataCollectorService::class.java).apply {
                this.putExtra(SERVICE_COMMAND, COMMAND_STOP_ALL_USER_SELECTED)
                context.startService(this)
            }
        }
    }

    var serviceCommandCount = 0
    var serviceListener: ServiceListener? = null

    // Location updating
    private val mBinder = CollectorBinder(this)
    private var mIsBound = false

    private lateinit var locationCallback: LocationCallback
    private var lastUpdate = unixtime()
    private val updateInterval = 1 // hour


    // Data uploader
    private lateinit var mDatabase: DatabaseContract
    private var mDataAggregatorTask: DataAggregatorTask? = null
    private lateinit var mDevice: IoTHubDevice

    //managers
    private var appNotificationManager: AppNotificationManager? = null
    private var appBluetoothManager: AppBluetoothManager? = null
    private var appGpsManager: AppGpsManager? = null

    /**
     * On create.
     */
    override fun onCreate() {

        Timber.i("onCreate")
        mDatabase = DatabaseProvider.open(this)
        mDevice = IoTHubDevice.getInstance(this.application)
        mDevice.connect()

        // setup location services using a fused location provider
        // provides high-level interface to location services
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                if (locationResult == null) {
                    Timber.i("Location result is null")
                    return
                }

                for (location in locationResult.locations) {
                    handleNewLocation(location)
                }
            }
        }
        appNotificationManager = AppNotificationManager(this)
        appBluetoothManager = AppBluetoothManager(this)
        appGpsManager = AppGpsManager(this, serviceListener, locationCallback)
        appGpsManager?.registerLocationReceiver()
        appBluetoothManager?.registerBluetoothReceiver()
        EventBus.getDefault().register(this)

    }

    /**
     * On start.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.i("onStartCommand")


        if (intent?.getBooleanExtra(FROM_SERVICE, false) == true) {
            // User stopped the service from notification
            Utils.setDidUserStartBTService(this, false)

            // User stopped the service from notification
            Utils.setDidUserStartGPSService(this, false)

            stopServiceUpdates()
            stopSelf()

        }


        if (intent?.hasExtra(SERVICE_COMMAND) == true) {
            when (intent.getIntExtra(SERVICE_COMMAND, -1)) {
                COMMAND_START_LOCATION_UPDATES -> {
                    Timber.i("start location updates")
                    appGpsManager?.startLocationUpdates()
                }
                COMMAND_START_BT_UPDATES -> {
                    Timber.i("start bt updates")
                    appBluetoothManager?.startBluetoothUpdates(application, this, serviceListener)
                }

                COMMAND_START_ALL_USER_SELECTED -> {
                    Timber.i("start all updates")
                    if (Utils.didUserStartGPS(this)) {
                        appGpsManager?.startLocationUpdates()
                    }

                    if (Utils.didUserStartBluetooth(this)) {
                        appBluetoothManager?.startBluetoothUpdates(
                            application,
                            this,
                            serviceListener
                        )
                    }

                    if (intent.getBooleanExtra(AUTO_START, false)) {
                        appNotificationManager?.startForegroundIfNotExists(this)
                    }
                }

                COMMAND_STOP_LOCATION_UPDATES -> {
                    Timber.i("stop location updates")
                    appGpsManager?.stopLocationUpdates()
                }

                COMMAND_STOP_BT_UPDATES -> {
                    Timber.i("stop bt updates")
                    appBluetoothManager?.stopBluetoothUpdates()
                }

                COMMAND_STOP_ALL_USER_SELECTED -> {
                    Timber.i("stop all updates")
                    appGpsManager?.stopLocationUpdates()
                    appBluetoothManager?.stopBluetoothUpdates()
                    stopSelf() // Only place where selfstop can be called.
                }
                COMMAND_STATUS_QUERY -> {
                    publishStatus(serviceCommandCount == 0)
                }
            }

            serviceCommandCount++
        }



        return START_NOT_STICKY
    }

    private fun publishStatus(stopService: Boolean) {
        RunningStatus.publish(
            appGpsManager?.isLocationUpdating() == true,
            appBluetoothManager?.isBluetoothUpdating() == true
        )

        if (stopService) {
            stopSelf()
        }
    }


    /**
     * On bind.
     */
    override fun onBind(intent: Intent?): IBinder? {
        mIsBound = true
        appNotificationManager?.removeNotificationReminders()
        return mBinder
    }

    /**
     * On rebind.
     */
    override fun onRebind(intent: Intent?) {
        mIsBound = true
        appNotificationManager?.removeNotificationReminders()
        super.onRebind(intent)
    }

    /**
     * On unbind
     */
    override fun onUnbind(intent: Intent?): Boolean {
        mIsBound = false

        handleForegroundNotification()

        return true
    }


    /**
     * On destroy. Shut down running services.
     */
    override fun onDestroy() {

        appNotificationManager?.stopForeground(this)
        appGpsManager?.destroy()
        appNotificationManager?.destroy()
        appBluetoothManager?.destroy()
        mDevice.close()

        if (mDataAggregatorTask?.isRunningOrPending() == true) {
            try {
                mDataAggregatorTask?.cancel(true)
            } catch (ex: Exception) {
                Crashes.trackError(
                    ex,
                    mutableMapOf<String, String>().apply {
                        set(
                            "where",
                            "DataCollector::onDestroy"
                        )
                    },
                    null
                )
            }
        }
        mDataAggregatorTask = null
        serviceListener?.onServiceDestroyed()
        EventBus.getDefault().unregister(this)
        mDatabase.close()

        serviceCommandCount = 0

        super.onDestroy()

    }

    private fun stopServiceUpdates() {
        appGpsManager?.stopLocationUpdates()
        appBluetoothManager?.stopBluetoothUpdates()
    }

    /**
     * Is updating
     */

    fun isUpdating(): Boolean =
        appGpsManager?.isLocationUpdating()!! || appBluetoothManager?.isBluetoothUpdating()!! // Partially activated service

    fun isBluetoothUpdating(): Boolean = appBluetoothManager?.isBluetoothUpdating()!!
    fun isLocationUpdating(): Boolean = appGpsManager?.isLocationUpdating()!!

    /**
     * Build Measurement from Location object and insert it into the database.
     */
    private fun handleNewLocation(location: Location) {

        if (Utils.hasGivenConsent(this) == false) {
            Timber.d("no consent")
            return
        }

        if (Utils.hasToken(this) == false) {
            Timber.d("no token")
            return
        }

        val latitude = location.latitude
        val longitude = location.longitude
        val accuracy = location.accuracy
        Timber.i("($latitude, $longitude), accuracy: $accuracy")

        // build Measurement
        val builder = MeasurementBuilder(toUnixTime(location.time))
        builder.setGpsLocation(location)
        mDatabase.insertGPS(builder.toEntity())

        // Upload to database
        if (timeToUpload())
            startUploading()
    }

    /**
     * check if it is time to start a new uploader instance
     */
    private fun timeToUpload(): Boolean {
        // allow update every hour
        val waitTime = updateInterval * 60 * 60
        val noisyWaitTime = ThreadLocalRandom.current().nextInt(-10, 10) * 60
        val current = unixtime()
        val provisionTime = current > lastUpdate + waitTime + noisyWaitTime
        val upload = current > lastUpdate + waitTime

        if (provisionTime && !Utils.isProvisioned(this)) {
            createNotificationOrProvisionRetry()
            return false
        }

        return upload && Utils.isProvisioned(this)
    }

    private fun createNotificationOrProvisionRetry() {
        if (!Utils.isAuthenticated(this)) { // unprovsioned auth has been expired
            if (CoronaApp.isInBackground() == true) {
                appNotificationManager?.createAuthNotificationIfNotCreateAlready()
                appGpsManager?.stopLocationUpdates()
                appBluetoothManager?.stopBluetoothUpdates()
                stopSelf()
            }
            return
        }

        lastUpdate = unixtime()
        retryProvisioning()
    }

    private fun retryProvisioning() {
        val token = Utils.getToken(this)

        if (token.isNotBlank()) {
            IoTHubDevice.getInstance(this.application).performProvisioning(token, object :
                IoTHubDevice.ProvisioningListener {
                override fun onProvisionComplete(json: JSONObject?, saved: Boolean) {
                    Heartbeat.register(applicationContext)

                    Analytics.trackEvent(
                        "Provisioned",
                        EventProperties().apply { set("where", "DataCollector retryProvisioning") })
                }

                override fun onProvisionFailed(error: VolleyError) {
                    Crashes.trackError(
                        error,
                        mutableMapOf<String, String>().apply {
                            put(
                                "where",
                                "DataCollector retryProvisioning"
                            )
                        },
                        null
                    )
                }
            })
        }
    }


    /**
     * Start new thread to upload data.
     */
    private fun startUploading() {
        if (mDataAggregatorTask?.isRunningOrPending() == true) {
            return
        }

        if (!mDevice.isConnected()) {
            mDevice.connect()
            return
        }

        if (updateIsAvailable(mDevice.version)) {
            appNotificationManager?.notifyUpdateAvailable()
        }

        // take a note of the last upload initiation
        lastUpdate = unixtime()

        val task = DataAggregatorTask(this, object : DataAggregatorListener {
            override fun onJSonDataPrepared(chunks: JsonChunks?) {
                if (chunks == null) {
                    return
                }

                sendMessages("gps", chunks.gps)
                sendMessages("bluetooth", chunks.bluetooth)
            }
        })

        mDataAggregatorTask = task
        task.execute()
    }

    private fun sendMessages(type: String, data: JsonChunk) {
        for (i in 0 until data.size()) {
            val (id, json) = data.get(i)

            if (json.length() > 0 && json.getJSONArray("events").length() > 0) {
                val message = Message(json.toString())
                message.setProperty("eventType", type)
                mDevice.sendEvent(message, IotHubEventCallbackImp(type, id))
            }
        }
    }

    private fun uploadSuccessful(mode: String, sourceIds: List<Long>) {
        when (mode) {
            "gps" -> mDatabase.markAsUploadedGPS(sourceIds)
            "bluetooth" -> mDatabase.markAsUploadedBluetooth(sourceIds)
            else -> Analytics.trackEvent(
                "Mark upload failed",
                EventProperties().apply { set("eventType", mode) })
        }
    }

    inner class IotHubEventCallbackImp(
        private val mode: String,
        private val tobeUpdated: List<Long>
    ) :
        IotHubEventCallback {
        override fun execute(responseStatus: IotHubStatusCode?, callbackContext: Any?) {
            when (responseStatus) {
                IotHubStatusCode.OK, IotHubStatusCode.OK_EMPTY -> {
                    Timber.i("Uploaded $mode data, OK, OK_EMPTY")
                    uploadSuccessful(mode, tobeUpdated)
                }
                IotHubStatusCode.HUB_OR_DEVICE_ID_NOT_FOUND -> {
                    // consent removed from another device and/or
                    // iot hub device is deleted in the cloud
                    handleDeletedDevice()
                }
                else -> {
                    // don't mark anything as uploaded since that failed
                    // delete the current instance such that we are able to try again
                    // on one hour
                    val why = "Uploaded failed because ${responseStatus?.name ?: "N/A"}"
                    Analytics.trackEvent(why)
                    Timber.e(why)
                }
            }

            mDataAggregatorTask = null
        }
    }

    override fun onNewBluetoothResult(
        deviceId: String, rssi: Int, txPower: Int,
        timestamp: Long
    ) {
        if (Utils.hasGivenConsent(this) == false) {
            Timber.d("no consent")
            return
        }

        if (Utils.hasToken(this) == false) {
            Timber.d("no token")
            return
        }

        Timber.i("found connection to $deviceId, signal strength $rssi")

        // build Measurement
        val builder = BluetoothContactBuilder(toUnixTime(timestamp))
        builder.setCloseContact(deviceId, rssi, txPower)
        mDatabase.insertBluetooth(builder.toEntity())

        // Upload to server
        if (timeToUpload())
            startUploading()
    }

    /**
     * Iot Hub device was deleted either by revoking consent
     *  or deleted from the cloud
     *
     *  Stop this service and show user a notification
     */
    private fun handleDeletedDevice() {
        Utils.setConsent(this, false)

        appGpsManager?.stopLocationUpdates()
        appBluetoothManager?.stopBluetoothUpdates()
        Utils.deleteLocalData(this)

        mDatabase.deleteEverythingBluetooth()
        mDatabase.deleteEverythingGPS()

        appNotificationManager?.handleDeletedDevice()
        serviceListener?.onIotDeviceDeleted()

        stopSelf()
    }

    /**
     * Check in the incoming version from Iot Hub is newer than the current app version
     *
     * @param version most recent version given my incoming Iot Hub message
     * @return true is there's a newer version available
     */
    private fun updateIsAvailable(version: String): Boolean {
        val current = BuildConfig.VERSION_NAME.split(".")
        if (current.size < 3) {
            // Release has 3 x.x.x
            // Debug has 4 x.x.x.d
            return false
        }

        val major = current[0].toInt()
        val minor = current[1].toInt()
        val patch = current[2].toInt()

        val other = version.split(".")
        if (other.size != 3) {
            // bad version string, ignore
            Analytics.trackEvent(
                "Malformed version string",
                EventProperties().apply { set("version", version) })

            return false
        }

        val ma = other[0].toInt()
        val mi = other[1].toInt()
        val pa = other[2].toInt()

        return ma > major ||
                (ma == major && mi > minor) || // minor incremented
                (ma == major && mi == minor && pa > patch)  // patch incremented
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: BluetoothAdapterEvent) {
        Timber.d(event.toString())
        if (appBluetoothManager?.isBluetoothUpdating()!!) {
            appBluetoothManager?.stopBluetoothUpdates()
        }

        if (event.on && !appBluetoothManager?.isBluetoothUpdating()!!) {

            if (Utils.didUserStartBluetooth(this) && isUserReady(this)) {
                Timber.d("starting Bluetooth adapter on event")
                DataCollectorService.startBluetoothUpdates(this)
            }
        }
    }


    private fun handleForegroundNotification() {
        if (isUpdating()) { // only create notification if service is partially working (either BT or Loc)
            appNotificationManager?.startForegroundIfNotExists(this)
        } else {
            appNotificationManager?.stopForeground(this)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: LocationServiceEvent) {
        Timber.d(event.toString())
        if (appGpsManager?.isLocationUpdating()!!) {
            appGpsManager?.stopLocationUpdates()
        }

        if (event.on && !appGpsManager?.isLocationUpdating()!!) {

            if (Utils.didUserStartGPS(this) && isUserReady(this)) {
                Timber.d("starting location adapter on event")
                DataCollectorService.startLocationUpdates(this)
            }
        }
    }

    private fun isUserReady(context: Context): Boolean =
        Utils.hasToken(context) && Utils.hasGivenConsent(context)

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public fun onEvent(event: BluetoothEvent) {
        handleForegroundNotification()
        RunningStatus.setBluetoothRunning(event.on)
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public fun onEvent(event: GPSEvent) {
        handleForegroundNotification()
        RunningStatus.setGpsRunning(event.on)
    }
}