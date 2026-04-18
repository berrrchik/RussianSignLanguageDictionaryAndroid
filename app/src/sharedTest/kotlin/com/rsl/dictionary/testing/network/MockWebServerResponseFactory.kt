package com.rsl.dictionary.testing.network

import okhttp3.mockwebserver.MockResponse

object MockWebServerResponseFactory {
    fun jsonOk(body: String): MockResponse = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)

    fun notModified(etag: String? = null): MockResponse = MockResponse()
        .setResponseCode(304)
        .apply {
            if (etag != null) {
                setHeader("ETag", etag)
            }
        }
}
