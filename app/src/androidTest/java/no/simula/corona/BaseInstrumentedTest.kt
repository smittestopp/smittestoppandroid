package no.simula.corona

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import no.simula.corona.data.DatabaseProvider
import org.junit.After
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith

/**
 * Tests green dao db
 */
abstract class BaseInstrumentedTest {
    @Before
    abstract fun setup()
    @After
    abstract fun cleanup()

}
