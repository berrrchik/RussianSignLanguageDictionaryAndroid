package com.rsl.dictionary.services.cache

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.rsl.dictionary.testing.factories.TestDataFactory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CacheServiceTest {
    private lateinit var context: Context
    private lateinit var service: CacheService

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        service = CacheService(context)
        service.clear()
    }

    @Test
    fun saveAndLoad_roundTripSyncData() = runTest {
        val data = TestDataFactory.syncData()

        service.save(data)

        assertEquals(data, service.load())
    }

    @Test
    fun missingCacheFile_returnsNull() = runTest {
        service.clear()

        assertNull(service.load())
    }

    @Test
    fun corruptedJson_returnsNullInsteadOfThrowing() = runTest {
        cacheFile().writeText("{bad json")

        assertNull(service.load())
    }

    @Test
    fun clear_deletesCacheFile() = runTest {
        service.save(TestDataFactory.syncData())
        assertTrue(cacheFile().exists())

        service.clear()

        assertFalse(cacheFile().exists())
    }

    private fun cacheFile() = context.filesDir.resolve("cached_signs_data.json")
}
