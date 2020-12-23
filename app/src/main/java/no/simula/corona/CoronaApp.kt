package no.simula.corona

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import no.simula.corona.data.DBExporter
import no.simula.corona.heartbeat.Heartbeat
import no.simula.corona.security.SecretValueGenerator
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.lang.Exception


class CoronaApp: MultiDexApplication(), LifecycleObserver {


    companion object{
        private var theApp:CoronaApp? = null
        fun instance() = theApp
        fun isInBackground() = instance()?.inBackground
    }

    var inBackground = false
    var doesAuthNotificationCreated = false

    override fun onCreate() {
        super.onCreate()

        theApp = this
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this)

        AppCenter.start(
            this, getString(R.string.aid),
            Analytics::class.java, Crashes::class.java)

        if (BuildConfig.DEBUG) {
                Timber.plant(DebugTree())
        }
        else
        {
                Timber.plant(ReleaseTree())
        }
        Timber.d("starting app")

        SecretValueGenerator.prepare(this)
        DBExporter.export(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        inBackground = true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        inBackground = false
    }

    class ReleaseTree : Timber.Tree() {
        override fun log(
            priority: Int,
            tag: String?,
            message: String,
            t: Throwable?
        ) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG) {
                return
            }
            try {
                Crashes.trackError(t)
            }
            catch (ex:Exception){
            }
        }
    }


}