package no.simula.corona

import android.app.Application
import android.content.Context
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import com.microsoft.azure.sdk.iot.device.*
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus
import com.microsoft.azure.sdk.iot.device.transport.RetryDecision
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.lang.Exception

/**
 * Wrapper for the IoTHub Device
 *
 * Will automatically handle provisioning
 *
 * if isConnected is false the device is not connected and it
 * may or may not be provisioned. In either case, call connect. It
 * will figure out what to do
 *
 * You may call sendMessage at any time, but if the device is not connected
 * the callback will be invoked with the error status
 *
 */
class IoTHubDevice private constructor(private val appContext: Context) {

    companion object : SingletonHolder<IoTHubDevice, Context>(::IoTHubDevice) {
        fun isProvisioned(context: Context): Boolean {
            val connectionString = Utils.IoTConnectionString(context)
            if (connectionString.isEmpty()) {
                return false
            }
            return true
        }
    }

    private val mQueue = Volley.newRequestQueue(appContext)
    private var mDevice: DeviceClient? = null
    private var mDeviceConnected = false
    private var mIsProvisioning = false
    private var mIsConnecting = false

    var version = BuildConfig.VERSION_NAME

    private val mContentEncoding = "utf-8"
    private val mContentType = "application/json"

    /**
     * Try connect to DeviceClient
     *
     * Device need to be not connected and provisioned
     */
    fun connect() {
        if (isConnected() || mIsConnecting) {
            return
        }

        // check if provisioned
        val connectionString = Utils.IoTConnectionString(appContext)
        if (connectionString.isEmpty()) {
            Timber.e("Error: device is not provision")
            return
        }

        connectDevice(connectionString)
    }


    fun isConnected(): Boolean {
        return mDeviceConnected
    }

    fun close() {
        try {
            mDevice?.closeNow()
            mDevice = null
            mDeviceConnected = false
            mIsProvisioning = false
            mIsConnecting = false
        } catch (e: IOException) {
            Crashes.trackError(
                e,
                mutableMapOf<String, String>().apply { set("where", "IoTHubDevice::close") },
                null
            )
            Timber.e(e)
        }
    }

    /**
     * Possible status codes denoted in text. If the status corresponds to a HTTP
     * status code, the HTTP code is written below
     * OK   200
     * OK_EMPTY 204
     * BAD_FORMAT   400
     * UNAUTHORIZED 401
     * TOO_MANY_DEVICES 403
     * HUB_OR_DEVICE_ID_NOT_FOUND   404
     * PRECONDITION_FAILED  412
     * REQUEST_ENTITY_TOO_LARGE 413
     * THROTTLED    429
     * INTERNAL_SERVER_ERROR    500
     * SERVER_BUSY  503
     * ERROR
     * MESSAGE_EXPIRED
     * MESSAGE_CANCELLED_ONCLOSE
     */
    fun sendEvent(
        message: Message,
        callback: IotHubEventCallback
    ) {
        if (!mDeviceConnected || mDevice == null) {
            Timber.i("Device is not connected or provisioned")
            callback.execute(IotHubStatusCode.ERROR, mutableListOf<List<Int>>())
            return
        }


        if (message.contentType != mContentType) {
            message.setContentTypeFinal(mContentType)
        }

        if (message.contentEncoding != mContentEncoding) {
            message.contentEncoding = mContentEncoding
        }

        try {
            // IllegalArgumentException
            // IllegalStateException
            mDevice?.sendEventAsync(message, callback, null)
            Timber.i("Pushed message")
        } catch (e: Exception) {
            Crashes.trackError(
                e,
                mutableMapOf<String, String>().apply {
                    set(
                        "where",
                        "IoTHubDevice::sendEventAsync"
                    )
                },
                null
            )
            Timber.e(e)

            callback.execute(IotHubStatusCode.ERROR, mutableListOf<List<Int>>())
        }
    }

    fun performProvisioning(token: String, listener: ProvisioningListener) {
        if (isProvisioned(appContext)) {
            listener.onProvisionComplete(
                null /* null  means get data from saved preferences*/,
                true
            )
        }

        provision(object : ProvisioningListener {
            override fun onProvisionComplete(json: JSONObject?, saved: Boolean) {
                listener.onProvisionComplete(json, Utils.registerDevice(appContext, json!!))
            }

            override fun onProvisionFailed(error: VolleyError) {
                listener.onProvisionFailed(error)
            }
        }, token)

    }

    private fun provision(listener: ProvisioningListener?, token: String) {

        if (mIsProvisioning) {
            // provisioning is already in progress
            listener?.onProvisionFailed(VolleyError("Provisioning already started"))
        }

        Timber.i("Starting device provisioning")

        mIsProvisioning = true

        val request = object : JsonObjectRequest(
            Method.POST, BuildConfig.REGISTER_DEVICE_URL, null,
            Response.Listener<JSONObject> { response ->
                listener?.onProvisionComplete(response, false)
            },
            Response.ErrorListener { error -> listener?.onProvisionFailed(error) }
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
    }

    private fun connectDevice(connectionString: String) {
        Timber.i("Trying to connect device")
        mIsConnecting = true

        try {
            // c-tor throws URISyntaxException
            mDevice = DeviceClient(connectionString, IotHubClientProtocol.HTTPS).apply {
                setRetryPolicy { _, exception ->
                    Crashes.trackError(exception)
                    RetryDecision(false, 10000)
                }
                registerConnectionStatusChangeCallback({ status, statusChangeReason, throwable, _ ->
                    if (throwable != null) {
                        Crashes.trackError(
                            throwable,
                            mutableMapOf<String, String>().apply {
                                set(
                                    "where",
                                    "IoTHubDevice::registerConnectionStatus"
                                )
                            },
                            null
                        )
                    }

                    mIsConnecting = false
                    mDeviceConnected = when (status) {
                        IotHubConnectionStatus.CONNECTED -> true
                        else -> false
                    }

                    val reason = statusChangeReason?.name ?: "null"
                    val result = status?.name ?: "null-status"
                    val event = "$result because $reason"
                    Timber.e(event)
                    Analytics.trackEvent(event)
                }, null) // IllegalArgumentException
                setOperationTimeout(1000)  // 1 second - IllegalArgumentException

                // We don't want to poll the backend
                // I don't see how it is possible to disable polling using HTTPS as client protocol
                // poll the server every 30 minutes
                setOption("SetMinimumPollingInterval", 1_800_000L) // IllegalArgumentException
                open()  // IOException
                setMessageCallback(MessageCallback(), null)
            }
        } catch (e: Exception) {
            mIsConnecting = false
            mDevice = null

            Timber.e(e)
            Crashes.trackError(
                e,
                mutableMapOf<String, String>().apply { set("where", "IoTHubDevice::connect") },
                null
            )
        }
    }

    interface ProvisioningListener {
        fun onProvisionComplete(json: JSONObject?, saved: Boolean)
        fun onProvisionFailed(error: VolleyError)
    }

    inner class MessageCallback : com.microsoft.azure.sdk.iot.device.MessageCallback {
        override fun execute(message: Message?, callbackContext: Any?): IotHubMessageResult {
            val version = message?.getProperty("versionAndroid")
            if (version == null) {
                return IotHubMessageResult.ABANDON
            }

            if (version.split(".").size == 3) {
                this@IoTHubDevice.version = version
                return IotHubMessageResult.COMPLETE
            }

            return IotHubMessageResult.ABANDON
        }
    }
}