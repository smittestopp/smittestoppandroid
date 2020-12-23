package no.simula.corona.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import com.securepreferences.SecurePreferences
import no.simula.corona.BuildConfig
import no.simula.corona.CoronaApp
import no.simula.corona.IoTHubDevice
import no.simula.corona.SingletonHolder
import no.simula.corona.security.Device
import no.simula.corona.security.SecretValueGenerator
import org.json.JSONObject
import timber.log.Timber
import java.sql.Time


class Preferences : PreferencesBase() {

    companion object {
        var secureSharedPreferences: SharedPreferences? = null
    }

    /**
     * Check if user has given consent to the privacy policy.
     *
     * @return True if user has given consent, false otherwise.
     */
    override fun hasGivenConsent(context: Context): Boolean {
        val preferences = context.getSharedPreferences(
            settings,
            Context.MODE_PRIVATE
        )
        return preferences.getBoolean(hasConsented, false)
    }

    /**
     * Set consent preference.
     *
     * @param[b] Consent preference to be set.
     */
    override fun setConsent(context: Context, b: Boolean) {
        val preferences = context.getSharedPreferences(
            settings,
            Context.MODE_PRIVATE
        )
        val editor = preferences.edit()
        editor.putBoolean(hasConsented, b)
        editor.apply()
    }

    override fun setDate(context: Context, a: String) {
        val prefs = getSecuredPreference(context)
        val editor = prefs.edit()
        editor.putString(Secured.dateOfBirth, a)
        editor.apply()
    }

    override fun getDate(context: Context): String {
        val prefs = getSecuredPreference(context)
        return prefs.getString(Secured.dateOfBirth, "") ?: ""
    }

    override fun deleteDate(context: Context) {
        if (BuildConfig.DEBUG) {
            val prefs = getSecuredPreference(context)
            val editor = prefs.edit()
            editor.remove(Secured.dateOfBirth)
            editor.apply()
        }
    }

    /**
     * Check if app does not have permission to ask for location access again.
     *
     * @return True if we should not ask for permission again, false otherwise.
     */
    override fun dontAskForLocationPermission(context: Context): Boolean {
        val preferences = context.getSharedPreferences(
            settings,
            Context.MODE_PRIVATE
        )
        return preferences.getBoolean(dontAskAgain, false)
    }

    /**
     * Set preference to ask for location permission for a second time.
     *
     * @param[b] Preference to be set.
     */
    override fun setDontAskForLocation(context: Context, b: Boolean) {
        val preferences = context.getSharedPreferences(
            settings,
            Context.MODE_PRIVATE
        )
        val editor = preferences.edit()
        editor.putBoolean(dontAskAgain, b)
        editor.apply()
    }


    fun isMigrated(context: Context): Boolean {
        val preferences = context.getSharedPreferences(
            settings,
            Context.MODE_PRIVATE
        )
        return preferences.getBoolean(isMigrated, false)
    }

    fun markMigrated(context: Context) {
        val preferences = context.getSharedPreferences(
            settings,
            Context.MODE_PRIVATE
        )
        val editor = preferences.edit()
        editor.putBoolean(isMigrated, true)
        editor.apply()
    }

    private fun isPasswordChanged(context: Context): Boolean {
        val preferences = context.getSharedPreferences(
            settings,
            Context.MODE_PRIVATE
        )
        return preferences.getBoolean(isSecuredValueChanged, false)
    }

    private fun markPasswordChanged(context: Context) {
        val preferences = context.getSharedPreferences(
            settings,
            Context.MODE_PRIVATE
        )
        val editor = preferences.edit()
        editor.putBoolean(isSecuredValueChanged, true)
        editor.apply()
    }


    fun setMigrated(context: Context) {
        val preferences = context.getSharedPreferences(
            settings,
            Context.MODE_PRIVATE
        )
        val editor = preferences.edit()
        editor.putBoolean(isMigrated, true)
        editor.apply()
    }

    override fun isDeviceModelFixApplied(context: Context): Boolean {
        val preferences = context.getSharedPreferences(
            settings,
            Context.MODE_PRIVATE
        )
        return preferences.getBoolean(deviceModelDbFix, false)
    }

    override fun setDeviceModelFixApplied(context: Context, success: Boolean) {
        val preferences = context.getSharedPreferences(
            settings,
            Context.MODE_PRIVATE
        )
        val editor = preferences.edit()
        editor.putBoolean(deviceModelDbFix, success)
        editor.apply()
    }

    /**
     * Check if user has previously activated Bluetooth.
     *
     * @return True if user has previously activated Bluetooth, false otherwise.
     */
    override fun didUserStartBluetooth(context: Context): Boolean {
        val preferences = context.getSharedPreferences(
            settings,
            Context.MODE_PRIVATE
        )
        return preferences.getBoolean(didUserStartBtService, true)
    }

