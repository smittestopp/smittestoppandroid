package no.simula.corona.heartbeat

object RunningStatus {

    /**
     *
     * @return status of the collector service.
     *      0 both GPS and BT are running,
     *      1 only GPS is running,
     *      2 Only BT is running,
     *      3 both are disabled
     */

    var statusListener: OnStatusListening? = null

    const val UNKNOWN = -1
    const val FULL_FEATURE_RUNNING = 0
    const val GPS_RUNNING_ONLY = 1
    const val BLUETOOTH_RUNNING_ONLY = 2
    const val NOTHING_RUNNING = 3

    private var mGpsRunning: Boolean = false
    private var mBluetoothRunning: Boolean = false

    fun setGpsRunning(gpsRunning: Boolean) {
        mGpsRunning = gpsRunning
    }

    fun setBluetoothRunning(bluetoothRunning: Boolean) {
        mBluetoothRunning = bluetoothRunning
    }

    fun publish() {
        publish(mGpsRunning, mBluetoothRunning)
    }

    fun publish(gpsRunning: Boolean, bluetoothRunning: Boolean) {


        var status: Int = if (gpsRunning && bluetoothRunning) FULL_FEATURE_RUNNING else {
            if (gpsRunning) {
                GPS_RUNNING_ONLY
            } else if (bluetoothRunning) {
                BLUETOOTH_RUNNING_ONLY
            } else
                NOTHING_RUNNING
        }

        statusListener?.onStatusPublished(status)
    }


    interface OnStatusListening {
        fun onStatusPublished(status: Int)
    }
}