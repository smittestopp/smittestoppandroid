package no.simula.corona.data.legacy

import android.content.Context
import no.simula.corona.data.DatabaseContract

class LegacyDatabaseImpl : DatabaseContract {

    constructor(context: Context) {
        mDatabase = MeasurementDatabase.get(context)
    }

    private var mDatabase: MeasurementDatabase

    override fun allGPSMeasurements(): List<no.simula.corona.data.model.Measurement> =
        no.simula.corona.data.model.Measurement.fromEntity(
            mDatabase.measurementDao().allMeasurements()
        )

    override fun notUploadedGPS(): List<no.simula.corona.data.model.Measurement> =
        no.simula.corona.data.model.Measurement.fromEntity(mDatabase.measurementDao().notUploaded())

    override fun deleteUploadedGPS() = mDatabase.measurementDao().deleteUploaded()

    override fun insertGPS(measurment: no.simula.corona.data.model.Measurement) =
        mDatabase.measurementDao()
            .insertAll(no.simula.corona.data.model.Measurement.toEntity(measurment))

    override fun markAsUploadedGPS(ids: List<Long>) {

        var intIds = ArrayList<Int>()
        ids.map {
            intIds.add(it.toInt())
        }

        mDatabase.measurementDao().markAsUploaded(intIds)

    }

    override fun deleteEverythingGPS() = mDatabase.measurementDao().deleteEverything()


    override fun allBluetoothContacts(): List<no.simula.corona.data.model.BluetoothContact> =
        no.simula.corona.data.model.BluetoothContact.fromEntity(
            mDatabase.bluetoothContactDao().allBluetoothContacts()
        )

    override fun notUploadedBluetooth(): List<no.simula.corona.data.model.BluetoothContact> =
        no.simula.corona.data.model.BluetoothContact.fromEntity(
            mDatabase.bluetoothContactDao().notUploaded()
        )

    override fun deleteUploadedBluetooth() = mDatabase.bluetoothContactDao().deleteUploaded()

    override fun insertBluetooth(bluetoothContact: no.simula.corona.data.model.BluetoothContact) =
        mDatabase.bluetoothContactDao()
            .insertAll(no.simula.corona.data.model.BluetoothContact.toEntity(bluetoothContact))

    override fun markAsUploadedBluetooth(ids: List<Long>) {
        var intIds = ArrayList<Int>()
        ids.map {
            intIds.add(it.toInt())
        }
        mDatabase.bluetoothContactDao().markAsUploaded(intIds)
    }

    override fun deleteEverythingBluetooth() = mDatabase.bluetoothContactDao().deleteEverything()


    override fun close() {

    }
}