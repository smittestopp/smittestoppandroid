package no.simula.corona.data.greendao

import android.content.Context
import no.simula.corona.data.DatabaseContract
import no.simula.corona.data.model.Measurement
import no.simula.corona.security.SecretValueGenerator
import org.greenrobot.greendao.database.Database
import timber.log.Timber
import java.lang.Exception
import java.util.concurrent.Semaphore


class GreenDaoDatabaseImpl : DatabaseContract {

    companion object {
        var DB_FILENAME = "measurement_database_encrypted"

        private var queryLock: Semaphore = Semaphore(1)
        private var databaseLock: Semaphore = Semaphore(1)

    }

    constructor(context: Context) {
        mContext = context
        open(mContext)
    }

    private fun open(context: Context) {

        try {

            databaseLock.acquire()

            val helper = DaoMaster.DevOpenHelper(context, DB_FILENAME)
            mDatabase =
                helper.getEncryptedWritableDb(SecretValueGenerator.getSecureValueDB(context))

            mSession = DaoMaster(mDatabase).newSession()
        } finally {
            databaseLock.release()
        }

    }

    private var mContext: Context
    private lateinit var mSession: DaoSession
    private lateinit var mDatabase: Database

    override fun allGPSMeasurements(): List<no.simula.corona.data.model.Measurement> {

        return threadProtection<List<no.simula.corona.data.model.Measurement>>(object :
            DbTask<List<no.simula.corona.data.model.Measurement>> {
            override fun run(): List<Measurement> {
                return no.simula.corona.data.model.Measurement.fromEntityGreenDao(mSession.measurementDao.loadAll())
            }
        }, ArrayList<Measurement>())
    }

    override fun notUploadedGPS(): List<no.simula.corona.data.model.Measurement> {

        return threadProtection<List<no.simula.corona.data.model.Measurement>>(object :
            DbTask<List<no.simula.corona.data.model.Measurement>> {
            override fun run(): List<Measurement> {
                return no.simula.corona.data.model.Measurement.fromEntityGreenDao(
                    mSession.measurementDao.queryBuilder()
                        .where(MeasurementDao.Properties.IsUploaded.eq(false)).list()
                )
            }
        }, ArrayList<Measurement>())

    }

    override fun deleteUploadedGPS() {

        threadProtectionNoReturn(object : Runnable {
            override fun run() {
                mSession.measurementDao.queryBuilder()
                    .where(MeasurementDao.Properties.IsUploaded.eq(true)).buildDelete()
                    .forCurrentThread().executeDeleteWithoutDetachingEntities()
                mSession.clear()
            }
        })
    }

    override fun insertGPS(measurement: no.simula.corona.data.model.Measurement) {

        threadProtectionNoReturn(object : Runnable {
            override fun run() {
                mSession.measurementDao.insert(
                    no.simula.corona.data.model.Measurement.toEntityGreenDao(
                        measurement
                    )
                )
            }
        })
    }

    fun insertGPSAll(measurement: List<no.simula.corona.data.model.Measurement>) {

        threadProtectionNoReturn(object : Runnable {
            override fun run() {
                mSession.measurementDao.insertOrReplaceInTx(
                    no.simula.corona.data.model.Measurement.toEntityGreenDao(
                        measurement
                    )
                )
            }
        })

    }

    fun insertBtAll(contacts: List<no.simula.corona.data.model.BluetoothContact>) {

        threadProtectionNoReturn(object : Runnable {
            override fun run() {
                mSession.bluetoothContactDao.insertOrReplaceInTx(
                    no.simula.corona.data.model.BluetoothContact.toEntityGreenDao(
                        contacts
                    )
                )
            }
        })

    }


    override fun markAsUploadedGPS(ids: List<Long>) {

        threadProtectionNoReturn(object : Runnable {
            override fun run() {
                var entities = mSession.measurementDao.queryBuilder()
                    .where(MeasurementDao.Properties.Id.`in`(ids)).list()

                entities.map {
                    it.isUploaded = true
                }
                mSession.measurementDao.updateInTx(entities)
            }
        })

    }

    override fun deleteEverythingGPS() {

        threadProtectionNoReturn(object : Runnable {
            override fun run() {
                mSession.measurementDao.deleteAll()
                mSession.measurementDao.detachAll()
            }
        })


    }

