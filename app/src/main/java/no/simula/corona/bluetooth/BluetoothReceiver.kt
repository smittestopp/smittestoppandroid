package no.simula.corona.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import no.simula.corona.events.BluetoothAdapterEvent
import no.simula.corona.events.BluetoothEvent
import org.greenrobot.eventbus.EventBus
import timber.log.Timber


class BluetoothReceiver: BroadcastReceiver() {
    companion object{
        var bluetoothAdapter = false
    }
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent!!.action
        val state: Int

        when (action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                state = intent!!.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                if (state == BluetoothAdapter.STATE_OFF) {
                    bluetoothAdapter = false
                    Timber.d("Bluetooth is off")
                    EventBus.getDefault().post(BluetoothAdapterEvent(false))
                } else if (state == BluetoothAdapter.STATE_ON) {
                    bluetoothAdapter = true
                    EventBus.getDefault().post(BluetoothAdapterEvent(true))
                    Timber.d("Bluetooth is on")
                }
            }
        }
    }
}