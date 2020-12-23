package no.simula.corona

import org.junit.After
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.mockito.MockitoAnnotations

/**
 * contains all common methods for unit tests
 */
open class BaseTest {

    @Before
    open fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @After
    open fun cleanup(){

    }
}
