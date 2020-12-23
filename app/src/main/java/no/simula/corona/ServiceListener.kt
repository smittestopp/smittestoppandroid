package no.simula.corona

interface ServiceListener {
    fun onLocationUpdateStarted()
    fun onBluetoothUpdateStarted()
    fun onLocationUpdateStopped()
    fun onBluetoothUpdateStopped()
    fun onServiceDestroyed()

    /// when Iot Hub devices was deleted in the cloud
    fun onIotDeviceDeleted()
}