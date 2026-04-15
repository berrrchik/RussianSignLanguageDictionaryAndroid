package com.rsl.dictionary.utilities

import com.rsl.dictionary.errors.LessonRepositoryError
import com.rsl.dictionary.errors.SignRepositoryError
import com.rsl.dictionary.errors.SyncError
import com.rsl.dictionary.errors.VideoCacheError
import com.rsl.dictionary.errors.VideoRepositoryError

object ErrorMessageMapper {
    fun map(error: Throwable): String = when (error) {
        is SyncError.NoInternet -> "Нет подключения к интернету"
        is SyncError.ServerUnavailable -> "Сервер недоступен"
        is SyncError.NetworkError -> "Ошибка сети"
        is SyncError.DecodingError -> "Ошибка обработки данных"
        is SignRepositoryError.NoDataAvailable -> "Данные недоступны"
        is SignRepositoryError.ServerUnavailable -> "Сервер недоступен"
        is SignRepositoryError.NetworkError -> "Ошибка сети"
        is SignRepositoryError.DecodingError -> "Ошибка обработки данных"
        is SignRepositoryError.NotFound -> "Жест не найден"
        is LessonRepositoryError.NoDataAvailable -> "Данные недоступны"
        is LessonRepositoryError.ServerUnavailable -> "Сервер недоступен"
        is LessonRepositoryError.NetworkError -> "Ошибка сети"
        is VideoRepositoryError.NoInternet -> "Нет подключения к интернету"
        is VideoRepositoryError.UrlNotFound,
        is VideoRepositoryError.DownloadFailed,
        is VideoRepositoryError.CacheError,
        is VideoRepositoryError.UnknownError -> "Видео сейчас недоступно"
        is VideoCacheError.DiskFull -> "Недостаточно места на устройстве"
        is LessonRepositoryError.NotFound -> "Урок не найден"
        else -> "Произошла неизвестная ошибка"
    }
}
