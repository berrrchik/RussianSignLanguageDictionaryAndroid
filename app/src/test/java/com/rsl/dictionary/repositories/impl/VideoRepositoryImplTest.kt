package com.rsl.dictionary.repositories.impl

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.rsl.dictionary.errors.VideoRepositoryError
import com.rsl.dictionary.models.FavoriteEntry
import com.rsl.dictionary.models.FavoriteOfflineStatus
import com.rsl.dictionary.models.FavoriteOfflineVideo
import com.rsl.dictionary.services.cache.VideoCacheDirectoryManager
import com.rsl.dictionary.services.cache.VideoCacheService
import com.rsl.dictionary.services.network.NetworkMonitor
import com.rsl.dictionary.testing.factories.TestDataFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VideoRepositoryImplTest {
    private lateinit var context: Context
    private lateinit var videoCacheService: VideoCacheService
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var repository: VideoRepositoryImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        videoCacheService = mockk(relaxed = true)
        networkMonitor = mockk {
            every { isConnected() } returns true
        }
        repository = VideoRepositoryImpl(context, videoCacheService, networkMonitor)
        VideoCacheDirectoryManager.shortTermDir(context).deleteRecursively()
        VideoCacheDirectoryManager.favoritesDir(context).deleteRecursively()
    }

    @Test
    fun useFavoritesCacheTrue_usesFavoritesDirectory() = runTest {
        val video = TestDataFactory.video(id = 1, url = "fav.mp4")
        val favoritesDir = VideoCacheDirectoryManager.favoritesDir(context)
        val expected = mockk<Uri>()
        every { videoCacheService.getFromMemory(video) } returns null
        coEvery { videoCacheService.getOrDownload(video, favoritesDir, context) } returns expected

        val result = repository.getVideoURL(video, useFavoritesCache = true)

        assertSame(expected, result)
        coVerify(exactly = 1) { videoCacheService.getOrDownload(video, favoritesDir, context) }
    }

    @Test
    fun shortTermCache_hasPriorityOverFavoritesCache() = runTest {
        val video = TestDataFactory.video(id = 2, url = "priority.mp4")
        val shortTermDir = VideoCacheDirectoryManager.shortTermDir(context)
        val favoritesDir = VideoCacheDirectoryManager.favoritesDir(context)
        val expected = mockk<Uri>()

        every { videoCacheService.getFromMemory(video) } returns null
        coEvery { videoCacheService.isCached(video, shortTermDir) } returns true
        coEvery { videoCacheService.getOrDownload(video, shortTermDir, context) } returns expected

        val result = repository.getVideoURL(video, useFavoritesCache = false)

        assertSame(expected, result)
        coVerify(exactly = 1) { videoCacheService.getOrDownload(video, shortTermDir, context) }
        coVerify(exactly = 0) { videoCacheService.getOrDownload(video, favoritesDir, context) }
    }

    @Test
    fun favoritesCache_isUsedAsFallback() = runTest {
        val video = TestDataFactory.video(id = 3, url = "fallback.mp4")
        val shortTermDir = VideoCacheDirectoryManager.shortTermDir(context)
        val favoritesDir = VideoCacheDirectoryManager.favoritesDir(context)
        val expected = mockk<Uri>()

        every { videoCacheService.getFromMemory(video) } returns null
        coEvery { videoCacheService.isCached(video, shortTermDir) } returns false
        coEvery { videoCacheService.isCached(video, favoritesDir) } returns true
        coEvery { videoCacheService.getOrDownload(video, favoritesDir, context) } returns expected

        val result = repository.getVideoURL(video, useFavoritesCache = false)

        assertSame(expected, result)
        coVerify(exactly = 1) { videoCacheService.getOrDownload(video, favoritesDir, context) }
    }

    @Test
    fun prepareFavoriteOffline_returnsReadyOnlyAfterAllVideosDownloaded() = runTest {
        val sign = TestDataFactory.sign(
            videos = listOf(
                TestDataFactory.video(id = 10, url = "a.mp4"),
                TestDataFactory.video(id = 20, url = "b.mp4")
            )
        )
        val favoritesDir = VideoCacheDirectoryManager.favoritesDir(context)
        every { videoCacheService.getFromMemory(any()) } returns null
        coEvery {
            videoCacheService.getOrDownload(TestDataFactory.video(id = 10, url = "a.mp4"), favoritesDir, context)
        } returns Uri.fromFile(File(favoritesDir, "video_10.mp4").apply { writeText("a") })
        coEvery {
            videoCacheService.getOrDownload(TestDataFactory.video(id = 20, url = "b.mp4"), favoritesDir, context)
        } returns Uri.fromFile(File(favoritesDir, "video_20.mp4").apply { writeText("b") })

        val result = repository.prepareFavoriteOffline(sign)

        assertTrue(result is com.rsl.dictionary.repositories.protocols.FavoriteOfflinePreparationResult.Ready)
        assertEquals(
            listOf(10, 20),
            (result as com.rsl.dictionary.repositories.protocols.FavoriteOfflinePreparationResult.Ready)
                .downloadedVideos
                .map { it.videoId }
        )
    }

    @Test
    fun prepareFavoriteOffline_returnsFailedWhenAnyVideoDownloadFails() = runTest {
        val firstVideo = TestDataFactory.video(id = 30, url = "a.mp4")
        val secondVideo = TestDataFactory.video(id = 31, url = "b.mp4")
        val sign = TestDataFactory.sign(videos = listOf(firstVideo, secondVideo))
        val favoritesDir = VideoCacheDirectoryManager.favoritesDir(context)
        every { videoCacheService.getFromMemory(any()) } returns null
        coEvery { videoCacheService.getOrDownload(firstVideo, favoritesDir, context) } returns
            Uri.fromFile(File(favoritesDir, "video_30.mp4").apply { writeText("cached") })
        coEvery { videoCacheService.getOrDownload(secondVideo, favoritesDir, context) } throws
            IOException("boom")

        val result = repository.prepareFavoriteOffline(sign)

        assertTrue(result is com.rsl.dictionary.repositories.protocols.FavoriteOfflinePreparationResult.Failed)
        result as com.rsl.dictionary.repositories.protocols.FavoriteOfflinePreparationResult.Failed
        assertEquals(listOf(30), result.downloadedVideos.map { it.videoId })
        assertTrue(result.error is VideoRepositoryError.DownloadFailed)
        assertEquals("boom", result.error.cause?.message)
    }

    @Test
    fun removeFavoriteOffline_deletesTrackedFilesFromManifest() = runTest {
        val entry = FavoriteEntry(
            signId = "sign-1",
            status = FavoriteOfflineStatus.READY_OFFLINE,
            requiredVideoIds = listOf(30, 31),
            downloadedVideos = listOf(
                FavoriteOfflineVideo(videoId = 30, fileName = "video_30.mp4"),
                FavoriteOfflineVideo(videoId = 31, fileName = "video_31.mp4")
            ),
            updatedAt = 1L
        )
        val favoritesDir = VideoCacheDirectoryManager.favoritesDir(context)
        entry.downloadedVideos.forEach { video ->
            File(favoritesDir, "video_${video.videoId}.mp4").writeText("cached")
        }

        repository.removeFavoriteOffline(entry)

        entry.downloadedVideos.forEach { video ->
            assertTrue(!File(favoritesDir, "video_${video.videoId}.mp4").exists())
        }
    }

    @Test
    fun ioException_becomesDownloadFailed() = runTest {
        val video = TestDataFactory.video(id = 4, url = "error.mp4")
        val shortTermDir = VideoCacheDirectoryManager.shortTermDir(context)

        every { videoCacheService.getFromMemory(video) } returns null
        coEvery { videoCacheService.isCached(video, any()) } returns false
        coEvery { videoCacheService.getOrDownload(video, shortTermDir, context) } throws IOException("network")

        val error = runCatching {
            repository.getVideoURL(video, useFavoritesCache = false)
        }.exceptionOrNull()

        assertTrue(error is VideoRepositoryError.DownloadFailed)
        assertEquals("network", error?.cause?.message)
    }

    @Test
    fun noInternet_cacheMissFailsImmediatelyWithoutDownload() = runTest {
        val video = TestDataFactory.video(id = 5, url = "offline.mp4")

        every { videoCacheService.getFromMemory(video) } returns null
        coEvery { videoCacheService.isCached(video, any()) } returns false
        every { networkMonitor.isConnected() } returns false

        val error = runCatching {
            repository.getVideoURL(video, useFavoritesCache = false)
        }.exceptionOrNull()

        assertTrue(error is VideoRepositoryError.NoInternet)
        coVerify(exactly = 0) { videoCacheService.getOrDownload(any(), any(), any()) }
    }
}
