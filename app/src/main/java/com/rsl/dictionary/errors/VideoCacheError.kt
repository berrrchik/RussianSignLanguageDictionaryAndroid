package com.rsl.dictionary.errors

sealed class VideoCacheError(cause: Throwable? = null) : Exception(cause) {
    data class DownloadFailed(override val cause: Throwable) : VideoCacheError(cause)
    object FileNotFound : VideoCacheError()
    object DiskFull : VideoCacheError()
    object UnknownError : VideoCacheError()
}
