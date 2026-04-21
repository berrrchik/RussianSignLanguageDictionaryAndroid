package com.rsl.dictionary.repositories.decorators

import com.rsl.dictionary.errors.SyncError
import com.rsl.dictionary.models.SyncData
import com.rsl.dictionary.models.SyncMetadata
import com.rsl.dictionary.repositories.protocols.SyncFetchResult
import com.rsl.dictionary.repositories.protocols.SyncRepository
import com.rsl.dictionary.testing.factories.TestDataFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoggingSyncRepositoryDecoratorTest {
    @Test
    fun checkForUpdates_delegatesToWrappedRepository() = runTest {
        val metadata = SyncMetadata(lastUpdated = 123L, hasUpdates = true)
        val delegate = mockk<SyncRepository> {
            coEvery { checkForUpdates(123L) } returns metadata
        }

        val result = LoggingSyncRepositoryDecorator(delegate).checkForUpdates(123L)

        assertEquals(metadata, result)
        coVerify(exactly = 1) { delegate.checkForUpdates(123L) }
    }

    @Test
    fun fetchAllData_delegatesToWrappedRepository() = runTest {
        val cachedData = TestDataFactory.syncData(lastUpdated = 10L)
        val freshData = TestDataFactory.syncData(lastUpdated = 20L)
        var providerWasInvoked = false
        val delegate = mockk<SyncRepository> {
            coEvery { fetchAllData(any()) } coAnswers {
                val provider = firstArg<(() -> SyncData?)?>()
                providerWasInvoked = provider?.invoke() == cachedData
                SyncFetchResult.Updated(freshData)
            }
        }

        val result = LoggingSyncRepositoryDecorator(delegate).fetchAllData { cachedData }

        assertEquals(SyncFetchResult.Updated(freshData), result)
        assertTrue(providerWasInvoked)
        coVerify(exactly = 1) { delegate.fetchAllData(any()) }
    }

    @Test
    fun decorator_doesNotSwallowDelegateExceptions() = runTest {
        val expected = SyncError.NetworkError(IOException("offline"))
        val delegate = mockk<SyncRepository> {
            coEvery { checkForUpdates(any()) } throws expected
        }

        val error = runCatching {
            LoggingSyncRepositoryDecorator(delegate).checkForUpdates(0L)
        }.exceptionOrNull()

        assertSame(expected, error)
    }
}
