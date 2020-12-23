package no.simula.corona.data

import android.content.Context
import no.simula.corona.BuildConfig
import no.simula.corona.data.greendao.GreenDaoDatabaseImpl
import no.simula.corona.data.legacy.MeasurementDatabase
import timber.log.Timber
import java.io.File

object DatabaseUtils {

    fun deleteOldDatabase(context: Context): Boolean {
        return deleteDatabase(context, MeasurementDatabase.DB_FILENAME)
    }

    fun deleteNewDatabase(context: Context): Boolean {
        return deleteDatabase(context, GreenDaoDatabaseImpl.DB_FILENAME)
    }

    fun databaseExistsOld(context: Context): Boolean {
        return databaseExists(context, MeasurementDatabase.DB_FILENAME)
    }

    fun databaseExistsNew(context: Context): Boolean {
        return databaseExists(context, GreenDaoDatabaseImpl.DB_FILENAME)
    }

    private fun deleteDatabase(context: Context, filename: String): Boolean {

        var retValue = false

        try {
            retValue = context.deleteDatabase(filename)
            if (retValue) {
                Timber.d("database ${filename} file deleted")
            }
        } catch (ex: Exception) {
            Timber.e(ex)
        }

        return retValue
    }

    private fun databaseExists(context: Context, filename: String): Boolean {
        val file: File = context.getDatabasePath(filename)
        var exits = file.exists()
        if (exits) {
            Timber.d("database ${filename} exists")
        }
        return exits
    }

    fun info(database: DatabaseContract) {
        if (BuildConfig.DEBUG) {
            Timber.d("location records  " + database.allGPSMeasurements().size)
            Timber.d(" bluetooth records  " + database.allBluetoothContacts().size)
        }
    }

}