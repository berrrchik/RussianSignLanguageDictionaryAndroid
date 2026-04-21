package com.rsl.dictionary.repositories.impl

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.rsl.dictionary.errors.SyncError
import com.rsl.dictionary.models.SyncData
import com.rsl.dictionary.models.SyncMetadata
import com.rsl.dictionary.repositories.protocols.SyncFetchResult
import com.rsl.dictionary.services.network.ETagManager
import com.rsl.dictionary.services.network.http.ApiJsonDecoder
import com.rsl.dictionary.services.network.http.HttpResponseHandler
import com.rsl.dictionary.testing.factories.TestDataFactory
import com.rsl.dictionary.testing.network.MockWebServerClientFactory
import com.rsl.dictionary.testing.network.MockWebServerResponseFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncRepositoryImplMockWebServerTest {
    private lateinit var server: MockWebServer
    private lateinit var eTagManager: ETagManager
    private lateinit var repository: SyncRepositoryImpl

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val context = ApplicationProvider.getApplicationContext<Context>()
        eTagManager = ETagManager(context)
        eTagManager.clearETag("sync_check_etag")
        eTagManager.clearETag("sync_data_etag")

        repository = SyncRepositoryImpl(
            okHttpClient = MockWebServerClientFactory.create(server),
            etagManager = eTagManager,
            httpResponseHandler = HttpResponseHandler()
        )
    }

    @After
    fun tearDown() {
        eTagManager.clearETag("sync_check_etag")
        eTagManager.clearETag("sync_data_etag")
        server.shutdown()
    }

    @Test
    fun checkForUpdates_200ReturnsMetadata() = runTest {
        val metadata = SyncMetadata(lastUpdated = 123L, hasUpdates = true)
        server.enqueue(jsonResponse(metadata, etag = validEtag))

        val result = repository.checkForUpdates(lastUpdated = 100L)

        assertEquals(metadata, result)
    }

    @Test
    fun checkForUpdates_200SavesValidETag() = runTest {
        server.enqueue(jsonResponse(SyncMetadata(lastUpdated = 123L, hasUpdates = true), etag = validEtag))

        repository.checkForUpdates(lastUpdated = 100L)

        assertEquals(validEtag, eTagManager.getETag("sync_check_etag"))
    }

    @Test
    fun checkForUpdates_304ReturnsNoUpdates() = runTest {
        eTagManager.saveETag("sync_check_etag", validEtag)
        server.enqueue(MockWebServerResponseFactory.notModified(validEtag))

        val result = repository.checkForUpdates(lastUpdated = 100L)

        assertEquals(0L, result.lastUpdated)
        assertFalse(result.hasUpdates)
        assertEquals(validEtag, server.takeRequest().getHeader("If-None-Match"))
    }

    @Test
    fun fetchAllData_200ReturnsSyncData() = runTest {
        val data = TestDataFactory.syncData()
        server.enqueue(jsonResponse(data, etag = validEtag))

        val result = repository.fetchAllData(cachedDataProvider = null)

        assertEquals(SyncFetchResult.Updated(data), result)
    }

    @Test
    fun fetchAllData_200SavesValidETag() = runTest {
        server.enqueue(jsonResponse(TestDataFactory.syncData(), etag = validEtag))

        repository.fetchAllData(cachedDataProvider = null)

        assertEquals(validEtag, eTagManager.getETag("sync_data_etag"))
    }

    @Test
    fun fetchAllData_304WithCachedProviderReturnsCachedData() = runTest {
        val cached = TestDataFactory.syncData(lastUpdated = 999L)
        eTagManager.saveETag("sync_data_etag", validEtag)
        server.enqueue(MockWebServerResponseFactory.notModified(validEtag))

        val result = repository.fetchAllData { cached }

        assertSame(cached, (result as SyncFetchResult.NotModified).data)
    }

    @Test(expected = SyncError.NoInternet::class)
    fun fetchAllData_304WithoutCachedProviderThrowsNoInternet() = runTest {
        eTagManager.saveETag("sync_data_etag", validEtag)
        server.enqueue(MockWebServerResponseFactory.notModified(validEtag))

        repository.fetchAllData(cachedDataProvider = null)
    }

    @Test
    fun ioException_mapsToNetworkError() = runTest {
        server.shutdown()

        val error = runCatching {
            repository.checkForUpdates(lastUpdated = 100L)
        }.exceptionOrNull()

        assertTrue(error is SyncError.NetworkError)
    }

    @Test
    fun badJson_mapsToDecodingError() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{bad json")
        )

        val error = runCatching {
            repository.fetchAllData(cachedDataProvider = null)
        }.exceptionOrNull()

        assertTrue(error is SyncError.DecodingError)
    }

    @Test
    fun http4xx_mapsToUnknownError() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("missing"))

        val error = runCatching {
            repository.fetchAllData(cachedDataProvider = null)
        }.exceptionOrNull()

        assertTrue(error is SyncError.UnknownError)
    }

    private fun jsonResponse(data: Any, etag: String): MockResponse {
        val body = when (data) {
            is SyncMetadata -> ApiJsonDecoder.json.encodeToString(data)
            is SyncData -> ApiJsonDecoder.json.encodeToString(data)
            else -> error("Unsupported payload type")
        }

        return MockWebServerResponseFactory.jsonOk(body)
            .setHeader("ETag", etag)
    }

    private companion object {
        const val validEtag = "1234567890abcdef1234567890abcdef"
    }
}
