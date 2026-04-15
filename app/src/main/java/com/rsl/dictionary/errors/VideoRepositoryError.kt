package com.rsl.dictionary.errors

sealed class VideoRepositoryError(cause: Throwable? = null) : Exception(cause) {
    object UrlNotFound : VideoRepositoryError()
    object NoInternet : VideoRepositoryError()
    data class DownloadFailed(override val cause: Throwable) : VideoRepositoryError(cause)
    data class CacheError(override val cause: Throwable) : VideoRepositoryError(cause)
    object UnknownError : VideoRepositoryError()
}
