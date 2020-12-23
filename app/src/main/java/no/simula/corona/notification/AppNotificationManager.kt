package no.simula.corona.notification

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import no.simula.corona.*


class AppNotificationManager(private val context: Context) {


    private var mLocationReminder: Notification? = null
    private var mBluetoothReminder: Notification? = null
    internal var mUpdateReminder: Notification? = null
    private var isBackgroundNotificationCreated = false

    init {
        // Setup foreground service notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Smittestopp"
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL,
                name,
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val reminders = NotificationChannel(
                REMINDER_CHANNEL,
                "Smittestopp Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val notificationManager =
                context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(reminders)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL = "Smittestopp Notification Channel"
        const val REMINDER_CHANNEL = "Smittestopp Reminders"
        const val BACKGROUND_NOTIFICATION_ID = 12
        const val AUTH_ERROR_NOTIFICATION_ID = 13
        const val GPS_REMINDER_NOTIFICATION = 14
        const val BT_REMINDER_NOTIFICATION = 15
        const val NEW_VERSION_NOTIFICATION = 16
        const val DATA_DELETED_NOTIFICATION = 17

        @RequiresApi(Build.VERSION_CODES.M)
        fun doesStatusNotificationExist(context: Context): Boolean {
            val notificationManager =
                context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager?

            var notifications = notificationManager!!.activeNotifications
            for (notification in notifications) {
                if (notification.id == BACKGROUND_NOTIFICATION_ID) {
                    return true
                }
            }

            return false
        }
    }

    internal fun createNotification(): Notification {
        val intent = Intent(context, DataCollectorService::class.java)
        intent.putExtra(DataCollectorService.FROM_SERVICE, true)
        val serviceIntent = PendingIntent.getService(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        var mainActivity = Intent(context, MainActivity::class.java)
        mainActivity.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK

        val activityIntent = PendingIntent.getActivity(
            context, 0,
            mainActivity, 0
        )

        val builder = NotificationCompat.Builder(context)
            .setContentTitle(context.getString(R.string.persistent_notification_title))
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentText(context.getString(R.string.notification_description))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(R.drawable.ic_stop_red, context.getString(R.string.stop), serviceIntent)
            .addAction(
                R.drawable.ic_settings_green,
                context.getString(R.string.open),
                activityIntent
            )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(NOTIFICATION_CHANNEL)
        }

        return builder.build()
    }

    private fun createAuthErrorNotification(): Notification {

        val activityIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, SplashActivity::class.java), 0
        )

