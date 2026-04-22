package com.rsl.dictionary.services.search

import com.rsl.dictionary.errors.SBERTSearchError
import com.rsl.dictionary.testing.network.MockWebServerClientFactory
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SBERTSearchServiceMockWebServerTest {
    private lateinit var server: MockWebServer
    private lateinit var service: SBERTSearchService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        service = SBERTSearchService(MockWebServerClientFactory.create(server))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun successTrue_returnsResults() = kotlinx.coroutines.test.runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "success": true,
                      "data": {
                        "results": [
                          {
                            "id": "sign-1",
                            "word": "Привет",
                            "similarity": 0.98
                          }
                        ]
                      }
                    }
                    """.trimIndent()
                )
        )

        val result = service.search("привет", limit = 5)

        assertEquals(1, result.size)
        assertEquals("sign-1", result.first().id)
    }

    @Test
    fun successFalse_throwsServerError() = kotlinx.coroutines.test.runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"success":false,"error":{"message":"bad","code":500}}""")
        )

        val error = runCatching {
            service.search("привет", limit = 5)
        }.exceptionOrNull()

        assertTrue(error is SBERTSearchError.ServerError)
        assertEquals(500, (error as SBERTSearchError.ServerError).code)
    }

    @Test
    fun networkFailure_throwsNetworkError() = kotlinx.coroutines.test.runTest {
        server.shutdown()

        val error = runCatching {
            service.search("привет", limit = 5)
        }.exceptionOrNull()

        assertTrue(error is SBERTSearchError.NetworkError)
    }

    @Test
    fun badJson_throwsDecodingError() = kotlinx.coroutines.test.runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{bad json")
        )

        val error = runCatching {
            service.search("привет", limit = 5)
        }.exceptionOrNull()

        assertTrue(error is SBERTSearchError.DecodingError)
    }

    @Test
    fun problematicQueryCharacters_documentCurrentManualJsonRisk() = kotlinx.coroutines.test.runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"success":true,"data":{"results":[]}}""")
        )

        service.search("привет\"мир", limit = 5)
        val recordedRequest = server.takeRequest()
        val body = recordedRequest.body.readUtf8()

        assertTrue(body.contains("привет\"мир"))
        assertTrue(!body.contains("привет\\\"мир"))
    }
}
