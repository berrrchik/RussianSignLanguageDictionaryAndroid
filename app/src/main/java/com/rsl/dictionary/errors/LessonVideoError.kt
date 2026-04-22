package com.rsl.dictionary.errors

sealed class LessonVideoError(cause: Throwable? = null) : Exception(cause) {
    object UrlNotFound : LessonVideoError()
    object NoInternet : LessonVideoError()
    object ServerUnavailable : LessonVideoError()
    data class UnknownError(override val cause: Throwable) : LessonVideoError(cause)
}
