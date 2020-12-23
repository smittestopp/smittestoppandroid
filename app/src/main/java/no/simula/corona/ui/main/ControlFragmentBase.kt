package no.simula.corona.ui.main

import android.bluetooth.BluetoothManager
import android.content.Context
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import no.simula.corona.events.BluetoothAdapterEvent
import no.simula.corona.events.BluetoothEvent
import no.simula.corona.events.GPSEvent
import no.simula.corona.events.LocationServiceEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

open class ControlFragmentBase : MainBaseFragment() {
    protected var bluetoothAdapterState = false
    protected var gpsLocationEnabled = false
    protected var bluetooth: Boolean = false
    protected var location: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bluetooth = callback?.isBluetoothWorking() == true
        location = callback?.isLocationWorking() == true

        val bt = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapterState = bt.adapter?.isEnabled ?: false

        val manager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        gpsLocationEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }


    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public fun onEvent(event: BluetoothEvent) {
        bluetooth = event.on

        setUIState()
    }

    open fun setUIState() {
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public fun onEvent(event: GPSEvent) {
        location = event.on
        setUIState()

    }


    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public fun onEvent(event: BluetoothAdapterEvent) {
        if (!event.on) {
            bluetooth = false
        }

        bluetoothAdapterState = event.on

        setUIState()

    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public fun onEvent(event: LocationServiceEvent) {
        if (!event.on) {
            location = false
        }

        gpsLocationEnabled = event.on

        setUIState()
    }

}