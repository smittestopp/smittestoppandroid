package no.simula.corona.bluetooth

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import com.microsoft.appcenter.crashes.Crashes
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.simula.corona.ServiceListener
import no.simula.corona.events.BluetoothEvent
import org.greenrobot.eventbus.EventBus
import timber.log.Timber

class AppBluetoothManager(private val context: Context) {

    // Bluetooth
    private var mBluetoothUpdatesActive = false
    private var mAdvertise: Advertise? = null
    private var mScanScheduler: ScanScheduler? = null
    private var mGatt: Gatt? = null
    private var mBluetoothReceiver: BluetoothReceiver? = null

    internal fun registerBluetoothReceiver() {
        mBluetoothReceiver = BluetoothReceiver()
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }

        context.registerReceiver(mBluetoothReceiver, filter)
    }

    internal fun unRegisterBluetoothReceiver() {
        context.unregisterReceiver(mBluetoothReceiver)
    }

    private fun initializeBluetooth(application: Application, scanListener: ScanListener) {

        Timber.d("Initializing bluetooth components")

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        try {
            if (bluetoothManager.adapter?.isEnabled == true) { // Devices with bluetooth off will have bluetoothManager.adapter == null
                if (bluetoothManager.adapter.bluetoothLeAdvertiser == null) {
                    Timber.w("Le advertiser is not supported on this device")
                    Analytics.trackEvent("LeAdvertise", EventProperties().apply {
                        set("what", "Not supported")
                        set("model", "${Build.MANUFACTURER} ${Build.MODEL}")
                    })
                }

                mGatt = Gatt(context, bluetoothManager)
                mAdvertise = Advertise(bluetoothManager.adapter.bluetoothLeAdvertiser)
                mScanScheduler = ScanScheduler(
                    application,
                    scanListener,
                    BluetoothLeScannerCompat.getScanner()
                )
            } else {
                Timber.d("Bluetooth adapter is null")
            }
        } catch (ex: Exception) {
            Crashes.trackError(
                ex,
                mutableMapOf<String, String>().apply {
                    set(
                        "where",
                        "DataCollector::initializeBluetooth"
                    )
                },
                null
            )
        }
    }

    internal fun startBluetoothUpdates(application: Application, scanListener: ScanListener,serviceListener: ServiceListener?) {

        if (!isBluetoothInitialize()) {
            initializeBluetooth(application, scanListener)
        }
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        Timber.i("Starting bluetooth updates")
        try {
            if (bluetoothManager.adapter?.isEnabled == true) {
                mGatt?.start()
                mAdvertise?.start()
                mScanScheduler?.start()
                mBluetoothUpdatesActive = true
                EventBus.getDefault().post(BluetoothEvent(true))
                serviceListener?.onBluetoothUpdateStarted()

            } else {
                Timber.e("Bluetooth adaptor is off")
            }
        } catch (e: SecurityException) {
            Timber.e("Lost bluetooth permissions")
        }
    }

    internal fun stopBluetoothUpdates() {
        if (mBluetoothUpdatesActive) {
            Timber.i("Stopping bluetooth updates")
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

            if (bluetoothManager.adapter?.isEnabled == true) {
                try {
                    mGatt?.stop()
                    mScanScheduler?.stop()
                    mAdvertise?.stop()
                    mBluetoothUpdatesActive = false
                    EventBus.getDefault().post(BluetoothEvent(false))
                } catch (e: java.lang.Exception) {
                    Timber.e(e)
                    Crashes.trackError(
                        e,
                        mutableMapOf<String, String>().apply {
                            set(
                                "where",
                                "DataCollector::stopBluetoothUpdates"
                            )
                        },
                        null
                    )
                }
            } else {
                // if user turns off bluetooth adapter then you have to re - initialize bluetooth classes
                // After adaptor turns  off new initialization is required

                // Do need that.
                /* mGatt = null
                 mScanScheduler = null
                 mAdvertise = null*/
                mScanScheduler?.stop()
                mBluetoothUpdatesActive = false
                EventBus.getDefault().post(BluetoothEvent(false))
            }
        } else {
            Timber.i("bluetooth is already closed")
        }
    }

    private fun isBluetoothInitialize(): Boolean {
        return mGatt != null && mAdvertise != null && mScanScheduler != null
    }

    internal fun isBluetoothUpdating() = mBluetoothUpdatesActive

    internal fun destroy() {
        if (isBluetoothUpdating()) {
            stopBluetoothUpdates()
        }
        unRegisterBluetoothReceiver()
        mGatt = null
        mAdvertise = null
        mScanScheduler = null
    }
}