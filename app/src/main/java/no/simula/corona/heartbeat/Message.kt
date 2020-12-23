package no.simula.corona.heartbeat

import android.content.Context
import android.os.Build
import no.simula.corona.BuildConfig
import no.simula.corona.Utils
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class Message(context: Context, var status:Int) {


    fun json(): JSONObject {
        val v = JSONObject()

        v.put("appVersion", BuildConfig.VERSION_NAME)
        v.put("platform", "android")
        v.put("osVersion", Build.VERSION.RELEASE)
        v.put("model", "${Build.MANUFACTURER} ${Build.MODEL}")
        v.put("events", heartbeatEvent())

        return v
    }

    private fun heartbeatEvent(): JSONArray {
        return JSONArray().apply {
            put(JSONObject().apply {
                put("timestamp", timestamp())
                put("status", status)
            })
        }
    }

    /**
     * @return current time as UTC
     */
    private fun timestamp(): String {
        return with(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)) {
            timeZone = TimeZone.getTimeZone("UTC")
            format(System.currentTimeMillis())
        }
    }
}