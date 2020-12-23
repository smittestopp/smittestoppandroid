package no.simula.corona.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import no.simula.corona.MainActivity
import no.simula.corona.ui.register.AgeVerification
import no.simula.corona.ui.register.RegisterActivity
import timber.log.Timber

open class BaseActivity: AppCompatActivity() {

    protected fun gotoMainScreen() {

        if(isFinishing){
            return
        }

        Timber.d(this.javaClass.name + "gotoMainScreen" )

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    protected fun gotoRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    protected  fun gotoAgeVerification() {
        val intent = Intent(this, AgeVerification::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }
}