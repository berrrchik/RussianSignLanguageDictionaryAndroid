package com.rsl.dictionary.services.search

import com.rsl.dictionary.errors.SBERTSearchError
import com.rsl.dictionary.models.SBERTSearchResult
import com.rsl.dictionary.services.network.NetworkMonitor
import com.rsl.dictionary.testing.factories.TestDataFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HybridSearchServiceTest {
    @Test
    fun exactMatch_isReturnedFirst() = runTest {
        val sbert = mockk<SBERTSearchService> {
            coEvery { search(any(), any(), any()) } returns emptyList()
        }
        val networkMonitor = mockk<NetworkMonitor> {
            every { isConnected() } returns true
        }
        val service = HybridSearchService(sbert, networkMonitor)
        val signs = listOf(
            TestDataFactory.sign(id = "1", word = "Приветствие"),
            TestDataFactory.sign(id = "2", word = "Привет"),
            TestDataFactory.sign(id = "3", word = "Супривет")
        )

        val result = service.performHybridSearch("Привет", signs, limit = 10)

        assertEquals("2", result.first().id)
    }

    @Test
    fun onlinePath_callsSbertSearch() = runTest {
        val sbert = mockk<SBERTSearchService> {
            coEvery { search("привет", 5, any()) } returns emptyList()
        }
        val networkMonitor = mockk<NetworkMonitor> {
            every { isConnected() } returns true
        }
        val service = HybridSearchService(sbert, networkMonitor)

        service.performHybridSearch("привет", listOf(TestDataFactory.sign()), limit = 5)

        coVerify(exactly = 1) { sbert.search("привет", 5, any()) }
    }

    @Test
    fun sbertFailure_stillFallsBackToTextSearch() = runTest {
        val signs = listOf(
            TestDataFactory.sign(id = "1", word = "Привет"),
            TestDataFactory.sign(id = "2", word = "Приветствие")
        )
        val sbert = mockk<SBERTSearchService> {
            coEvery { search(any(), any(), any()) } throws SBERTSearchError.NetworkError(IllegalStateException())
        }
        val networkMonitor = mockk<NetworkMonitor> {
            every { isConnected() } returns true
        }
        val service = HybridSearchService(sbert, networkMonitor)

        val result = service.performHybridSearch("привет", signs, limit = 10)

        assertTrue(result.isNotEmpty())
        assertEquals(listOf("1", "2"), result.map { it.id })
    }

    @Test
    fun results_areDeduplicatedAndLimited() = runTest {
        val signs = listOf(
            TestDataFactory.sign(id = "1", word = "Привет"),
            TestDataFactory.sign(id = "2", word = "Приветствие"),
            TestDataFactory.sign(id = "3", word = "Приветик")
        )
        val sbert = mockk<SBERTSearchService> {
            coEvery { search(any(), any(), any()) } returns listOf(
                SBERTSearchResult(id = "2", word = "Приветствие", similarity = 0.95),
                SBERTSearchResult(id = "1", word = "Привет", similarity = 0.90),
                SBERTSearchResult(id = "3", word = "Приветик", similarity = 0.85)
            )
        }
        val networkMonitor = mockk<NetworkMonitor> {
            every { isConnected() } returns true
        }
        val service = HybridSearchService(sbert, networkMonitor)

        val result = service.performHybridSearch("привет", signs, limit = 2)

        assertEquals(2, result.size)
        assertEquals(listOf("1", "2"), result.map { it.id })
    }

    @Test
    fun offlinePath_skipsSbertSearch() = runTest {
        val sbert = mockk<SBERTSearchService>()
        val networkMonitor = mockk<NetworkMonitor> {
            every { isConnected() } returns false
        }
        val service = HybridSearchService(sbert, networkMonitor)
        val signs = listOf(
            TestDataFactory.sign(id = "1", word = "Привет"),
            TestDataFactory.sign(id = "2", word = "Приветствие")
        )

        val result = service.performHybridSearch("привет", signs, limit = 10)

        coVerify(exactly = 0) { sbert.search(any(), any(), any()) }
        assertEquals(listOf("1", "2"), result.map { it.id })
    }
}
