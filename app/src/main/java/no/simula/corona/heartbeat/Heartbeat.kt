package no.simula.corona.heartbeat

import android.content.Context
import android.provider.Settings
import androidx.work.*
import com.microsoft.appcenter.crashes.Crashes
import no.simula.corona.Utils
import timber.log.Timber
import java.lang.Exception
import java.util.concurrent.TimeUnit

class Heartbeat {
    companion object {
        /**
         * Register a new Heartbeat recurring task
         *
         * If the worker is already registered nothing is done
         */
        fun register(context: Context) {
            Timber.d("Start register")
            if (isNotReady(context)) {
                return
            }

            try {
                WorkManager.getInstance(context).apply {
                    enqueueUniquePeriodicWork(
                        workerName(),
                        ExistingPeriodicWorkPolicy.KEEP,
                        PeriodicWorkRequest.Builder(
                            HeartbeatWorker::class.java,
                            1,
                            TimeUnit.DAYS
                        ).build()
                    )
                }

                Timber.d("Registered HeartbeatWorker")
            } catch (err: Exception) {
                Timber.e(err)
                Crashes.trackError(err)
            }
        }

        /**
         * Remove the HeartbeatWorker task if it exists
         */
        fun unregister(context: Context) {
            Timber.d("Start unregister")
            try {
                WorkManager.getInstance(context).apply {
                    cancelUniqueWork(workerName())
                }

                Timber.d("Unregistered HeartbeatWorker")
            } catch (err: Exception) {
                Timber.e(err)
                Crashes.trackError(err)
            }
        }

        /**
         * Schedule the HeartbeatWorker to run a single time after a 10 minutes delay
         */
        fun scheduleSingle(context: Context) {
            if (isNotReady(context)) {
                return
            }

            try {
                WorkManager.getInstance(context).apply {

                    enqueue(with(OneTimeWorkRequest.Builder(HeartbeatWorker::class.java))
                    {
                        setInitialDelay(1, TimeUnit.MINUTES)
                        setInputData(
                            Data.Builder().put(HeartbeatWorker.RESCHEDULE, true).build()
                        )
                        build()
                    })

                }
            } catch (e: Exception) {
                Timber.e(e)
                Crashes.trackError(e)
            }
        }

        /**
         *
         */
        private fun workerName(): String {
            return with(Settings.Secure.ANDROID_ID) {
                substring(0, if (length > 24) 24 else length)
            }
        }

        private fun isNotReady(context: Context): Boolean {
            if (!Utils.hasGivenConsent(context) || !Utils.isProvisioned(context)) {
                // don't start before the privacy policy is accepted
                Timber.d("Missing consent or provisioning, terminating")
                return true
            }

            return false
        }
    }
}