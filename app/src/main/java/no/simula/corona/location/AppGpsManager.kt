package no.simula.corona.location

import android.content.Context
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.microsoft.appcenter.crashes.Crashes
import no.simula.corona.ServiceListener
import no.simula.corona.events.GPSEvent
import org.greenrobot.eventbus.EventBus
import timber.log.Timber

class AppGpsManager(
    private val context: Context,
    private val serviceListener: ServiceListener?,
    private val locationCallback: LocationCallback
) {

    private var mLocationUpdatesActive = false
    private var locationRequest = LocationRequest.create().apply {
        interval = 25000
        fastestInterval = 20000
        smallestDisplacement = 40F
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY  // make adaptive
    }

    private var fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private var mLocationReceiver: LocationServiceReceiver? = null


    internal fun registerLocationReceiver() {
        mLocationReceiver = LocationServiceReceiver()
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        context.registerReceiver(mLocationReceiver, filter)
    }

    private fun unregisterLocationReceiver() {
        if (mLocationReceiver != null) {
            context.unregisterReceiver(mLocationReceiver)
        }
    }

    internal fun stopLocationUpdates() {
        if (mLocationUpdatesActive) {
            Timber.i("Stopping location updates")

            try {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                mLocationUpdatesActive = false
                serviceListener?.onLocationUpdateStopped()
                EventBus.getDefault().post(GPSEvent(false))
                mLocationUpdatesActive = false
            } catch (e: SecurityException) {
                Timber.e(e)
                Crashes.trackError(e)
            }
        } else {
            Timber.i("location services already closed")
        }
    }

    internal fun startLocationUpdates() {
        Timber.i("Starting location updates")
        try {

            if (!LocationServiceReceiver.isLocationServicesOn(context)) {
                Timber.e("Location services are off")
                return
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.myLooper()
            )
            mLocationUpdatesActive = true
            EventBus.getDefault().post(GPSEvent(true))
            serviceListener?.onLocationUpdateStarted()


        } catch (e: SecurityException) {
            Timber.e("Lost location permissions")
            Crashes.trackError(e)
        }
    }

    internal fun isLocationUpdating() = mLocationUpdatesActive

    internal fun destroy() {
        if (isLocationUpdating()) {
            stopLocationUpdates()
        }
        unregisterLocationReceiver()
        locationRequest = null
        mLocationReceiver = null
    }
}