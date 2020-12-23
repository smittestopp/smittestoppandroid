package no.simula.corona.security

import android.content.Context
import android.util.Base64

import de.adorsys.android.securestoragelibrary.SecurePreferences
import de.adorsys.android.securestoragelibrary.SecureStorageException
import timber.log.Timber
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object SecretValueGenerator {

    private const val DEFAULT_KEY_LENGTH = 256
    private const val KEY_ALIAS_DB = "alias_db"
    private const val KEY_ALIAS_PREFS = "alias_prefs"
    const val UNSUPPORTED = "ksNotSupportedOnDevice"


    private var securedValuedDb: String? = null
    private var securedValuePrefs: String? = null


    fun prepare(appContext: Context) {
        getSecureValueDB(appContext)
        getSecureValuePrefs(appContext)
    }

    fun getSecureValueDB(context: Context?): String? {

        if (Device.isProblematicModel()) {
            return UNSUPPORTED
        }

        if (securedValuedDb == null) {
            securedValuedDb =
                getSecureValue(
                    context,
                    KEY_ALIAS_DB
                )
        }
        return securedValuedDb
    }

    fun getSecureValuePrefs(context: Context?): String? {
        if (securedValuePrefs == null) {
            securedValuePrefs =
                getSecureValue(
                    context,
                    KEY_ALIAS_PREFS
                )
        }
        return securedValuePrefs
    }

    private fun getSecureValue(context: Context?, alias: String?): String? {


        var securedValue: String? = null

        var start = System.currentTimeMillis()

        try {
            securedValue = SecurePreferences.getStringValue(context!!, alias!!, "")
            if (securedValue?.isEmpty() == true) {
                securedValue =
                    generateKey()
                if (securedValue?.isNotEmpty() == true) {
                    SecurePreferences.setValue(context, alias, securedValue!!)
                }
            }
        } catch (ex: SecureStorageException) {
            when (ex.type) {
                //If KEYSTORE_NOT_SUPPORTED_EXCEPTION exception type is set it means simply that
                // the keystore cannot be used on the current device as it is not supported by this
                // library. This probably means that you are targeting a device prior to api 23 or
                // any not supported fingerprint sensors of Samsung
                SecureStorageException.ExceptionType.KEYSTORE_NOT_SUPPORTED_EXCEPTION,
                    //If KEYSTORE_EXCEPTION exception type is found then you cannot use the
                    // keystore / this library on the current device. This is fatal and most
                    // likely due to native key store issues.
                SecureStorageException.ExceptionType.KEYSTORE_EXCEPTION -> {
                    securedValue = UNSUPPORTED
                }
                else -> {
                    Timber.e(ex)
                }
            }
        } catch (ex: Exception) {
            Timber.e(ex)
        }

        var end = System.currentTimeMillis()

        Timber.d("Time taken ${end - start} milliseconds key length ${securedValue?.length}")

        return securedValue
    }

    private fun generateKey(): String? {
        try {
            val secretKey =
                internalGenerateKey()
            return Base64.encodeToString(
                secretKey.encoded,
                Base64.NO_WRAP
            )
        } catch (ex: Exception) {
            Timber.e(ex)
        }
        return null
    }

    @Throws(NoSuchAlgorithmException::class)
    private fun internalGenerateKey(): SecretKey {
        val random = SecureRandom()
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(DEFAULT_KEY_LENGTH, random)
        return keyGenerator.generateKey()
    }

}