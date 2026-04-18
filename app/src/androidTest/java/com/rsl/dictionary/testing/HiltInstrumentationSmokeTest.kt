package com.rsl.dictionary.testing

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rsl.dictionary.repositories.protocols.SignRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import javax.inject.Inject
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HiltInstrumentationSmokeTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var signRepository: SignRepository

    @Test
    fun hiltInstrumentationEnvironment_startsWithoutErrors() {
        hiltRule.inject()

        val application = ApplicationProvider.getApplicationContext<Application>()

        assertTrue(application is HiltTestApplication)
        assertNotNull(signRepository)
    }
}
