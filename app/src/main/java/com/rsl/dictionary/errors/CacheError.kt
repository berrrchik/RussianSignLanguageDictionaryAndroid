package com.rsl.dictionary.errors

sealed class CacheError(cause: Throwable? = null) : Exception(cause) {
    data class ReadFailed(override val cause: Throwable) : CacheError(cause)
    data class WriteFailed(override val cause: Throwable) : CacheError(cause)
    data class DeleteFailed(override val cause: Throwable) : CacheError(cause)
    object NotFound : CacheError()
}
