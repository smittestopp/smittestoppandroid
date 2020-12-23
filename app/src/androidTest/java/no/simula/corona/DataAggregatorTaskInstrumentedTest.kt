package no.simula.corona

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import no.simula.corona.data.greendao.GreenDaoDatabaseImpl
import no.simula.corona.data.model.BluetoothContact
import no.simula.corona.data.model.Measurement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.*


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class DataAggregatorTaskInstrumentedTest : BaseInstrumentedTest() {

    lateinit var dbContract: GreenDaoDatabaseImpl

    override fun setup() {
        dbContract = GreenDaoDatabaseImpl(InstrumentationRegistry.getInstrumentation().targetContext)
        dbContract.deleteEverythingGPS()
        dbContract.deleteEverythingBluetooth()
    }

    @Test
    fun dataAggregator_aggregation_isSuccess() {

        var jsonChunk : JsonChunks? = null

        val time = Date().time/1000
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        df.timeZone = TimeZone.getTimeZone("UTC")
        val timeString = df.format(Utils.toEpochTime(time))

        val bluetoothContact =
            BluetoothContact(timestamp = time, deviceId = "device_id_test", rssi = 20, txPower = 10)
        val measurement = Measurement(
            timestamp = time,
            latitude = 78.66,
            longitude = 20.99,
            latLongAccuracy = 1.00,
            altitude = 5.00,
            altitudeAccuracy = 0.50,
            speed = 50.00,
            speedAccuracy = 0.30,
            isUploaded = false
        )

        dbContract.insertBluetooth(bluetoothContact)
        dbContract.insertGPS(measurement)

        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        DataAggregatorTask(appContext, object : DataAggregatorListener {
            override fun onJSonDataPrepared(chunks: JsonChunks?) {
                jsonChunk = chunks
            }
        }).execute()

        Thread.sleep(1000)

        assertNotNull(jsonChunk)
        assertNotNull(jsonChunk?.bluetooth)
        val bluetoothEvent = jsonChunk?.bluetooth?.get(0)?.second?.getJSONArray("events")?.getJSONObject(0)
        assertEquals(1, jsonChunk?.bluetooth?.get(0)?.second?.getJSONArray("events")?.length())
        assertEquals(timeString, bluetoothEvent?.get("time"))
        assertEquals("device_id_test", bluetoothEvent?.get("deviceId"))
        assertEquals(20, bluetoothEvent?.get("rssi"))
        assertEquals(10, bluetoothEvent?.get("txPower"))


        assertNotNull(jsonChunk?.gps)
        val gpsEvent = jsonChunk?.gps?.get(0)?.second?.getJSONArray("events")?.getJSONObject(0)
        //assertEquals(1, jsonChunk?.gps?.get(0)?.second?.getJSONArray("events")?.length())
        assertEquals(timeString, gpsEvent?.get("timeFrom"))
        assertEquals(78.66, gpsEvent?.get("latitude"))
        assertEquals(20.99, gpsEvent?.get("longitude"))
        assertEquals(1.00, gpsEvent?.get("accuracy"))
        assertEquals(5.00, gpsEvent?.get("altitude"))
        assertEquals(0.50, gpsEvent?.get("altitudeAccuracy"))
        assertEquals(50.00, gpsEvent?.get("speed"))
        assertEquals(0.30, gpsEvent?.get("speedAccuracy"))
    }

    override fun cleanup() {
        dbContract.deleteEverythingGPS()
        dbContract.deleteEverythingBluetooth()
        dbContract.close()
    }
}
