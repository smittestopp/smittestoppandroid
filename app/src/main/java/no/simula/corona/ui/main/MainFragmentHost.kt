package no.simula.corona.ui.main

interface MainFragmentHost {
    fun onHomeFragmentAttach(mainFragment: MainFragmentConnection?)
    fun isServiceRunning(): Boolean?
    fun isBluetoothWorking():Boolean?
    fun isLocationWorking():Boolean?
    fun enableLocationUpdates(enable: Boolean)
    fun enableBluetoothUpdates(enable: Boolean)
    fun gotoSettings()
}