package com.rsl.dictionary.services.network.http

import com.rsl.dictionary.errors.SyncError
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpResponseHandlerTest {
    private val handler = HttpResponseHandler()

    @Test
    fun handle_200ReturnsSuccess() {
        val response = response(code = 200, body = """{"ok":true}""", etag = "etag-1")

        val result = handler.handle(response) { body -> "parsed:$body" }

        assertTrue(result is HttpResult.Success)
        result as HttpResult.Success
        assertEquals("""parsed:{"ok":true}""", result.data)
        assertEquals("etag-1", result.etag)
    }

    @Test
    fun handle_304ReturnsNotModified() {
        val response = response(code = 304, body = null, etag = "etag-2")

        val result = handler.handle(response) { body -> body }

        assertTrue(result is HttpResult.NotModified)
        result as HttpResult.NotModified
        assertEquals("etag-2", result.etag)
    }

    @Test(expected = SyncError.ServerUnavailable::class)
    fun handle_5xxThrowsServerUnavailable() {
        val response = response(code = 503, body = "unavailable", message = "Service Unavailable")

        handler.handle(response) { body -> body }
    }

    @Test
    fun handle_4xxReturnsError() {
        val response = response(code = 404, body = "missing", message = "Not Found")

        val result = handler.handle(response) { body -> body }

        assertTrue(result is HttpResult.Error)
        result as HttpResult.Error
        assertEquals(404, result.code)
        assertEquals("Not Found", result.message)
    }

    private fun response(
        code: Int,
        body: String?,
        message: String = "OK",
        etag: String? = null
    ): Response {
        val builder = Response.Builder()
            .request(Request.Builder().url("https://example.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(message)

        if (etag != null) {
            builder.header("ETag", etag)
        }

        if (body != null) {
            builder.body(body.toResponseBody())
        }

        return builder.build()
    }
}