    /**
     * Check if user has previously activated location.
     *
     * @return True if user has previously activated location, false otherwise.
     */
    override fun didUserStartGPS(context: Context): Boolean {
        val preferences = context.getSharedPreferences(
            settings,
            Context.MODE_PRIVATE
        )
        return preferences.getBoolean(didUserStartGPSService, true)
    }

    /**
     * Change preference for user having previously activated Bluetooth.
     *
     * @param[b] Preference to be set.
     */
    override fun setDidUserStartBTService(context: Context, b: Boolean) {
        val preferences = context.getSharedPreferences(
            settings,
            Context.MODE_PRIVATE
        )
        val editor = preferences.edit()
        editor.putBoolean(didUserStartBtService, b)
        editor.apply()
    }

    /**
     * Change preference for user having previously activated location.
     *
     * @param[b] Preference to be set.
     */
    override fun setDidUserStartGPSService(context: Context, b: Boolean) {
        val preferences = context.getSharedPreferences(
            settings,
            Context.MODE_PRIVATE
        )
        val editor = preferences.edit()
        editor.putBoolean(didUserStartGPSService, b)
        editor.apply()
    }


    /**
     * Stores user phone number that has been used for sign in.
     *
     * @param[token] User-specific authentication token from registration.
     */
    override fun storePhoneNumber(context: Context, token: String) {
        val parts = token.split(".")
        // Check that token has correct format
        if (parts.size == 3) {
            try {
                // decode phone part from token
                val json = Base64.decode(parts[1], Base64.NO_WRAP).toString(Charsets.US_ASCII)
                val phone = JSONObject(json)
                // Phone has phone number
                if (phone.has("signInNames.phoneNumber")) {
                    // Set user phone number
                    setPhoneNumber(context, phone.getString("signInNames.phoneNumber"))
                }
            } catch (e: Exception) {
                // Track error in Appcenter
                val map = mutableMapOf<String, String>().apply {
                    put("where", "storePhoneNumber")
                }
                Crashes.trackError(e, map, null)
            }
        }
    }

    /**
     * Delete Preferences data.
     */
    override fun deleteLocalData(context: Context) {
        val preferences = context.getSharedPreferences(
            settings,
            Context.MODE_PRIVATE
        )
        preferences.edit().clear().apply()

        val secure = getSecuredPreference(context)
        secure.edit().clear().apply()
    }
//%%%%%%%%%%%%%%----------------------------------%%%%%%%%%%%%%%
    /**
     * Get authentication token from Preferences.
     *
     * @return Authentication token from Preferences.
     */
    override fun getToken(context: Context): String {

        val preferences = getSecuredPreference(context)

        var token = preferences.getString(Secured.token, "") ?: ""

        return token
    }

    /**
     * Validate provision response and store connection string.
     *
     * @return True if the connection string is valid, false otherwise.
     */
    override fun registerDevice(context: Context, json: JSONObject): Boolean {
        // JSON keys
        val device = "DeviceId"
        val host = "HostName"
        val key = "SharedAccessKey"
        val conn = "ConnectionString"

        // At least one JSON key is not present in JSON object
        if (!json.has(device) or !json.has(host) or !json.has(key) or !json.has(conn)) {
            // Track invalid provisioning
            Analytics.trackEvent("Invalid provision response")
            return false
        }


        var securePreferences = getSecuredPreference(context)

        val editor = securePreferences.edit()

        // Set preferences object using values from JSON object
        editor.putString(Secured.deviceId, json.getString(device))
        editor.putString(Secured.hostName, json.getString(host))
        editor.putString(Secured.accessKey, json.getString(key))
        editor.putString(Secured.connectionString, json.getString(conn))
        editor.apply()
        return true
    }


    fun getSecuredPreference(context: Context): SharedPreferences {

        if (secureSharedPreferences != null)
            return secureSharedPreferences!!

        var securedValue =
            getSecuredValue(context.applicationContext) // get the correct secured value depending upon the device preferences state

        var securePreferences =
            SecurePreferences(context.applicationContext, securedValue, settingV2)

        if (isMigrated(context.applicationContext) == false) {
            migrateSecureKeys(context.applicationContext, securePreferences)

            if (Device.isProblematicModel()) {
                markPasswordChanged(context)
                Timber.d("first time setup of secured preferences")
            }
        } else {
            if (Device.isProblematicModel() && !isPasswordChanged(context)) {
                Timber.d("changed secured value")
                securePreferences.handlePasswordChange(
                    SecretValueGenerator.UNSUPPORTED,
                    context
                ) // Problematic device models can not have randomly generated secured value
                markPasswordChanged(context)
            }
        }


        secureSharedPreferences = securePreferences

        return secureSharedPreferences!!
    }

    private fun getSecuredValue(appContext: Context): String {

        if (Device.isProblematicModel()) {
            if (isPasswordChanged(appContext) || !isMigrated(appContext)) {
                Timber.d("use fixed secure value")
                return SecretValueGenerator.UNSUPPORTED
            }
        }
        Timber.d("get dynamic secured value")
        return SecretValueGenerator.getSecureValuePrefs(appContext)!!
    }


