package no.simula.corona.bluetooth

interface DeviceReadListener {
    fun onReadComplete( id:String?, rssi: Int?)
}