package com.rsl.dictionary.services.cache

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.rsl.dictionary.errors.VideoRepositoryError
import com.rsl.dictionary.testing.factories.TestDataFactory
import com.rsl.dictionary.testing.network.MockWebServerClientFactory
import com.rsl.dictionary.utilities.cache.VideoDownloadCoordinator
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
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
class VideoCacheServiceIntegrationTest {
    private lateinit var context: Context
    private lateinit var server: MockWebServer
    private lateinit var service: VideoCacheService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        server = MockWebServer()
        server.start()
        service = VideoCacheService(
            okHttpClient = MockWebServerClientFactory.create(server),
            videoDownloadCoordinator = VideoDownloadCoordinator()
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun memoryCacheHit_returnsUriWithoutExtraDiskOrNetwork() = runTest {
        val directory = createTempDirectory()
        val video = TestDataFactory.video(id = 11, url = "memory-hit.mp4")
        server.enqueue(MockResponse().setResponseCode(200).setBody("video-bytes"))

        val first = service.getOrDownload(video, directory, context)
        val second = service.getOrDownload(video, directory, context)

        assertEquals(1, server.requestCount)
        assertEquals(first, second)
    }

    @Test
    fun diskCacheHit_returnsUriAndPlacesItIntoMemoryCache() = runTest {
        val directory = createTempDirectory()
        val video = TestDataFactory.video(id = 12, url = "disk-hit.mp4")
        val targetFile = File(directory, "video_${video.id}.mp4").apply {
            writeText("cached")
        }

        val result = service.getOrDownload(video, directory, context)

        assertEquals(Uri.fromFile(targetFile), result)
        assertEquals(result, service.getFromMemory(video))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun cacheMiss_downloadsFileFromNetwork() = runTest {
        val directory = createTempDirectory()
        val video = TestDataFactory.video(id = 13, url = "download.mp4")
        server.enqueue(MockResponse().setResponseCode(200).setBody("video-content"))

        val result = service.getOrDownload(video, directory, context)

        val targetFile = File(directory, "video_${video.id}.mp4")
        assertTrue(targetFile.exists())
        assertEquals(Uri.fromFile(targetFile), result)
        assertEquals("video-content", targetFile.readText())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun diskHit_updatesLastModified() = runTest {
        val directory = createTempDirectory()
        val video = TestDataFactory.video(id = 14, url = "touch.mp4")
        val targetFile = File(directory, "video_${video.id}.mp4").apply {
            writeText("cached")
            setLastModified(100L)
        }

        service.getOrDownload(video, directory, context)

        assertTrue(targetFile.lastModified() > 100L)
    }

    @Test
    fun parallelLoadsForSameVideo_areDeduplicated() = runTest {
        val directory = createTempDirectory()
        val video = TestDataFactory.video(id = 15, url = "parallel.mp4")
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("video-content")
                .setBodyDelay(200, java.util.concurrent.TimeUnit.MILLISECONDS)
        )

        val first = async { service.getOrDownload(video, directory, context) }
        val second = async { service.getOrDownload(video, directory, context) }

        assertEquals(first.await(), second.await())
        assertEquals(1, server.requestCount)
    }

    @Test(expected = VideoRepositoryError.UrlNotFound::class)
    fun emptyUrl_throwsUrlNotFound() = runTest {
        val directory = createTempDirectory()
        val video = TestDataFactory.video(id = 16, url = "")

        service.getOrDownload(video, directory, context)
    }

    @Test(expected = java.io.IOException::class)
    fun httpError_throwsDownloadError() = runTest {
        val directory = createTempDirectory()
        val video = TestDataFactory.video(id = 17, url = "error.mp4")
        server.enqueue(MockResponse().setResponseCode(500).setBody("error"))

        service.getOrDownload(video, directory, context)
    }

    private fun createTempDirectory(): File = Files.createTempDirectory("video-cache-service-test").toFile()
}
