package no.simula.corona.data.model


import android.location.Location
import android.os.Build
import no.simula.corona.Utils
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

data class Measurement(
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    val latLongAccuracy: Double?,
    val altitude: Double?,
    val altitudeAccuracy: Double?,
    val speed: Double?,
    val speedAccuracy: Double?,
    val isUploaded: Boolean = false
) {


    companion object {


        fun fromEntity(entity: no.simula.corona.data.greendao.Measurement): Measurement {

            var model = Measurement(
                entity.timestamp,
                entity.latitude,
                entity.longitude,
                entity.latLongAccuracy,
                entity.altitude,
                entity.altitudeAccuracy,
                entity.speed,
                entity.speedAccuracy,
                entity.isUploaded
            )

            model.id = entity.id
            model.timeFrom = entity.timestamp
            model.timeTo = entity.timestamp

            return model
        }


        fun fromEntity(entity: no.simula.corona.data.legacy.Measurement): Measurement {

            var model = Measurement(
                entity.timestamp,
                entity.latitude,
                entity.longitude,
                entity.latLongAccuracy,
                entity.altitude,
                entity.altitudeAccuracy,
                entity.speed,
                entity.speedAccuracy,
                entity.isUploaded
            )

            model.id = entity.id?.toLong()
            model.timeFrom = entity.timestamp
            model.timeTo = entity.timestamp

            return model
        }


        fun fromEntityGreenDao(measurements: List<no.simula.corona.data.greendao.Measurement>): List<Measurement> {
            var mapped = ArrayList<Measurement>()

            measurements.map {
                mapped.add(fromEntity(it))
            }

            return mapped
        }

        fun fromEntity(measurements: List<no.simula.corona.data.legacy.Measurement>): List<Measurement> {
            var mapped = ArrayList<Measurement>()

            measurements.map {
                mapped.add(fromEntity(it))
            }

            return mapped
        }


        fun toEntity(measurement: Measurement): no.simula.corona.data.legacy.Measurement {
            return no.simula.corona.data.legacy.Measurement(
                measurement.timestamp,
                measurement.latitude,
                measurement.longitude,
                measurement.latLongAccuracy,
                measurement.altitude,
                measurement.altitudeAccuracy,
                measurement.speed,
                measurement.speedAccuracy,
                measurement.isUploaded
            )
        }

        fun toEntityGreenDao(measurement: Measurement): no.simula.corona.data.greendao.Measurement {
            return no.simula.corona.data.greendao.Measurement(
                null, measurement.timestamp,
                measurement.latitude,
                measurement.longitude,
                measurement.latLongAccuracy,
                measurement.altitude,
                measurement.altitudeAccuracy,
                measurement.speed,
                measurement.speedAccuracy,
                measurement.isUploaded
            )
        }


        fun toEntityGreenDao(measurements: List<Measurement>): List<no.simula.corona.data.greendao.Measurement> {
            var mapped = ArrayList<no.simula.corona.data.greendao.Measurement>()

            measurements.map {
                mapped.add(toEntityGreenDao(it))
            }

            return mapped
        }

    }

    var id: Long? = null

    var timeFrom: Long = timestamp
    var timeTo: Long = timestamp

    fun toJson(): JSONObject {
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        df.timeZone = TimeZone.getTimeZone("UTC")

        val obj = JSONObject()
        obj.put("timeFrom", df.format(Utils.toEpochTime(timeFrom)))
        obj.put("timeTo", df.format(Utils.toEpochTime(timeTo)))
        obj.put("latitude", latitude)
        obj.put("longitude", longitude)
        obj.put("accuracy", latLongAccuracy)
        obj.put("altitude", altitude)
        obj.put("altitudeAccuracy", altitudeAccuracy)
        obj.put("speed", speed)
        obj.put("speedAccuracy", speedAccuracy)

        return obj
    }

}


class MeasurementBuilder(private val timestamp: Long) {
    private var latitude = 0.0
    private var longitude = 0.0
    private var gpsAccuracy = 0.0

    private var altitude = 0.0
    private var altitudeAccuracy = 0.0

    private var speed = 0.0
    private var speedAccuracy = 0.0

    fun setGpsLocation(location: Location) {
        latitude = location.latitude
        longitude = location.longitude
        gpsAccuracy = location.accuracy.toDouble()

        altitude = location.altitude
        speed = location.speed.toDouble()

        if (Build.VERSION.SDK_INT >= 26) {
            altitudeAccuracy = location.verticalAccuracyMeters.toDouble()
            speedAccuracy = location.speedAccuracyMetersPerSecond.toDouble()
        } else {
            altitudeAccuracy = -1.0
            speedAccuracy = -1.0
        }
    }

    fun toEntity(): Measurement {
        return Measurement(
            timestamp,
            latitude, longitude, gpsAccuracy,
            altitude, altitudeAccuracy,
            speed, speedAccuracy
        )
    }
}
