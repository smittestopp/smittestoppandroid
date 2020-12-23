package no.simula.corona

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import no.simula.corona.data.DatabaseProvider
import org.junit.Test

import org.junit.Assert.*
import org.junit.runner.RunWith

/**
 * Tests green dao db
 */
@RunWith(AndroidJUnit4::class)
class DatabaseProviderInstrumentedTest{

    @Test
    fun dataBase_NotNull() {
        val db = DatabaseProvider.open(InstrumentationRegistry.getInstrumentation().targetContext)

        assertNotNull(db)

        db.close()
    }
}
