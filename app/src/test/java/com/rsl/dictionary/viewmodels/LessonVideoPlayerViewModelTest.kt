package com.rsl.dictionary.viewmodels

import android.net.Uri
import com.rsl.dictionary.errors.LessonVideoError
import com.rsl.dictionary.repositories.protocols.LessonVideoRepository
import com.rsl.dictionary.testing.rules.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LessonVideoPlayerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var lessonVideoRepository: LessonVideoRepository
    private lateinit var viewModel: LessonVideoPlayerViewModel

    @Before
    fun setUp() {
        lessonVideoRepository = mockk()
        viewModel = LessonVideoPlayerViewModel(lessonVideoRepository)
    }

    @Test
    fun loadVideo_mapsNoInternetIntoLessonSpecificMessage() = runTest {
        coEvery {
            lessonVideoRepository.getLessonVideoUri("lesson.mp4")
        } throws LessonVideoError.NoInternet

        viewModel.loadVideo("lesson.mp4")
        advanceUntilIdle()

        assertEquals("Для просмотра урока требуется интернет", viewModel.error.value)
        assertNull(viewModel.videoUri.value)
    }

    @Test
    fun loadVideo_setsRemoteUriOnSuccess() = runTest {
        val expected = mockk<Uri>()
        coEvery {
            lessonVideoRepository.getLessonVideoUri("lesson.mp4")
        } returns expected

        viewModel.loadVideo("lesson.mp4")
        advanceUntilIdle()

        assertEquals(expected, viewModel.videoUri.value)
        assertNull(viewModel.error.value)
    }
}
