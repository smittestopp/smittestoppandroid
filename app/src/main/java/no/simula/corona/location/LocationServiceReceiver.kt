package no.simula.corona.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import no.simula.corona.events.LocationServiceEvent
import org.greenrobot.eventbus.EventBus


class LocationServiceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.getAction()?.equals(LocationManager.PROVIDERS_CHANGED_ACTION) == true) {
            EventBus.getDefault().post(LocationServiceEvent(isLocationServicesOn(context!!)))
        }
    }

    companion object {
        fun isLocationServicesOn(context: Context): Boolean {
            val mode: Int = Settings.Secure.getInt(
                context?.contentResolver,
                Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF
            )
            return mode != Settings.Secure.LOCATION_MODE_OFF

        }
    }
}
