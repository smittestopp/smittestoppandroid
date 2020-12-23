package no.simula.corona.bluetooth

import android.bluetooth.*
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ
import android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY
import android.content.Context
import no.simula.corona.Utils
import timber.log.Timber


fun BluetoothGattCharacteristic.isDeviceIdentifier(): Boolean =
    this.uuid == Utils.DEVICE_CHARACTERISTIC_UUID

class Gatt(private val context: Context, private val bluetoothManager: BluetoothManager) {
    private val identifier: ByteArray =
        Utils.getProvisionDeviceId(context.applicationContext).toByteArray(Charsets.US_ASCII)

    private val service: BluetoothGattService = BluetoothGattService(
        Utils.SMITTESTOPP_SERVICE_UUID,
        SERVICE_TYPE_PRIMARY
    ).also {
        it.addCharacteristic(
            BluetoothGattCharacteristic(
                Utils.DEVICE_CHARACTERISTIC_UUID,
                PROPERTY_READ,
                PERMISSION_READ
            )
        )
    }

    private var server: BluetoothGattServer? = null

    fun start() {
        val callback = object : BluetoothGattServerCallback() {
            override fun onCharacteristicReadRequest(
                device: BluetoothDevice,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic
            ) {
                if (!characteristic.isDeviceIdentifier() || server == null) {
                    return
                }

                if (offset == 0) {
                    Timber.d("Requesting characteristic UUID: ${characteristic.uuid}")
                    Timber.d("Responding: + ${identifier.toString(Charsets.US_ASCII)}")
                }

                if (offset > identifier.size) {
                    server!!.sendResponse(
                        device,
                        requestId,
                        GATT_SUCCESS,
                        0,
                        byteArrayOf()
                    )
                    return
                }


                val size: Int = identifier.size - offset
                val response = ByteArray(size)

                for (i in offset until identifier.size) {
                    response[i - offset] = identifier[i]
                }

                server!!.sendResponse(device, requestId, GATT_SUCCESS, offset, response)
            }
        }

        server = bluetoothManager.openGattServer(context, callback).also {
            it?.addService(service)
        }
    }

    fun stop() {
        server?.close()
    }
}