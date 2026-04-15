package com.rsl.dictionary.utilities

import com.rsl.dictionary.errors.LessonRepositoryError
import com.rsl.dictionary.errors.SignRepositoryError
import com.rsl.dictionary.errors.SyncError
import com.rsl.dictionary.errors.VideoCacheError
import com.rsl.dictionary.errors.VideoRepositoryError
import org.junit.Assert.assertEquals
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
            VideoRepositoryError.NoInternet to "Нет подключения к интернету",
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
}
