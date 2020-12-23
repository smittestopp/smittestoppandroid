package no.simula.corona.heartbeat

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode
import kotlinx.coroutines.delay
import no.simula.corona.CoronaApp
import no.simula.corona.DataCollectorService
import no.simula.corona.IoTHubDevice
import no.simula.corona.notification.AppNotificationManager
import timber.log.Timber

class HeartbeatWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private val context = context
    private val isRescheduled = params.inputData.getBoolean(RESCHEDULE, false)


    override fun doWork(): Result {
        // runs on background thread
        Timber.d("HeartbeatWorker do some work")

        val device = IoTHubDevice.getInstance(context)
        if (device.isConnected()) {

            RunningStatus.statusListener = statusListener // statusListener is declared in companion

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (AppNotificationManager.doesStatusNotificationExist(context)) {
                    messageQueue.sendEmptyMessage(QUERY_MSG)
                } else {
                    sendCompletelyOff()
                }
            } else {
                messageQueue.sendEmptyMessage(QUERY_MSG)    // shifting the ownership of remaining work to main thread handler
            }
        } else {
            Timber.d("Connect iot hub device")
            device.connect()

            if (!isRescheduled) {
                // Schedule this task to run again in a few minutes such
                // that the iot hub device have enough time to connect
                Heartbeat.scheduleSingle(context)
            }
        }

        return Result.success()
    }

    private fun sendCompletelyOff() {
        var message = android.os.Message()
        message.what = SEND_HEARTBEAT_MSG
        message.arg1 = RunningStatus.NOTHING_RUNNING
        messageQueue.sendMessage(message)
    }


    companion object {
        const val RESCHEDULE = "1aa8"
        const val QUERY_MSG = 1
        const val SEND_HEARTBEAT_MSG = 2


        var messageQueue = @SuppressLint("HandlerLeak")
        object : Handler(Looper.getMainLooper()) {

            override fun handleMessage(msg: android.os.Message) {
                when (msg.what) {

                    QUERY_MSG -> {
                        var theApp = CoronaApp.instance()!!
                        DataCollectorService.queryServiceStatus(
                            theApp.applicationContext,
                            theApp.inBackground
                        )
                    }

                    SEND_HEARTBEAT_MSG -> {
                        sendHeartbeat(msg.arg1)
                    }
                }
            }
        }

        fun sendHeartbeat(status: Int) {
            val device = IoTHubDevice.getInstance(CoronaApp.instance()!!.applicationContext)
            val message = com.microsoft.azure.sdk.iot.device.Message(
                Message(
                    CoronaApp.instance()!!.applicationContext,
                    status
                ).json().toString()
            )
            message.setProperty("eventType", "sync")

            Timber.d("Send heartbeat")
            device.sendEvent(message, Noop())

        }

        var statusListener = object : RunningStatus.OnStatusListening {
            override fun onStatusPublished(status: Int) {
                //will be called in main thread
                var message = android.os.Message()
                message.what = SEND_HEARTBEAT_MSG
                message.arg1 = status
                messageQueue.sendMessage(message)
            }
        }


    }

    class Noop : IotHubEventCallback {
        override fun execute(a: IotHubStatusCode?, b: Any?) {}
    }

}