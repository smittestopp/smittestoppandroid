package no.simula.corona.data.model

import no.simula.corona.Utils
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

data class BluetoothContact(
    val timestamp: Long,
    val deviceId: String,
    val rssi: Int,
    val txPower: Int,
    val isUploaded: Boolean = false
) {

    companion object {

        fun fromEntity(entity: no.simula.corona.data.legacy.BluetoothContact): BluetoothContact {
            var model = BluetoothContact(
                entity.timestamp,
                entity.deviceId,
                entity.rssi,
                entity.txPower,
                entity.isUploaded
            )

            model.id = entity.id?.toLong()

            return model
        }


        fun fromEntity(entity: no.simula.corona.data.greendao.BluetoothContact): BluetoothContact {
            var model = BluetoothContact(
                entity.timestamp,
                entity.deviceId,
                entity.rssi,
                entity.txPower,
                entity.isUploaded
            )

            model.id = entity.id

            return model
        }

        fun toEntity(bluetoothContact: BluetoothContact): no.simula.corona.data.legacy.BluetoothContact {
            return no.simula.corona.data.legacy.BluetoothContact(
                bluetoothContact.timestamp,
                bluetoothContact.deviceId,
                bluetoothContact.rssi,
                bluetoothContact.txPower,
                bluetoothContact.isUploaded
            )
        }

        fun fromEntity(measurements: List<no.simula.corona.data.legacy.BluetoothContact>): List<BluetoothContact> {
            var mapped = ArrayList<BluetoothContact>()

            measurements.map {
                mapped.add(BluetoothContact.fromEntity(it))
            }

            return mapped
        }

        fun fromEntityGreenDao(models: List<no.simula.corona.data.greendao.BluetoothContact>): List<BluetoothContact> {

            var mapped = ArrayList<BluetoothContact>()

            models.map {
                mapped.add(BluetoothContact.fromEntity(it))
            }

            return mapped

        }

        fun toEntityGreenDao(model: BluetoothContact): no.simula.corona.data.greendao.BluetoothContact {

            return no.simula.corona.data.greendao.BluetoothContact(
                null, model.timestamp,
                model.deviceId,
                model.rssi,
                model.txPower,
                model.isUploaded
            )

        }

        fun toEntityGreenDao(models: List<BluetoothContact>): List<no.simula.corona.data.greendao.BluetoothContact> {

            var mapped = ArrayList<no.simula.corona.data.greendao.BluetoothContact>()

            models.map {
                mapped.add(toEntityGreenDao(it))
            }

            return mapped

        }
    }

    var id: Long? = null

    fun toJson(): JSONObject {
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        df.timeZone = TimeZone.getTimeZone("UTC")

        val obj = JSONObject()
        obj.put("time", df.format(Utils.toEpochTime(timestamp)))
        obj.put("deviceId", deviceId)
        obj.put("rssi", rssi)
        obj.put("txPower", txPower)

        return obj
    }
}

class BluetoothContactBuilder(private val timestamp: Long) {
    private var deviceId = ""
    private var rssi = 0
    private var txPower = 0


    fun setCloseContact(contactId: String, contactRssi: Int, contactTx: Int) {
        deviceId = contactId
        rssi = contactRssi
        txPower = contactTx

    }

    fun toEntity(): BluetoothContact {
        return BluetoothContact(
            timestamp,
            deviceId, rssi, txPower
        )
    }
}