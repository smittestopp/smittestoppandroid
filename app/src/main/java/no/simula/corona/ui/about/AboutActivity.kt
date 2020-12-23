package no.simula.corona.ui.about

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.mikepenz.aboutlibraries.LibsBuilder
import kotlinx.android.synthetic.main.activity_about.*
import no.simula.corona.R
import timber.log.Timber
import java.lang.Exception

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val fragment = LibsBuilder()
            .withFields(R.string::class.java.fields)
            .withAboutDescription("")
            .supportFragment()


        val fragmentManager = supportFragmentManager
        fragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()

        setupToolbar()
    }


    private fun setupToolbar() {
        try {
            setSupportActionBar(toolbar)
        } catch (e: Exception) {
            // can this throw?
            Timber.e(e)
        }

        setToolBarTitle("" + title)
        setDisplayHomeAsUpEnabled(true)
    }

    private fun setToolBarTitle(toolBarTitle: String) {
        if (supportActionBar != null) {
            supportActionBar!!.title = toolBarTitle
        }
    }

    private fun setDisplayHomeAsUpEnabled(showHomeAsUp: Boolean) {
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(showHomeAsUp)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

}
