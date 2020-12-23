package no.simula.corona.security

import android.content.Context
import android.os.Build
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import com.scottyab.rootbeer.Const
import com.scottyab.rootbeer.RootBeer
import no.simula.corona.BuildConfig
import timber.log.Timber

object Device {
    fun isEmulator(): Boolean {
        if (BuildConfig.DEBUG) {
            return false
        }

        if (Build.FINGERPRINT.startsWith("generic")) {
            emulatorFailed("fingerprint generic")
            return true
        }

        if (Build.FINGERPRINT.startsWith("unknown")) {
            emulatorFailed("fingerprint unknown")
            return true
        }

        if (Build.MODEL.contains("google_sdk")) {
            emulatorFailed("model contain google_sdk")
            return true
        }

        if (Build.MODEL.contains("Emulator")) {
            emulatorFailed("model contains Emulator")
            return true
        }

        if (Build.MODEL.contains("Android SDK built for x86")) {
            emulatorFailed("model contains Android SDK built for x86")
            return true
        }

        if (Build.MANUFACTURER.contains("Genymotion")) {
            emulatorFailed("manufacturer contains Genymotion")
            return true
        }

        if ("google_sdk" == Build.PRODUCT) {
            emulatorFailed("product is google_sdk")
            return true
        }

        return false
    }

    fun isRooted(context: Context): Boolean {
        if (BuildConfig.DEBUG) {
            return false
        }

        val root = RootBeer(context)

        if (root.detectRootManagementApps()) {
            rootFailed("root:detectRootManagementApps failed")
            return true
        }

        if (root.checkForBinary(Const.BINARY_SU)) {
            rootFailed("root:checkForBinarySu fialed")
            return true
        }

        if (root.checkForDangerousProps()) {
            rootFailed("root:checkForDangerousProps failed")
            return true
        }

        if (root.checkForRWPaths()) {
            rootFailed("root:checkForRWPaths failed")
            return true
        }

        if (root.checkForRootNative()) {
            rootFailed("root:checkForRootNative failed")
            return true
        }

        if (root.checkForMagiskBinary()) {
            rootFailed("root:checkForMagiskBinary failed")
            return true
        }

        return false
    }

    private fun rootFailed(event: String) {
        Analytics.trackEvent(
            "Root detection failed",
            EventProperties().apply { set("test", event) })
        Timber.d(event)
    }

    private fun emulatorFailed(event: String) {
        Analytics.trackEvent(
            "Emulator detection failed",
            EventProperties().apply { set("test", event) })
        Timber.d(event)
    }

    fun isProblematicModel(): Boolean {

        var models = arrayListOf<String>(
            "SM-G960F",
            "SM-G965F",
            "SM-A750FN",
            "SM-A520F",
            "SM-J600FN",
            "SM-N960F",
            "SM-G930F",
            "SM-G398FN",
            "SM-G935F",
            "GT-I9506",
            "SM-A600FN",
            "SM-G903F"
        )

        for (model in models) {
            if (model == Build.MODEL.toUpperCase()) {
                return true
            }
        }
        return false
    }

}