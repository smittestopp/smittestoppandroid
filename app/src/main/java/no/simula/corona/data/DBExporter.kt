package no.simula.corona.data

import android.content.Context
import android.os.Build
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import no.simula.corona.BuildConfig
import no.simula.corona.Utils
import no.simula.corona.data.greendao.GreenDaoDatabaseImpl
import no.simula.corona.data.legacy.LegacyDatabaseImpl
import no.simula.corona.security.Device
import timber.log.Timber

class DBExporter {

    companion object {

        fun export(context: Context) {

            if (Device.isProblematicModel()) {

                if (!Utils.isDeviceModelFixApplied(context)) {
                    if (DatabaseUtils.databaseExistsNew(context)) {
                        var success = DatabaseUtils.deleteNewDatabase(context)
                        Utils.setDeviceModelFixApplied(context, success)
                        if (success) {
                            Timber.d("device model db fix applied")
                            Analytics.trackEvent(
                                "database fixed",
                                EventProperties().apply { set("model", Build.MODEL) })
                        }
                    }
                } else {
                    Timber.d("device model db fix applied already")
                }
            }

            if (!DatabaseUtils.databaseExistsOld(context)) {
                showInfo(context)
                Timber.d("Already exported")
                return
            }

            var legacy = LegacyDatabaseImpl(context)
            var newDatabase = GreenDaoDatabaseImpl(context)

            var dataGps = legacy.allGPSMeasurements()

            if (dataGps.isNotEmpty()) {
                newDatabase.insertGPSAll(dataGps)
                legacy.deleteEverythingGPS()
                Timber.d("location records deleted " + dataGps.size)
            } else {
                Timber.d("no location record found ")
            }

            var dataBt = legacy.allBluetoothContacts()

            if (dataBt.isNotEmpty()) {
                newDatabase.insertBtAll(dataBt)
                legacy.deleteEverythingBluetooth()
                Timber.d("bluetooth records deleted " + dataBt.size)
            } else {
                Timber.d("no bluetooth records found ")
            }

            Timber.d("new location records  " + newDatabase.allGPSMeasurements().size)
            Timber.d("new bluetooth records  " + newDatabase.allBluetoothContacts().size)

            newDatabase.close()

            DatabaseUtils.deleteOldDatabase(context)
        }

        private fun showInfo(context: Context) {

            if (!BuildConfig.DEBUG) {
                return
            }

            var newDatabase = GreenDaoDatabaseImpl(context)
            Timber.d("new location records  " + newDatabase.allGPSMeasurements().size)
            Timber.d("new bluetooth records  " + newDatabase.allBluetoothContacts().size)
            newDatabase.close()
        }
    }


}