        val builder = NotificationCompat.Builder(context)
            .setContentTitle(context.getString(R.string.persistent_notification_title))
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentText(context.getString(R.string.you_need_authentication))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(activityIntent)
            .setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(NOTIFICATION_CHANNEL)
        }

        return builder.build()
    }

    internal fun createAuthNotificationIfNotCreateAlready() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (CoronaApp.instance()?.doesAuthNotificationCreated == false) {
            notificationManager.notify(AUTH_ERROR_NOTIFICATION_ID, createAuthErrorNotification())
            CoronaApp.instance()?.doesAuthNotificationCreated = true
        }
    }

    internal fun notifyUpdateAvailable() {
        if (mUpdateReminder != null) {
            return
        }

        val intent = openAppStoreIntent(context)
        val pending = PendingIntent.getActivity(context, 0, intent, 0)

        mUpdateReminder = NotificationCompat.Builder(context).apply {
            setContentTitle(context.getString(R.string.new_version_available))
            setSmallIcon(R.drawable.ic_stat_name)
            setContentText(context.getString(R.string.new_version_description))
            priority = NotificationCompat.PRIORITY_HIGH
            setContentIntent(pending)
            setAutoCancel(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setChannelId(REMINDER_CHANNEL)
            }
        }.build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NEW_VERSION_NOTIFICATION, mUpdateReminder)
    }

    private fun createReminderNotification(title: Int): Notification {
        val intent = PendingIntent.getActivity(
            context, 0,
            Intent(context, SplashActivity::class.java), 0
        )

        val notification = NotificationCompat.Builder(context).apply {
            setContentTitle(context.getString(title))
            setSmallIcon(R.drawable.ic_stat_name)
            setContentText(context.getString(R.string.reminder_description))
            priority = NotificationCompat.PRIORITY_HIGH
            setContentIntent(intent)
            setAutoCancel(true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification.setChannelId(REMINDER_CHANNEL)
        }

        return notification.build()
    }


    private fun showLocationRemainder(mIsBound: Boolean) {
        if (mLocationReminder != null || !Utils.didUserStartGPS(context) || mIsBound) {
            return
        }

        var title = R.string.location_reminder
        var setBoth = false
        if (mBluetoothReminder != null) {
            title = R.string.location_gps_reminder
            setBoth = true
            removeNotificationReminders()
        }

        mLocationReminder = createReminderNotification(title)
        if (setBoth) {
            mBluetoothReminder = mLocationReminder
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(GPS_REMINDER_NOTIFICATION, mLocationReminder)
    }

    private fun showBluetoothReminder(mIsBound: Boolean) {
        if (mBluetoothReminder != null || !Utils.didUserStartBluetooth(context) || mIsBound) {
            return
        }

        var title = R.string.bluetooth_reminder
        var setBoth = false
        if (mLocationReminder != null) {
            title = R.string.location_gps_reminder
            setBoth = true
            removeNotificationReminders()
        }

        mBluetoothReminder = createReminderNotification(title)

        if (setBoth) {
            mLocationReminder = mBluetoothReminder
        }


        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(BT_REMINDER_NOTIFICATION, mBluetoothReminder)
    }

    internal fun removeNotificationReminders() {
        mLocationReminder = null
        mBluetoothReminder = null


        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(GPS_REMINDER_NOTIFICATION)
        manager.cancel(BT_REMINDER_NOTIFICATION)
    }

    private fun openAppStoreIntent(context: Context): Intent {
        val appId = BuildConfig.APPLICATION_ID
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appId"))
        var storeIntent: Intent? = null

        val otherApps = context.packageManager.queryIntentActivities(intent, 0)
        for (otherApp in otherApps) {
            if (otherApp.activityInfo.applicationInfo.packageName == "com.android.vending") {
                val other = otherApp.activityInfo
                val name = ComponentName(other.applicationInfo.packageName, other.name)

                storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appId"))
                    .apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        component = name
                    }

                break
            }
        }

        // if GP not present on device, open web browser
        return storeIntent
            ?: Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$appId")
            )
    }

    internal fun cancelAuthNotificationIfAny() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        CoronaApp.instance()?.doesAuthNotificationCreated = false
        notificationManager.cancel(AUTH_ERROR_NOTIFICATION_ID)
    }

    internal fun startForegroundIfNotExists(service: Service) {

        if (!doesBackgroundNotificationExist(service)) {
            service.startForeground(BACKGROUND_NOTIFICATION_ID, createNotification())
            isBackgroundNotificationCreated = true
        }
    }

    internal fun stopForeground(service: Service) {
        service.stopForeground(true)
        isBackgroundNotificationCreated = false
    }


    fun doesBackgroundNotificationExist(context: Context): Boolean {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (doesStatusNotificationExist(context)) {
                return true
            }
        }
        return isBackgroundNotificationCreated
    }


    internal fun destroy() {
        mUpdateReminder = null
    }

    internal fun handleDeletedDevice() {
        val i = Intent(context, SplashActivity::class.java)
        i.putExtra(SplashActivity.DELETED_REMOTE, true)
        val intent = PendingIntent.getActivity(context, 0, i, 0)

        val notification = NotificationCompat.Builder(context).apply {
            setContentTitle(context.getString(R.string.data_deleted_notification))
            setSmallIcon(R.drawable.ic_stat_name)
            setContentText(context.getString(R.string.data_deleted_action))
            priority = NotificationCompat.PRIORITY_HIGH
            setContentIntent(intent)
            setAutoCancel(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setChannelId(REMINDER_CHANNEL)
            }
        }.build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(DATA_DELETED_NOTIFICATION, notification)
    }

}