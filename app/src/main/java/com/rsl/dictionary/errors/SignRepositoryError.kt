package com.rsl.dictionary.errors

sealed class SignRepositoryError(cause: Throwable? = null) : Exception(cause) {
    object NoDataAvailable : SignRepositoryError()
    object ServerUnavailable : SignRepositoryError()
    data class NetworkError(override val cause: Throwable) : SignRepositoryError(cause)
    data class DecodingError(override val cause: Throwable) : SignRepositoryError(cause)
    object NotFound : SignRepositoryError()
    object UnknownError : SignRepositoryError()
}
