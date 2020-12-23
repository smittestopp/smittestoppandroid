package no.simula.corona.data

import android.content.Context
import org.json.JSONObject

open abstract class PreferencesBase {
    /**
     * Stores app preferences and parameters.
     */

    protected val settings = "settings"
    protected val settingV2 = "safe"

    protected val hasConsented = "consented"
    protected val dontAskAgain = "dont-ask-again"
    protected val didUserStartBtService = "did-user-start-bt"
    protected val didUserStartGPSService = "did-user-start-loc"
    protected val isMigrated = "is-migrated"
    protected val deviceModelDbFix = "device-model-db-fix"
    protected val isSecuredValueChanged = "secured-value-changed"

    object Secured {
        val hostName = "host-name"
        val accessKey = "access-key"
        val connectionString = "connection-data"
        val deviceId = "device-id-string"
        val token = "token"
        val phone = "phone-number"
        val timestamp = "timestamp"
        val firstLand = "firstland"
        val dateOfBirth = "2j2j2k2"
    }

    /**
     * Check if user has given consent to the privacy policy.
     *
     * @return True if user has given consent, false otherwise.
     */
    abstract fun hasGivenConsent(context: Context): Boolean

    /**
     * Set consent preference.
     *
     * @param[b] Consent preference to be set.
     */
    abstract fun setConsent(context: Context, b: Boolean)

    abstract fun setDate(context: Context, a: String)

    abstract fun getDate(context: Context): String

    abstract fun deleteDate(context: Context)

    /**
     * Request unique device ID from Preferences.
     *
     * @return Unique device ID
     */
    abstract fun getDeviceId(context: Context): String

    /**
     * Check if app does not have permission to ask for location access again.
     *
     * @return True if we should not ask for permission again, false otherwise.
     */
    abstract fun dontAskForLocationPermission(context: Context): Boolean

    /**
     * Set preference to ask for location permission for a second time.
     *
     * @param[b] Preference to be set.
     */
    abstract fun setDontAskForLocation(context: Context, b: Boolean)

    /**
     * Check if the user has landed on the main screen for the first time.
     *
     * @return True if user has landed on the main screen for the first time, false otherwise.
     */
    abstract fun isFirstland(context: Context): Boolean

    abstract fun markFirstLand(context: Context)

    /**
     * Check if user has previously activated Bluetooth.
     *
     * @return True if user has previously activated Bluetooth, false otherwise.
     */
    abstract fun didUserStartBluetooth(context: Context): Boolean

    /**
     * Check if user has previously activated location.
     *
     * @return True if user has previously activated location, false otherwise.
     */
    abstract fun didUserStartGPS(context: Context): Boolean

    /**
     * Change preference for user having previously activated Bluetooth.
     *
     * @param[b] Preference to be set.
     */
    abstract fun setDidUserStartBTService(context: Context, b: Boolean)

    /**
     * Change preference for user having previously activated location.
     *
     * @param[b] Preference to be set.
     */
    abstract fun setDidUserStartGPSService(context: Context, b: Boolean)

    /**
     * Validate provision response and store connection string.
     *
     * @return True if the connection string is valid, false otherwise.
     */
    abstract fun registerDevice(context: Context, json: JSONObject): Boolean

    /**
     * Set user phone number in Preferences.
     *
     * @param[number] User phone number.
     */
    abstract fun setPhoneNumber(context: Context, number: String)

    /**
     * Stores user phone number that has been used for sign in.
     *
     * @param[token] User-specific authentication token from registration.
     */
    abstract fun storePhoneNumber(context: Context, token: String)


    /**
     * Get user phone number from Preferences.
     *
     * @return User phone number.
     */
    abstract fun getPhoneNumber(context: Context): String

    /**
     * Get connection string for IoT Hub.
     *
     * @return Connection string for Iot Hub, empty string if not provisioned.
     */
    abstract fun IoTConnectionString(context: Context): String

    /**
     * Get device ID of provisioned phone.
     *
     * @return Device ID of provisioned phone, empty string if not provisioned.
     */
    abstract fun getProvisionDeviceId(context: Context): String

    /**
     * Get authentication token from Preferences.
     *
     * @return Authentication token from Preferences.
     */
    abstract fun getToken(context: Context): String

    /**
     * Get timestamp from Preferences.
     *
     * @return Timestamp from Preferences.
     */
    abstract fun getTimestamp(context: Context): Long

    /**
     * Save token into Preferences.
     *
     * @param[token] Token to be saved into Preferences.
     */
    abstract fun saveToken(context: Context, token: String)

    /**
     * Delete Preferences data.
     */
    abstract fun deleteLocalData(context: Context)

    /**
     * Remove device credentials. This involves deleting the connection string, device ID,
     * phone, token, and timestamp from Preferences.
     */
    abstract fun removeCredentials(context: Context)
    abstract fun setDeviceModelFixApplied(context: Context, success: Boolean)
    abstract fun isDeviceModelFixApplied(context: Context): Boolean

}