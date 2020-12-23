package no.simula.corona

import android.app.Activity
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import no.simula.corona.data.Preferences
import no.simula.corona.data.PreferencesBase
import org.json.JSONObject
import java.util.*


class Utils {
    companion object {

        private var mPreferences: PreferencesBase = Preferences()

        private fun getPreferences(): PreferencesBase {
            //TODO This will return secured preferences in future.
            return mPreferences
        }

        /**
         * Checks if a certain permission has been granted. Calls
         * @see[ContextCompat.checkSelfPermission]
         *
         * @param[context] Context.
         * @param[permission] The permission to be checked.
         * @return True if app has permission, false otherwise.
         */
        fun hasPermission(context: Context, permission: String): Boolean {
            val r = ContextCompat.checkSelfPermission(context, permission)
            return r == PackageManager.PERMISSION_GRANTED
        }

        /**
         * Request permissions to be granted to this application. Calls
         * @see[ActivityCompat.requestPermissions]
         *
         * @param activity The target activity.
         * @param permissions The requested permissions. Must me non-null and not empty.
         * @param requestCode Application specific request code, should be >= 0.
         */
        fun requestPermission(context: Activity, permissions: Array<String>, requestCode: Int) {
            ActivityCompat.requestPermissions(context, permissions, requestCode)
        }

        /**
         * Check if user has given consent to the privacy policy.
         *
         * @return True if user has given consent, false otherwise.
         */
        fun hasGivenConsent(context: Context) : Boolean = getPreferences().hasGivenConsent(context)

        /**
         * Set consent preference.
         *
         * @param[b] Consent preference to be set.
         */
        fun setConsent(context: Context, b: Boolean) = getPreferences().setConsent(context, b)

        /**
         * Set date of birth
         */
        fun setDate(context: Context, a: String) = getPreferences().setDate(context, a)

        /**
         * Get date of birth
         */
        fun getDate(context: Context): String = getPreferences().getDate(context)

        fun deleteDate(context: Context) {
            if (BuildConfig.DEBUG) {
                getPreferences().deleteDate(context)
                return
            }

            assert(false) { "Only usable in debug" }
        }

        /**
         * Request unique device ID from Preferences.
         *
         * @return Unique device ID
         */
        fun getDeviceId(context: Context): String = getPreferences().getDeviceId(context)
        /**
         * Check if app does not have permission to ask for location access again.
         *
         * @return True if we should not ask for permission again, false otherwise.
         */
        fun dontAskForLocationPermission(context: Context): Boolean = getPreferences().dontAskForLocationPermission(context)

        /**
         * Set preference to ask for location permission for a second time.
         *
         * @param[b] Preference to be set.
         */
        fun setDontAskForLocation(context: Context, b: Boolean)  = getPreferences().setDontAskForLocation(context, b)

        /**
         * Check if the user has landed on the main screen for the first time.
         *
         * @return True if user has landed on the main screen for the first time, false otherwise.
         */
        fun isFirstland(context: Context): Boolean = getPreferences().isFirstland (context)

        fun markFirstLand(context: Context) =  getPreferences().markFirstLand (context)

        /**
         * Check if user has previously activated Bluetooth.
         *
         * @return True if user has previously activated Bluetooth, false otherwise.
         */
        fun didUserStartBluetooth(context: Context): Boolean = getPreferences().didUserStartBluetooth (context)

        /**
         * Check if user has previously activated location.
         *
         * @return True if user has previously activated location, false otherwise.
         */
        fun didUserStartGPS(context: Context): Boolean = getPreferences().didUserStartGPS(context)

        /**
         * Change preference for user having previously activated Bluetooth.
         *
         * @param[b] Preference to be set.
         */
        fun setDidUserStartBTService(context: Context, b: Boolean) = getPreferences().setDidUserStartBTService(context, b)

        /**
         * Change preference for user having previously activated location.
         *
         * @param[b] Preference to be set.
         */
        fun setDidUserStartGPSService(context: Context, b: Boolean) = getPreferences().setDidUserStartGPSService(context, b)

        /**
         * Validate provision response and store connection string.
         *
         * @return True if the connection string is valid, false otherwise.
         */
        fun registerDevice(context: Context, json: JSONObject): Boolean = getPreferences().registerDevice(context, json)

        /**
         * Stores user phone number that has been used for sign in.
         *
         * @param[token] User-specific authentication token from registration.
         */
        fun storePhoneNumber(context: Context, token: String) = getPreferences().storePhoneNumber(context, token)

        /**
         * Get user phone number from Preferences.
         *
         * @return User phone number.
         */
        fun getPhoneNumber(context: Context): String = getPreferences().getPhoneNumber(context)
        /**
         * Get connection string for IoT Hub.
         *
         * @return Connection string for Iot Hub, empty string if not provisioned.
         */
        fun IoTConnectionString(context: Context) : String = getPreferences().IoTConnectionString(context)

        /**
         * Get device ID of provisioned phone.
         *
         * @return Device ID of provisioned phone, empty string if not provisioned.
         */
        fun getProvisionDeviceId(context:Context): String =  getPreferences().getProvisionDeviceId(context)

        /**
         * Get authentication token from Preferences.
         *
         * @return Authentication token from Preferences.
         */
        fun getToken(context: Context): String = getPreferences().getToken(context)

        /**
         * Get timestamp from Preferences.
         *
         * @return Timestamp from Preferences.
         */
        fun getTimestamp(context: Context): Long = getPreferences().getTimestamp(context)
        /**
         * Check if phone is provisioned by checking that the provisioned device ID is not blank.
         *
         * @return True if provisioned, false otherwise.
         */
        fun isProvisioned(context:Context): Boolean {
            return   getProvisionDeviceId(context)?.isNotBlank()!!
        }

        /**
         * Check if phone is authenticated by checking provisioning or token expiry.
         *
         * @return True if authenticated, false otherwise.
         */
        fun isAuthenticated(context: Context)  : Boolean{

            if( isProvisioned(context)){ // If device is provisioned then it is also authenticated
                return true
            }

            // if device is only authenticated then it MUST not have expired token
            return getToken(context).isNotBlank() && (isTokenExpired(context) == false)
        }

        /**
         * Check if non blank token exists.
         *
         * @return True if token is present, false otherwise.
         */

        fun hasToken(context: Context):  Boolean {
            return  getToken(context).isNotBlank()
        }

        /**
         * Check if authentication token is expired.
         *
         * @return True if token is expired, false otherwise.
         */
        fun isTokenExpired(context: Context)  : Boolean {
            // token authentication lasts 12 hours
            val HOURS : Long = 12
            val TWELVE_HRS_IN_MILLISEC : Long =  HOURS * 60 * 60 * 1000

            // * val TWELVE_HRS_IN_MILLISEC : Long =  2 * 60 * 1000 // ONLY FOR TESTING

            val timestamp = getTimestamp(context)
            val delta  = System.currentTimeMillis() -  timestamp

            // token is expired if delta is larger than 12 hours
            return delta >= TWELVE_HRS_IN_MILLISEC
        }

        /**
         * Check if phone needs to be re-authenticated. If a phone has an expired token and is not
         * provisioned, it needs to be re-authenticated.
         *
         * @return True if phone needs to be re-authenticated, false otherwise.
         */
        fun needReAuthentication(context: Context): Boolean {
            val hasToken = getToken(context).isNotEmpty()
            val expired = isTokenExpired(context)
            val hasProvision = getProvisionDeviceId(context).isNotEmpty()

            return hasToken && expired && !hasProvision
        }

        /**
         * Save token into Preferences.
         *
         * @param[token] Token to be saved into Preferences.
         */
        fun saveToken(context: Context, token: String) = getPreferences().saveToken(context, token)
        /**
         * Delete Preferences data.
         */
        fun deleteLocalData(context: Context) = getPreferences().deleteLocalData(context)

        /**
         * Remove device credentials. This involves deleting the connection string, device ID,
         * phone, token, and timestamp from Preferences.
         */
        fun removeCredentials(context: Context) = getPreferences().removeCredentials(context)


        fun setDeviceModelFixApplied(context: Context, success: Boolean) = getPreferences().setDeviceModelFixApplied(context, success )
        fun isDeviceModelFixApplied(context: Context): Boolean = getPreferences().isDeviceModelFixApplied( context )

        /**
         * Check if internet connection is available.
         *
         * @return True if internet connection is available, false otherwise.
         */
        fun isNetworkAvailable(context: Context?): Boolean {
            if (context == null) {
                return false
            }
            var connected = false
            var connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val netInfo = connectivityManager.activeNetworkInfo
            if (netInfo != null) {
                connected = netInfo.isConnected
            }
            return connected
        }

        /**
         * Get current time in seconds.
         *
         * @return Current time in seconds.
         */
        fun unixtime(): Long {
            return System.currentTimeMillis() / 1000
        }

        /**
         * Convert timestamp to unix time in seconds.
         *
         * @param[time] Timestamp in milliseconds
         * @return Timestamp in seconds
         */
        fun toUnixTime(time: Long): Long {
            return time / 1000
        }

        /**
         * Convert timestamp to epoch time in milliseconds.
         *
         * @param[time] Timestamp in seconds
         * @return Timestamp in milliseconds
         */
        fun toEpochTime(time: Long): Long {
            return time * 1000
        }

        fun BluetoothGattCharacteristic.isDeviceIdentifier(): Boolean =
            this.uuid == DEVICE_CHARACTERISTIC_UUID

        // UUID for Bluetooth service
        val SMITTESTOPP_SERVICE_UUID: UUID = UUID.fromString(
            "SMITTESTOPP_SERVICE_UUID"
        )
        // UUIC for device characteristic
        val DEVICE_CHARACTERISTIC_UUID: UUID = UUID.fromString(
            "DEVICE_CHARACTERISTIC_UUID"
        )

        val FINE_LOCATION_PERMISSION = 1


        fun changeHtmlTextColor(html: String, color: String) :  String {
            return "<html><head><style type=\"text/css\">body{color: $color;}</style></head><body>$html</body></html>"
        }

        fun isNorwegianLanguageDefault() : Boolean {
            return Locale.getDefault().language == "no" || Locale.getDefault().language == "nb" || Locale.getDefault().language == "nn"
        }

    }
}


