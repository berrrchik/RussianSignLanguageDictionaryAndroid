package com.rsl.dictionary.repositories.impl

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.rsl.dictionary.errors.LessonVideoError
import com.rsl.dictionary.services.cache.VideoCacheDirectoryManager
import com.rsl.dictionary.services.network.NetworkMonitor
import com.rsl.dictionary.testing.network.MockWebServerClientFactory
import java.io.File
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import io.mockk.every
import io.mockk.mockk

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LessonVideoRepositoryImplTest {
    private lateinit var context: Context
    private lateinit var server: MockWebServer
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var repository: LessonVideoRepositoryImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        server = MockWebServer()
        server.start()
        networkMonitor = mockk {
            every { isConnected() } returns true
        }
        repository = LessonVideoRepositoryImpl(
            okHttpClient = MockWebServerClientFactory.create(server),
            networkMonitor = networkMonitor
        )
        VideoCacheDirectoryManager.shortTermDir(context).deleteRecursively()
        VideoCacheDirectoryManager.favoritesDir(context).deleteRecursively()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun getLessonVideoUri_returnsRemoteUriWithoutCreatingCacheFiles() = runTest {
        server.enqueue(MockResponse().setResponseCode(206).setBody("a"))

        val result = repository.getLessonVideoUri("https://example.com/videos/lesson-1.mp4")

        assertEquals("https://example.com/videos/lesson-1.mp4", result.toString())
        assertTrue(VideoCacheDirectoryManager.shortTermDir(context).listFiles().orEmpty().isEmpty())
        assertTrue(VideoCacheDirectoryManager.favoritesDir(context).listFiles().orEmpty().isEmpty())
    }

    @Test
    fun repeatedPlaybackDoesNotCreateOfflineReplayCapability() = runTest {
        server.enqueue(MockResponse().setResponseCode(206).setBody("a"))
        repository.getLessonVideoUri("https://example.com/videos/lesson-1.mp4")
        every { networkMonitor.isConnected() } returns false

        val error = runCatching {
            repository.getLessonVideoUri("https://example.com/videos/lesson-1.mp4")
        }.exceptionOrNull()

        assertTrue(error is LessonVideoError.NoInternet)
        assertTrue(VideoCacheDirectoryManager.shortTermDir(context).listFiles().orEmpty().isEmpty())
        assertTrue(VideoCacheDirectoryManager.favoritesDir(context).listFiles().orEmpty().isEmpty())
    }

    @Test
    fun serverFailure_mapsToServerUnavailable() = runTest {
        server.enqueue(MockResponse().setResponseCode(503).setBody("down"))

        val error = runCatching {
            repository.getLessonVideoUri("https://example.com/videos/lesson-1.mp4")
        }.exceptionOrNull()

        assertTrue(error is LessonVideoError.ServerUnavailable)
    }
}
