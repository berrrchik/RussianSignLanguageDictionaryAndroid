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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ErrorMessageMapperTest {
    @Test
    fun map_returnsMessagesForKnownDomainErrors() {
        val cases = listOf(
            SyncError.NoInternet to "Нет подключения к интернету",
            SyncError.ServerUnavailable to "Сервер недоступен",
            SyncError.NetworkError(IllegalStateException()) to "Ошибка сети",
            SyncError.DecodingError(IllegalArgumentException()) to "Ошибка обработки данных",
            SignRepositoryError.NoDataAvailable to "Данные недоступны",
            SignRepositoryError.ServerUnavailable to "Сервер недоступен",
            SignRepositoryError.NetworkError(IllegalStateException()) to "Ошибка сети",
            SignRepositoryError.DecodingError(IllegalStateException()) to "Ошибка обработки данных",
            SignRepositoryError.NotFound to "Жест не найден",
            LessonRepositoryError.NoDataAvailable to "Данные недоступны",
            LessonRepositoryError.ServerUnavailable to "Сервер недоступен",
            LessonRepositoryError.NetworkError(IllegalStateException()) to "Ошибка сети",
            LessonVideoError.NoInternet to "Для просмотра урока требуется интернет",
            LessonVideoError.ServerUnavailable to "Видео урока сейчас недоступно",
            LessonVideoError.UrlNotFound to "Видео урока сейчас недоступно",
            LessonVideoError.UnknownError(IllegalStateException()) to "Видео урока сейчас недоступно",
            VideoRepositoryError.NoInternet to "Для загрузки нового видео требуется сеть",
            VideoRepositoryError.UrlNotFound to "Видео сейчас недоступно",
            VideoRepositoryError.DownloadFailed(IllegalStateException()) to "Видео сейчас недоступно",
            VideoRepositoryError.CacheError(IllegalStateException()) to "Видео сейчас недоступно",
            VideoCacheError.DiskFull to "Недостаточно места на устройстве",
            LessonRepositoryError.NotFound to "Урок не найден"
        )

        cases.forEach { (error, expectedMessage) ->
            assertEquals(expectedMessage, ErrorMessageMapper.map(error))
        }
    }

    @Test
    fun map_returnsGenericMessageForUnknownError() {
        assertEquals("Произошла неизвестная ошибка", ErrorMessageMapper.map(IllegalStateException()))
    }

    @Test
    fun map_returnsMessagesForRepositoryStatuses() {
        assertNull(ErrorMessageMapper.map(RepositoryDataStatus.Idle))
        assertEquals("Данные обновлены", ErrorMessageMapper.map(RepositoryDataStatus.Updated))
        assertEquals("Данные актуальны", ErrorMessageMapper.map(RepositoryDataStatus.UpToDate))
        assertEquals(
            "Нет интернета. Показаны сохранённые данные.",
            ErrorMessageMapper.map(RepositoryDataStatus.UsingCachedData(DataStatusReason.NoInternet))
        )
        assertEquals(
            "Сервер недоступен. Показаны сохранённые данные.",
            ErrorMessageMapper.map(
                RepositoryDataStatus.UsingCachedData(DataStatusReason.ServerUnavailable)
            )
        )
        assertEquals(
            "Для первого запуска требуется интернет",
            ErrorMessageMapper.map(RepositoryDataStatus.NoData(DataStatusReason.NoInternet))
        )
        assertEquals(
            "Сервер недоступен. Попробуйте позже.",
            ErrorMessageMapper.map(RepositoryDataStatus.NoData(DataStatusReason.ServerUnavailable))
        )
    }

    @Test
    fun map_returnsMessagesForScreenAndIndicatorStatuses() {
        assertEquals(
            "Нет интернета. Показаны сохранённые данные.",
            ErrorMessageMapper.map(ScreenDataStatus.LoadedWithCachedWarning(DataStatusReason.NoInternet))
        )
        assertEquals(
            "Сервер недоступен. Попробуйте позже.",
            ErrorMessageMapper.map(ScreenDataStatus.Error(DataStatusReason.ServerUnavailable))
        )
        assertEquals("Нет интернета", ErrorMessageMapper.map(OfflineIndicatorStatus.NoInternet))
    }
}