    private fun migrateSecureKeys(
        context: Context,
        securePreferences: SecurePreferences
    ) {

        val legacyPrefs = context.getSharedPreferences(
            settings,
            Context.MODE_PRIVATE
        )

        var securedKeys = arrayListOf<String>(
            Secured.hostName, Secured.accessKey, Secured.connectionString, Secured.deviceId,
            Secured.token, Secured.phone, Secured.timestamp /*Long*/, Secured.firstLand /*boolean*/
        )

        var securedEditor = securePreferences.edit()
        var unsecuredEditor = legacyPrefs.edit()

        var count = 0

        securedKeys.map { key ->

            if (legacyPrefs.contains(key)) {

                Timber.d(key + " found")

                when (key) {
                    Secured.timestamp -> {
                        securedEditor.putLong(key, legacyPrefs.getLong(key, 0))

                    }
                    Secured.firstLand -> {
                        securedEditor.putBoolean(key, legacyPrefs.getBoolean(key, true))

                    }
                    else ->
                        securedEditor.putString(key, legacyPrefs.getString(key, ""))
                }

                unsecuredEditor.remove(key)

                count++

            }
        }

        securedEditor.apply()
        unsecuredEditor.apply()

        Timber.d("" + count + " keys migrated")

        markMigrated(context)
    }


    /**
     * Remove device credentials. This involves deleting the connection string, device ID,
     * phone, token, and timestamp from Preferences.
     */
    override fun removeCredentials(context: Context) {
        val preferences = getSecuredPreference(context)

        val editor = preferences.edit()
        editor.remove(Secured.connectionString)
        editor.remove(Secured.deviceId)
        editor.remove(Secured.token)
        editor.remove(Secured.firstLand)
        editor.remove(Secured.timestamp)
        editor.remove(Secured.phone)
        editor.remove(Secured.hostName)
        editor.remove(Secured.accessKey)
        editor.remove(Secured.dateOfBirth)
        editor.apply()
    }

    /**
     * Save token into Preferences.
     *
     * @param[token] Token to be saved into Preferences.
     */
    override fun saveToken(context: Context, token: String) {

        val preferences = getSecuredPreference(context)

        val editor = preferences.edit()

        editor.putString(Secured.token, token)
        editor.putLong(Secured.timestamp, System.currentTimeMillis())

        editor.apply()
    }


    /**
     * Get connection string for IoT Hub.
     *
     * @return Connection string for Iot Hub, empty string if not provisioned.
     */
    override fun IoTConnectionString(context: Context): String {
        val preferences = getSecuredPreference(context)

        val con = preferences.getString(Secured.connectionString, "")
        return con ?: ""
    }

    /**
     * Get device ID of provisioned phone.
     *
     * @return Device ID of provisioned phone, empty string if not provisioned.
     */
    override fun getProvisionDeviceId(context: Context): String {
        val preferences = getSecuredPreference(context)

        val deviceId = preferences.getString(Secured.deviceId, "")

        // If phone is provisioned, deviceId is not null or blank
        if (deviceId?.isNullOrBlank() == false) {
            return deviceId!!
        }

        // Else return empty string
        return ""
    }

    /**
     * Request unique device ID from Preferences.
     *
     * @return Unique device ID
     */
    override fun getDeviceId(context: Context): String {
        val preferences = getSecuredPreference(context)

        return preferences.getString(Secured.deviceId, "") ?: ""
    }

    /**
     * Check if the user has landed on the main screen for the first time.
     *
     * @return True if user has landed on the main screen for the first time, false otherwise.
     */
    override fun isFirstland(context: Context): Boolean {
        val preferences = getSecuredPreference(context)

        return preferences.getBoolean(Secured.firstLand, true)
    }

    override fun markFirstLand(context: Context) {
        val preferences = getSecuredPreference(context)
        val editor = preferences.edit()
        editor.putBoolean(Secured.firstLand, false)
        editor.apply()
    }

    /**
     * Set user phone number in Preferences.
     *
     * @param[number] User phone number.
     */
    override fun setPhoneNumber(context: Context, number: String) {
        val preferences = getSecuredPreference(context)

        val editor = preferences.edit()
        editor.putString(Secured.phone, number)
        editor.apply()
    }

    /**
     * Get user phone number from Preferences.
     *
     * @return User phone number.
     */
    override fun getPhoneNumber(context: Context): String {
        val preferences = getSecuredPreference(context)
        return preferences.getString(Secured.phone, "") ?: ""
    }


    /**
     * Get timestamp from Preferences.
     *
     * @return Timestamp from Preferences.
     */
    override fun getTimestamp(context: Context): Long {
        val preferences = getSecuredPreference(context)
        return preferences.getLong(Secured.timestamp, 0)
    }

}