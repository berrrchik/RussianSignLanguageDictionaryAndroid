package com.rsl.dictionary.errors

sealed class SBERTSearchError(cause: Throwable? = null) : Exception(cause) {
    data class NetworkError(override val cause: Throwable) : SBERTSearchError(cause)
    data class ServerError(val code: Int) : SBERTSearchError()
    data class DecodingError(override val cause: Throwable) : SBERTSearchError(cause)
    object UnknownError : SBERTSearchError()
}
