package no.simula.corona.bluetooth

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.os.Build
import android.os.ParcelUuid
import no.nordicsemi.android.support.v18.scanner.*
import no.simula.corona.Utils
import no.simula.corona.bluetooth.Scan.Companion.SCAN_PERIOD
import timber.log.Timber
import java.lang.Exception
import java.lang.IllegalArgumentException


interface ScanListener {
    fun onNewBluetoothResult(deviceId: String, rssi: Int, txPower: Int, timestamp: Long)
}


class Scan(
    application: Application,
    scanListener: ScanListener,
    private val bluetoothLeScanner: BluetoothLeScannerCompat
) {

    companion object {
        const val SCAN_PERIOD: Long = 1000 * 1 * 35 // 35 second scan
        const val TIMEOUT: Long = 1000 * 60 * 2 // 2 minute timeout
        const val TIMEOUT_LONG: Long = 1000 * 60 * 6 // 5 minutes long timeout
    }

    private val serviceFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(Utils.SMITTESTOPP_SERVICE_UUID))
        .build()

    // iOS manufacture id from NHS
    private val iPhoneBackgroundFilter = ScanFilter.Builder()
        .setServiceUuid(null)
        .setManufacturerData(
            76,
            byteArrayOf(
                0x01, // 0
                0x00, // 1
                0x00, // 2
                0x00, // 3
                0x00, // 4
                0x00, // 5
                0x00, // 6
                0x00, // 7
                0x00, // 8
                0x00, // 9
                0x00, // 10
                0x00, // 11
                0x00, // 12
                0x00, // 13
                0x02, // 14
                0x00, // 15
                0x00 // 16
            ),
            byteArrayOf(
                0xFF.toByte(), // 0
                0x00, // 1
                0x00, // 2
                0x00, // 3
                0x00, // 4
                0x00, // 5
                0x00, // 6
                0x00, // 7
                0x00, // 8
                0x00, // 9
                0x00, // 10
                0x00, // 11
                0x00, // 12
                0x00, // 13
                0x02, // 14
                0x00, // 15
                0x00 // 16
            )
        )
        .build()

    private val filters = listOf(serviceFilter, iPhoneBackgroundFilter)

    private val settings = ScanSettings.Builder()
        .setReportDelay(0)
        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
        .build()

    private val scanCallBack = ScanningCallback(application, scanListener)

    fun start(retrying: Boolean = false) {
        try {
            bluetoothLeScanner.startScan(filters, settings, scanCallBack)
        } catch (e: IllegalArgumentException) {
            // throws IllegalArgumentException if scanner with this callback is already running
            bluetoothLeScanner.stopScan(scanCallBack)
            if (!retrying)
                start(true)
        } catch (e: Exception) {
            // throws IllegalStateException if bt disabled or not available
            Timber.e(e)
        }
    }

    fun stop() {
        try {
            bluetoothLeScanner.flushPendingScanResults(scanCallBack)
            bluetoothLeScanner.stopScan(scanCallBack)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun anyDevicesAround(): Boolean {
        return scanCallBack.anyDevicesAround()
    }
}


private class ScanningCallback(
    private val application: Application,
    var scanListener: ScanListener?
) : ScanCallback() {

    private var lastDeviceSentTime: Long = 0
    private val seenDevices: MutableList<String> = mutableListOf()

    override fun onScanResult(callbackType: Int, result: ScanResult) {
        onResult(result)
    }

    override fun onBatchScanResults(results: List<ScanResult>) {
        Timber.i("onBatchScanResults size ${results.size}")
        results.forEach { onResult(it) }
    }

    private fun onResult(result: ScanResult) {
        if (!seenDevices.contains(result.device.address)) {
            Timber.i("Received $result")
            lastDeviceSentTime = System.currentTimeMillis()
            val gattClientCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                GattClientCallback(scanListener, result.rssi, result.txPower)
            } else {
                GattClientCallback(scanListener, result.rssi, 0)
            }
            seenDevices.add(result.device.address)
            connectToDevices(result.device, gattClientCallback)
        } else {
            //"Ignore previously discovered device ${result.device}"
        }
    }

    private fun connectToDevices(
        device: BluetoothDevice,
        gattClientCallback: GattClientCallback
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(application, false, gattClientCallback, TRANSPORT_LE)
        } else {
            device.connectGatt(application, false, gattClientCallback)
        }
    }

    override fun onScanFailed(errorCode: Int) {
        Timber.e("Scan failed $errorCode")
    }

    fun anyDevicesAround(): Boolean {
        // SCAN_PERIOD is can be controlled to rescheduler
        return (System.currentTimeMillis() - lastDeviceSentTime) < SCAN_PERIOD
    }
}