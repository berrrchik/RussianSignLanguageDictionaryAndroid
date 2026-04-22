package com.rsl.dictionary.viewmodels

import androidx.lifecycle.SavedStateHandle
import com.rsl.dictionary.errors.VideoRepositoryError
import com.rsl.dictionary.repositories.protocols.VideoRepository
import com.rsl.dictionary.testing.factories.TestDataFactory
import com.rsl.dictionary.testing.rules.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VideoPlayerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var videoRepository: VideoRepository
    private lateinit var viewModel: VideoPlayerViewModel

    @Before
    fun setUp() {
        videoRepository = mockk()
        viewModel = VideoPlayerViewModel(videoRepository, SavedStateHandle())
    }

    @Test
    fun loadVideo_mapsRepositoryErrorsToFriendlyMessage() = runTest {
        val video = TestDataFactory.video(id = 1, url = "broken.mp4")
        coEvery {
            videoRepository.getVideoURL(video, false)
        } throws VideoRepositoryError.DownloadFailed(IOException("unexpected end of stream"))

        viewModel.loadVideo(video, isFavorite = false)
        advanceUntilIdle()

        assertEquals("Видео сейчас недоступно", viewModel.error.value)
        assertNull(viewModel.videoUri.value)
    }

    @Test
    fun loadVideo_mapsNoInternetToCacheAwareMessage() = runTest {
        val video = TestDataFactory.video(id = 2, url = "new.mp4")
        coEvery {
            videoRepository.getVideoURL(video, false)
        } throws VideoRepositoryError.NoInternet

        viewModel.loadVideo(video, isFavorite = false)
        advanceUntilIdle()

        assertEquals("Для загрузки нового видео требуется сеть", viewModel.error.value)
        assertNull(viewModel.videoUri.value)
    }
}
