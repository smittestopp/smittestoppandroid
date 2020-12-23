package no.simula.corona.data.legacy

import android.content.Context
import android.location.Location
import android.os.Build
import androidx.room.*
import no.simula.corona.Utils.Companion.toEpochTime
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


@Entity
data class Measurement(
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "latitude") val latitude: Double?,
    @ColumnInfo(name = "longitude") val longitude: Double?,
    @ColumnInfo(name = "latLongAccuracy") val latLongAccuracy: Double?,
    @ColumnInfo(name = "altitude") val altitude: Double?,
    @ColumnInfo(name = "altitudeAccuracy") val altitudeAccuracy: Double?,
    @ColumnInfo(name = "speed") val speed: Double?,
    @ColumnInfo(name = "speedAccuracy") val speedAccuracy: Double?,
    @ColumnInfo(name = "isUploaded") val isUploaded: Boolean = false
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
}

@Entity
data class BluetoothContact(
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "deviceId") val deviceId: String,
    @ColumnInfo(name = "rssi") val rssi: Int,
    @ColumnInfo(name = "txPower") val txPower: Int,
    @ColumnInfo(name = "isUploaded") val isUploaded: Boolean = false
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
}

@Dao
interface MeasurementDao {
    @Query("SELECT * from measurement")
    fun allMeasurements(): List<Measurement>

    @Query("SELECT * from measurement WHERE isUploaded is 0")
    fun notUploaded(): List<Measurement>

    @Query("DELETE from measurement WHERE isUploaded is 1")
    fun deleteUploaded()

    @Insert
    fun insertAll(vararg users: Measurement)

    @Query("UPDATE measurement SET isUploaded = 1 WHERE id in (:ids)")
    fun markAsUploaded(ids: List<Int>)

    @Query("DELETE FROM measurement")
    fun deleteEverything()
}

@Dao
interface BluetoothContactDao {
    @Query("SELECT * from bluetoothcontact")
    fun allBluetoothContacts(): List<BluetoothContact>

    @Query("SELECT * from bluetoothcontact WHERE isUploaded is 0")
    fun notUploaded(): List<BluetoothContact>

    @Query("DELETE from bluetoothcontact WHERE isUploaded is 1")
    fun deleteUploaded()

    @Insert
    fun insertAll(vararg users: BluetoothContact)

    @Query("UPDATE bluetoothcontact SET isUploaded = 1 WHERE id in (:ids)")
    fun markAsUploaded(ids: List<Int>)

    @Query("DELETE from bluetoothcontact")
    fun deleteEverything()
}

@Database(
    entities = [Measurement::class, BluetoothContact::class],
    version = 7,
    exportSchema = false
)
abstract class MeasurementDatabase : RoomDatabase() {
    abstract fun measurementDao(): MeasurementDao
    abstract fun bluetoothContactDao(): BluetoothContactDao

    companion object {

        var DB_FILENAME = "measurement_database"

        @Volatile
        private var INSTANCE: MeasurementDatabase? = null

        fun get(context: Context): MeasurementDatabase {
            val tmp =
                INSTANCE
            if (tmp != null) {
                return tmp
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MeasurementDatabase::class.java,
                    DB_FILENAME
                ).allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                return instance
            }
        }
    }


}