    override fun allBluetoothContacts(): List<no.simula.corona.data.model.BluetoothContact> {


        return threadProtection<List<no.simula.corona.data.model.BluetoothContact>>(object :
            DbTask<List<no.simula.corona.data.model.BluetoothContact>> {
            override fun run(): List<no.simula.corona.data.model.BluetoothContact> {
                return no.simula.corona.data.model.BluetoothContact.fromEntityGreenDao(mSession.bluetoothContactDao.loadAll())
            }
        }, ArrayList<no.simula.corona.data.model.BluetoothContact>())

    }

    override fun notUploadedBluetooth(): List<no.simula.corona.data.model.BluetoothContact> {

        return threadProtection<List<no.simula.corona.data.model.BluetoothContact>>(object :
            DbTask<List<no.simula.corona.data.model.BluetoothContact>> {
            override fun run(): List<no.simula.corona.data.model.BluetoothContact> {

                return no.simula.corona.data.model.BluetoothContact.fromEntityGreenDao(
                    mSession.bluetoothContactDao.queryBuilder()
                        .where(BluetoothContactDao.Properties.IsUploaded.eq(false)).list()
                )
            }
        }, ArrayList<no.simula.corona.data.model.BluetoothContact>())

    }

    override fun deleteUploadedBluetooth() {


        threadProtectionNoReturn(object : Runnable {
            override fun run() {
                mSession.bluetoothContactDao.queryBuilder()
                    .where(BluetoothContactDao.Properties.IsUploaded.eq(true)).buildDelete()
                    .forCurrentThread().executeDeleteWithoutDetachingEntities()

                mSession.clear()
            }
        })


    }

    override fun insertBluetooth(bluetoothContact: no.simula.corona.data.model.BluetoothContact) {

        threadProtectionNoReturn(object : Runnable {
            override fun run() {
                mSession.bluetoothContactDao.insert(
                    no.simula.corona.data.model.BluetoothContact.toEntityGreenDao(
                        bluetoothContact
                    )
                )
            }
        })
    }

    override fun markAsUploadedBluetooth(ids: List<Long>) {

        threadProtectionNoReturn(object : Runnable {
            override fun run() {
                var entities = mSession.bluetoothContactDao.queryBuilder()
                    .where(BluetoothContactDao.Properties.Id.`in`(ids)).list()

                entities.map {
                    it.isUploaded = true
                }
                mSession.bluetoothContactDao.updateInTx(entities)
            }
        })

    }

    override fun deleteEverythingBluetooth() {

        threadProtectionNoReturn(object : Runnable {
            override fun run() {
                mSession.bluetoothContactDao.deleteAll()
                mSession.bluetoothContactDao.detachAll()
            }
        })
    }

    override fun close() {
        try {
            mDatabase.close()
        } catch (ex: Exception) {
            Timber.e(ex)
        }
    }

    private fun <T> threadProtection(task: DbTask<T>, default: T): T {
        try {
            queryLock.acquire()
            return task.run()
        } catch (ex: net.sqlcipher.database.SQLiteException) {
            handleDatabaseFault()
            return safeReRun(task, default)
        } catch (ex: Exception) {
            Timber.d(ex)
            return default
        } finally {
            queryLock.release()
        }
    }


    private fun threadProtectionNoReturn(task: Runnable) {
        try {
            queryLock.acquire()
            Timber.d("lock acquired")
            task.run()
        } catch (ex: net.sqlcipher.database.SQLiteException) {
            handleDatabaseFault()
            safeReRun(task)
        } catch (ex: Exception) {
            Timber.d(ex)
        } finally {
            queryLock.release()
            Timber.d("lock release")
        }
    }

    private fun safeReRun(task: Runnable) {
        try {
            task.run()
        } catch (ex: Exception) {
            Timber.d(ex)
        }
    }

    private fun <T> safeReRun(task: DbTask<T>, default: T): T {
        try {
            return task.run()
        } catch (ex: Exception) {
            return default
        }
    }

    private fun handleDatabaseFault() {

        Timber.e("handleDatabaseFault")

        this.close()
        this.open(mContext) // Reopen to refresh the session
    }


    interface DbTask<T> {
        fun run(): T
    }
}