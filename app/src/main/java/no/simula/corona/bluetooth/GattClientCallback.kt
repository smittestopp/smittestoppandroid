package no.simula.corona.bluetooth

import android.bluetooth.*
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.os.Build
import no.simula.corona.Utils
import timber.log.Timber
import java.lang.NullPointerException

class GattClientCallback(private val scanListener: ScanListener?, private val rssi: Int,
                         private val txPower: Int) : BluetoothGattCallback(), DeviceReadListener {
    var identifier: String? = null

    private fun storeIfReady(gatt: BluetoothGatt) {
        Timber.i("storeIfRead ${this.rssi}, ${this.identifier}")
        if (this.identifier != null) {
            onReadComplete(identifier!!, rssi!!)
        }
        gatt.disconnect()
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        Timber.i("onServicesDiscovered status: $status")
        if (status == BluetoothGatt.GATT_SUCCESS) {
            try {
                gatt
                    .getService(Utils.SMITTESTOPP_SERVICE_UUID)
                    .getCharacteristic(Utils.DEVICE_CHARACTERISTIC_UUID)
                    .let {
                        gatt.readCharacteristic(it)
                    }
            } catch (e: NullPointerException) {
                Timber.d("onServicesDiscovered device does not display service UUID")
                onReadComplete(null, null)
            }
        } else {
            gatt.disconnect()
        }
    }

    override fun onReadComplete(id: String?, rssi: Int?)
    {
        if (id != null && rssi != null) {
            scanListener?.onNewBluetoothResult(
                id,
                rssi,
                txPower,
                System.currentTimeMillis()
            )
        }
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        if (status != GATT_SUCCESS) {
            gatt.disconnect()
        }
        if (characteristic.isDeviceIdentifier()) {
            Timber.i("onCharacteristicRead")
            this.identifier = characteristic.value?.toString(Charsets.US_ASCII)
            Timber.e("Got identifier $identifier")
            storeIfReady(gatt)
        }
    }

    override fun onConnectionStateChange(
        gatt: BluetoothGatt,
        status: Int,
        newState: Int
    ) {
        Timber.i("status: $status, state: $newState")

        //todo: do we need gatt.device.bondState?

        if(status == GATT_SUCCESS) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // We successfully connected, proceed with service discovery
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // We successfully disconnected on our own request
                gatt.close()
            } else {
                // We're CONNECTING or DISCONNECTING, ignore for now
            }
        } else {
            // An error happened...figure out what happened!
            gatt.close();
        }
    }
}

