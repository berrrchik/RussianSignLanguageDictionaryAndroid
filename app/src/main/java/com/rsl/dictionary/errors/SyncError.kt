package com.rsl.dictionary.errors

sealed class SyncError(cause: Throwable? = null) : Exception(cause) {
    object NoInternet : SyncError()
    object ServerUnavailable : SyncError()
    data class NetworkError(override val cause: Throwable) : SyncError(cause)
    data class DecodingError(override val cause: Throwable) : SyncError(cause)
    data class UnknownError(override val cause: Throwable? = null) : SyncError(cause)
}
