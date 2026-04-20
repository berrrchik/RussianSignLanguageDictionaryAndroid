package com.rsl.dictionary.services.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ETagManagerTest {
    private lateinit var manager: ETagManager

    @Before
    fun setUp() {
        manager = ETagManager(ApplicationProvider.getApplicationContext<Context>())
        manager.clearETag(KEY)
    }

    @Test
    fun saveETag_trimsWhitespaceAndQuotes() {
        manager.saveETag(KEY, "  \"1234567890abcdef1234567890abcdef\"  ")

        assertEquals("1234567890abcdef1234567890abcdef", manager.getETag(KEY))
    }

    @Test
    fun saveETag_cutsAtFirstColon() {
        manager.saveETag(KEY, "\"1234567890abcdef1234567890abcdef:meta\"")

        assertEquals("1234567890abcdef1234567890abcdef", manager.getETag(KEY))
    }

    @Test
    fun saveETag_ignoresInvalidLength() {
        manager.saveETag(KEY, "\"short\"")

        assertNull(manager.getETag(KEY))
    }

    @Test
    fun saveETag_persistsValidValue() {
        manager.saveETag(KEY, "1234567890abcdef1234567890abcdef")

        assertEquals("1234567890abcdef1234567890abcdef", manager.getETag(KEY))
    }

    @Test
    fun clearETag_removesStoredValue() {
        manager.saveETag(KEY, "1234567890abcdef1234567890abcdef")

        manager.clearETag(KEY)

        assertNull(manager.getETag(KEY))
    }

    private companion object {
        const val KEY = "test_etag"
    }
}
