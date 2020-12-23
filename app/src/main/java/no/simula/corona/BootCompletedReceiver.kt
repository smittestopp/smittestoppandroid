package no.simula.corona

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import no.simula.corona.DataCollectorService.Companion.AUTO_START
import no.simula.corona.DataCollectorService.Companion.COMMAND_START_ALL_USER_SELECTED
import no.simula.corona.DataCollectorService.Companion.SERVICE_COMMAND
import timber.log.Timber


/**
 * Class that implements @see[BroadcastReceiver]. If the app has been closed by the user or
 * crashed, it resets the state of the app on next startup. If the user has had location and/or
 * Bluetooth monitoring activated, these services will be automatically restarted.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    /**
     * On intent received. Starts an intent to start DataCollectorService if location and/or
     * Bluetooth were activated prior to the closing of the app. Below Android O (API Level 26)
     * this is handled using an @see[AlarmManager].
     */
    override fun onReceive(context: Context, intent: Intent) {
        if (Utils.isFirstland(context) ||
            !Utils.isProvisioned(context) ||
            Utils.getToken(context).isEmpty()
        ) {
            /**
             * Receiver is invoked before onboarding is completed
             * Ignore it until the user has consented and is registered
             */
            return
        }

        // app has booted or package has updated
        if (Intent.ACTION_BOOT_COMPLETED == intent.action ||
            Intent.ACTION_MY_PACKAGE_REPLACED == intent.action
        ) {

            // User previously had location and/or Bluetooth running
            if (Utils.didUserStartBluetooth(context) || Utils.didUserStartGPS(context)) {

                // >= Android O (API Level 26)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val intent = dataCollectorServiceIntent(context)
                    context.startForegroundService(intent)
                }
                // < Android O (API Level 26)
                else {
                    startServiceWithAlarm(context)
                }
            }
        }
    }

    /**
     * If the Android version is Nougat (API Level 25) or below, this method handles the restart
     * of previously run services using an @see[AlarmManager].
     */
    private fun startServiceWithAlarm(context: Context) {
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        Timber.d("starting app automatically")

        // Create intent to invoke the background service.
        val intent = dataCollectorServiceIntent(context)

        // Setup pending intent that will be started from alarm
        val pendingIntent = PendingIntent.getService(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Set alarm for 10 seconds from now
        alarmManager?.set(
            AlarmManager.RTC,
            SystemClock.currentThreadTimeMillis() + (10 * 1000),
            pendingIntent
        )
    }

    /**
     * Set up and start an Intent for DataCollectorService that restores monitoring
     * to the previous state. Also starts foreground service.
     *
     * @return Intent to start DataCollectorService
     */
    private fun dataCollectorServiceIntent(context: Context): Intent {
        var intent = Intent(context, DataCollectorService::class.java)
        intent.putExtra(SERVICE_COMMAND, COMMAND_START_ALL_USER_SELECTED)
        intent.putExtra(AUTO_START, true)
        return intent
    }
}
