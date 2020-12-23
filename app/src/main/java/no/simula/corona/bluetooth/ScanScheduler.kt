package no.simula.corona.bluetooth

import android.annotation.SuppressLint
import android.app.Application
import android.os.Handler
import android.os.Message
import com.microsoft.appcenter.crashes.Crashes
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import timber.log.Timber
import java.lang.Exception


class ScanScheduler(
    var application: Application, var scanListener: ScanListener,
    private val bluetoothLeScanner: BluetoothLeScannerCompat
) {

    private var scan: Scan? = null
    private var scheduling = false
    private var long_timeout = false

    companion object {
        const val MSG_TIMEOUT = 3
        const val MSG_RESTART = 4
        const val MONITORING_INTERVAL = Scan.SCAN_PERIOD
        const val MONITORING_TIMEOUT = Scan.TIMEOUT
        const val MONITORING_TIMEOUT_LONG = Scan.TIMEOUT_LONG
    }

    private fun startScan() {
        Timber.e("startScanning")
        if (scan == null) {
            scan = Scan(application, scanListener, bluetoothLeScanner)
        }
        scan?.start()
    }

    private fun stopScan() {
        Timber.e("stopScanning")
        scan?.stop()
        scan = null
    }

    fun start() {
        scheduling = true
        startScan()
        scheduleTimeout()
    }

    fun stop() {
        scheduling = false
        messageQueue.removeMessages(MSG_RESTART)
        stopScan()
    }

    private fun scheduleTimeout() {
        messageQueue.sendEmptyMessageDelayed(MSG_TIMEOUT, MONITORING_INTERVAL)
    }

    private fun scheduleRestart() {
        if (!long_timeout) {
            messageQueue.sendEmptyMessageDelayed(MSG_RESTART, MONITORING_TIMEOUT)
        } else {
            messageQueue.sendEmptyMessageDelayed(MSG_RESTART, MONITORING_TIMEOUT_LONG)
            long_timeout = false
        }
    }

    private fun setLongTimeout() {
        long_timeout = false
    }

    private var messageQueue = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            when (msg.what) {
                MSG_TIMEOUT -> {
                    Timber.e("MSG_TIMEOUT $scheduling")
                    try {
                        if (scheduling) {
                            Timber.e("Called from thread:  ${Thread.currentThread().name}")
                            if (scan?.anyDevicesAround() == false) {
                                setLongTimeout()
                            }
                            stopScan()
                            scheduleRestart()
                        }
                    } catch (ex: Exception) {
                        Timber.e(ex)
                        Crashes.trackError(
                            ex,
                            mutableMapOf<String, String>().apply {
                                set(
                                    "where",
                                    "ScanScheduler::handleMsgTimeout"
                                )
                            },
                            null
                        )
                    }
                }

                MSG_RESTART -> {
                    Timber.e("MSG_RESTART")
                    try {
                        Timber.e("$scheduling $scan")
                        if (scheduling) {
                            Timber.d("Called from thread:  ${Thread.currentThread().name}")
                            startScan()
                            scheduleTimeout()
                        }
                    } catch (ex: Exception) {
                        Timber.e(ex)
                        Crashes.trackError(
                            ex,
                            mutableMapOf<String, String>().apply {
                                set(
                                    "where",
                                    "ScanScheduler::handleMsgRestart"
                                )
                            },
                            null
                        )
                    }
                }
            }
        }
    }
}