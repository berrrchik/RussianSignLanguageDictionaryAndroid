package com.rsl.dictionary.utilities

import com.rsl.dictionary.errors.LessonRepositoryError
import com.rsl.dictionary.errors.LessonVideoError
import com.rsl.dictionary.errors.SignRepositoryError
import com.rsl.dictionary.errors.SyncError
import com.rsl.dictionary.errors.VideoCacheError
import com.rsl.dictionary.errors.VideoRepositoryError
import com.rsl.dictionary.models.DataStatusReason
import com.rsl.dictionary.models.OfflineIndicatorStatus
import com.rsl.dictionary.models.RepositoryDataStatus
import com.rsl.dictionary.models.ScreenDataStatus

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
        is LessonVideoError.NoInternet -> "Для просмотра урока требуется интернет"
        is LessonVideoError.ServerUnavailable -> "Видео урока сейчас недоступно"
        is LessonVideoError.UrlNotFound,
        is LessonVideoError.UnknownError -> "Видео урока сейчас недоступно"
        is VideoRepositoryError.NoInternet -> "Для загрузки нового видео требуется сеть"
        is VideoRepositoryError.UrlNotFound,
        is VideoRepositoryError.DownloadFailed,
        is VideoRepositoryError.CacheError,
        is VideoRepositoryError.UnknownError -> "Видео сейчас недоступно"
        is VideoCacheError.DiskFull -> "Недостаточно места на устройстве"
        is LessonRepositoryError.NotFound -> "Урок не найден"
        else -> "Произошла неизвестная ошибка"
    }

    fun map(status: RepositoryDataStatus): String? = when (status) {
        RepositoryDataStatus.Idle -> null
        RepositoryDataStatus.Updated -> "Данные обновлены"
        RepositoryDataStatus.UpToDate -> "Данные актуальны"
        is RepositoryDataStatus.UsingCachedData -> mapCachedData(status.reason)
        is RepositoryDataStatus.NoData -> mapNoData(status.reason)
    }

    fun map(status: ScreenDataStatus): String? = when (status) {
        ScreenDataStatus.Loaded -> null
        ScreenDataStatus.Updated -> "Данные обновлены"
        ScreenDataStatus.UpToDate -> "Данные актуальны"
        is ScreenDataStatus.LoadedWithCachedWarning -> mapCachedData(status.reason)
        is ScreenDataStatus.Error -> mapNoData(status.reason)
    }

    fun map(status: OfflineIndicatorStatus): String = when (status) {
        OfflineIndicatorStatus.NoInternet -> "Нет интернета"
        is OfflineIndicatorStatus.UsingCachedData -> mapCachedData(status.reason)
        is OfflineIndicatorStatus.NoData -> mapNoData(status.reason)
    }

    private fun mapCachedData(reason: DataStatusReason): String = when (reason) {
        DataStatusReason.NoInternet -> "Нет интернета. Показаны сохранённые данные."
        DataStatusReason.ServerUnavailable -> "Сервер недоступен. Показаны сохранённые данные."
    }

    private fun mapNoData(reason: DataStatusReason): String = when (reason) {
        DataStatusReason.NoInternet -> "Для первого запуска требуется интернет"
        DataStatusReason.ServerUnavailable -> "Сервер недоступен. Попробуйте позже."
    }
}
