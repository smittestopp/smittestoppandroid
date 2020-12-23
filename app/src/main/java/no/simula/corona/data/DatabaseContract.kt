package no.simula.corona.data

import no.simula.corona.data.model.BluetoothContact
import no.simula.corona.data.model.Measurement


interface DatabaseContract {

    fun allGPSMeasurements(): List<Measurement>
    fun notUploadedGPS(): List<Measurement>
    fun deleteUploadedGPS()
    fun insertGPS(measurement: Measurement)
    fun markAsUploadedGPS(ids: List<Long>)
    fun deleteEverythingGPS()

    fun allBluetoothContacts(): List<BluetoothContact>
    fun notUploadedBluetooth(): List<BluetoothContact>
    fun deleteUploadedBluetooth()
    fun insertBluetooth(bluetoothContact: BluetoothContact)
    fun markAsUploadedBluetooth(ids: List<Long>)
    fun deleteEverythingBluetooth()
    fun close()

}