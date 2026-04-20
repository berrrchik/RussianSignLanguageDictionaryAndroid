package com.rsl.dictionary.utilities.cache

import android.net.Uri
import com.rsl.dictionary.testing.rules.MainDispatcherRule
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VideoDownloadCoordinatorTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun sameVideoId_parallelCallsUseSingleDownloader() = runTest {
        val coordinator = VideoDownloadCoordinator()
        val release = CompletableDeferred<Uri>()
        var downloaderCalls = 0

        val firstCaller = async {
            coordinator.download(videoId = 1, scope = backgroundScope) {
                downloaderCalls += 1
                release.await()
            }
        }
        val secondCaller = async {
            coordinator.download(videoId = 1, scope = backgroundScope) {
                downloaderCalls += 1
                mockk()
            }
        }

        runCurrent()
        assertEquals(1, downloaderCalls)

        val expected = mockk<Uri>()
        release.complete(expected)

        assertSame(expected, firstCaller.await())
        assertSame(expected, secondCaller.await())
        assertEquals(1, downloaderCalls)
    }

    @Test
    fun differentVideoIds_doNotBlockEachOther() = runTest {
        val coordinator = VideoDownloadCoordinator()
        val firstStarted = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var downloaderCalls = 0
        val firstResult = mockk<Uri>()
        val secondResult = mockk<Uri>()

        val firstCaller = async {
            coordinator.download(videoId = 1, scope = backgroundScope) {
                downloaderCalls += 1
                firstStarted.complete(Unit)
                release.await()
                firstResult
            }
        }
        val secondCaller = async {
            coordinator.download(videoId = 2, scope = backgroundScope) {
                downloaderCalls += 1
                secondStarted.complete(Unit)
                secondResult
            }
        }

        runCurrent()

        assertTrue(firstStarted.isCompleted)
        assertTrue(secondStarted.isCompleted)
        assertEquals(2, downloaderCalls)

        release.complete(Unit)

        assertSame(firstResult, firstCaller.await())
        assertSame(secondResult, secondCaller.await())
    }

    @Test
    fun failedDownload_clearsSlotAndAllowsRetry() = runTest {
        val coordinator = VideoDownloadCoordinator()
        val downloadScope = CoroutineScope(backgroundScope.coroutineContext + SupervisorJob())
        val expected = IllegalStateException("download failed")
        var downloaderCalls = 0
        val retryResult = mockk<Uri>()

        val firstError = runCatching {
            coordinator.download(videoId = 7, scope = downloadScope) {
                downloaderCalls += 1
                throw expected
            }
        }.exceptionOrNull()

        val secondResult = coordinator.download(videoId = 7, scope = downloadScope) {
            downloaderCalls += 1
            retryResult
        }

        assertTrue(firstError is IllegalStateException)
        assertEquals("download failed", firstError?.message)
        assertSame(retryResult, secondResult)
        assertEquals(2, downloaderCalls)
    }
}
