package no.simula.corona

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import no.simula.corona.data.greendao.GreenDaoDatabaseImpl
import no.simula.corona.data.model.BluetoothContact
import no.simula.corona.data.model.Measurement
import org.junit.Test

import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.*

/**
 * Tests green dao db
 */
@RunWith(AndroidJUnit4::class)
class DatabaseContractInstrumentedTest : BaseInstrumentedTest() {

    lateinit var dbContract : GreenDaoDatabaseImpl

    override fun setup() {
        dbContract = GreenDaoDatabaseImpl(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    @Test
    fun dataBase_BluetoothContact_isInserted() {
        val time = Date().time
        val bluetoothContact = BluetoothContact(timestamp = time, deviceId = "device_id_test", rssi = 20, txPower = 10)

        dbContract.insertBluetooth(bluetoothContact)
        val bluetoothContactList = dbContract.allBluetoothContacts()

        assertNotNull(bluetoothContactList)
        assertEquals(1, bluetoothContactList.size)
        assertEquals(time, bluetoothContactList[0].timestamp)
        assertEquals("device_id_test", bluetoothContactList[0].deviceId)
        assertEquals(20, bluetoothContactList[0].rssi)
        assertEquals(10, bluetoothContactList[0].txPower)
        assertEquals(false, bluetoothContactList[0].isUploaded)

        //clearing
        dbContract.deleteEverythingBluetooth()
    }

    @Test
    fun dataBase_BluetoothContactCrud_Success() {
        val time = Date().time
        val bluetoothContact = BluetoothContact(timestamp = time, deviceId = "device_id_test", rssi = 20, txPower = 10)
        val bluetoothContact1 = BluetoothContact(timestamp = time, deviceId = "device_id_test", rssi = 20, txPower = 10, isUploaded = true)

        dbContract.insertBtAll(arrayListOf(bluetoothContact, bluetoothContact1))
        val bluetoothContactList = dbContract.allBluetoothContacts()
        val uploadedBluetoothContactList = dbContract.notUploadedBluetooth()

        assertNotNull(bluetoothContactList)
        assertEquals(2, bluetoothContactList.size)
        assertNotNull(uploadedBluetoothContactList)
        assertEquals(1, uploadedBluetoothContactList.size)

        //clearing
        dbContract.deleteEverythingBluetooth()
        assertEquals(0, dbContract.allBluetoothContacts().size)
    }

    @Test
    fun dataBase_DeleteUploadedBluetoothContact_Success() {
        val time = Date().time
        val bluetoothContact = BluetoothContact(timestamp = time, deviceId = "device_id_test", rssi = 20, txPower = 10, isUploaded = true)

        dbContract.insertBluetooth(bluetoothContact)
        dbContract.deleteUploadedBluetooth()

        assertEquals(0, dbContract.allBluetoothContacts().size)
    }

    @Test
    fun dataBase_GPSMeasurement_isInserted() {
        val time = Date().time
        val measurement = Measurement(timestamp = time, latitude = 78.66, longitude = 20.99, latLongAccuracy = 1.00,
        altitude = 5.00, altitudeAccuracy = 0.50, speed = 50.00, speedAccuracy = 0.30, isUploaded = false)

        dbContract.insertGPS(measurement)
        val measurementList = dbContract.allGPSMeasurements()

        assertNotNull(measurementList)
        assertEquals(1, measurementList.size)
        assertEquals(time, measurementList[0].timestamp)
        assertEquals(78.66, measurementList[0].latitude)
        assertEquals(20.99, measurementList[0].longitude)
        assertEquals(1.00, measurementList[0].latLongAccuracy)
        assertEquals(5.00, measurementList[0].altitude)
        assertEquals(0.50, measurementList[0].altitudeAccuracy)
        assertEquals(50.00, measurementList[0].speed)
        assertEquals(0.30, measurementList[0].speedAccuracy)
        assertEquals(false, measurementList[0].isUploaded)

        //clearing
        dbContract.deleteEverythingGPS()

    }

    @Test
    fun dataBase_GPSMeasurementCrud_Success() {
        val time = Date().time
        val measurement = Measurement(timestamp = time, latitude = 78.66, longitude = 20.99, latLongAccuracy = 1.00,
        altitude = 5.00, altitudeAccuracy = 0.50, speed = 50.00, speedAccuracy = 0.30, isUploaded = false)
        val measurement1 = Measurement(timestamp = time, latitude = 78.66, longitude = 20.99, latLongAccuracy = 1.00,
        altitude = 5.00, altitudeAccuracy = 0.50, speed = 50.00, speedAccuracy = 0.30, isUploaded = true)

        dbContract.insertGPSAll(arrayListOf(measurement, measurement1))
        val measurementList = dbContract.allGPSMeasurements()
        val uploadedMeasurementList = dbContract.notUploadedGPS()

        assertNotNull(measurementList)
        assertEquals(2, measurementList.size)
        assertNotNull(uploadedMeasurementList)
        assertEquals(1, uploadedMeasurementList.size)

        //clearing
        dbContract.deleteEverythingGPS()
        assertEquals(0, dbContract.allGPSMeasurements().size)

    }


    @Test
    fun dataBase_DeleteUploadedGPSMeasurement_Success() {
        val time = Date().time
        val measurement = Measurement(timestamp = time, latitude = 78.66, longitude = 20.99, latLongAccuracy = 1.00,
            altitude = 5.00, altitudeAccuracy = 0.50, speed = 50.00, speedAccuracy = 0.30, isUploaded = true)

        dbContract.insertGPS(measurement)
        dbContract.deleteUploadedGPS()

        assertEquals(0, dbContract.allGPSMeasurements().size)

    }

    override fun cleanup() {
        dbContract.deleteEverythingBluetooth()
        dbContract.deleteEverythingGPS()
        dbContract.close()
    }
}
