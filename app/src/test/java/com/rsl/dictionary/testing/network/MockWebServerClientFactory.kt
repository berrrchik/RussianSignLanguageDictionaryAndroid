package com.rsl.dictionary.testing.network

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockWebServer

object MockWebServerClientFactory {
    fun create(server: MockWebServer): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val rewrittenRequest = originalRequest.newBuilder()
                    .url(server.rewrite(originalRequest))
                    .build()
                chain.proceed(rewrittenRequest)
            }
            .build()
    }

    private fun MockWebServer.rewrite(request: Request): HttpUrl {
        return url("/")
            .newBuilder()
            .encodedPath(request.url.encodedPath)
            .encodedQuery(request.url.encodedQuery)
            .build()
    }
}
