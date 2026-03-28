package com.rsl.dictionary.services.network.http

import com.rsl.dictionary.errors.SyncError
import javax.inject.Inject
import okhttp3.Response

sealed class HttpResult<out T> {
    data class Success<T>(val data: T, val etag: String?) : HttpResult<T>()
    data class NotModified(val etag: String?) : HttpResult<Nothing>()
    data class Error(val code: Int, val message: String) : HttpResult<Nothing>()
}

class HttpResponseHandler @Inject constructor() {
    fun <T> handle(response: Response, parser: (String) -> T): HttpResult<T> {
        val etag = response.header("ETag")

        return when {
            response.code == 200 -> {
                val body = response.body?.string().orEmpty()
                HttpResult.Success(parser(body), etag)
            }

            response.code == 304 -> HttpResult.NotModified(etag)
            response.code in 500..599 -> throw SyncError.ServerUnavailable
            else -> HttpResult.Error(response.code, response.message)
        }
    }
}
