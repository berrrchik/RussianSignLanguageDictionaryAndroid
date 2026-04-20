package com.rsl.dictionary.utilities.cache

import com.rsl.dictionary.testing.rules.MainDispatcherRule
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class MemoryCacheManagerTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun setAndGet_returnsStoredObject() = runTest {
        val cache = MemoryCacheManager<String>()

        cache.set("cached-value")

        assertEquals("cached-value", cache.get())
    }

    @Test
    fun clear_removesCachedObject() = runTest {
        val cache = MemoryCacheManager<String>()
        cache.set("cached-value")

        cache.clear()

        assertNull(cache.get())
    }

    @Test
    fun parallelOperations_doNotCrashAndRemainUsable() = runTest {
        val cache = MemoryCacheManager<Int>()

        val jobs = (1..100).map { value ->
            async {
                cache.set(value)
                cache.get()
            }
        }

        jobs.awaitAll()
        cache.set(999)

        assertEquals(999, cache.get())
    }
}
