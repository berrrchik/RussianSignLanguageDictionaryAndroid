package com.rsl.dictionary.errors

sealed class LessonRepositoryError(cause: Throwable? = null) : Exception(cause) {
    object NoDataAvailable : LessonRepositoryError()
    data class NetworkError(override val cause: Throwable) : LessonRepositoryError(cause)
    object NotFound : LessonRepositoryError()
    object UnknownError : LessonRepositoryError()
}
