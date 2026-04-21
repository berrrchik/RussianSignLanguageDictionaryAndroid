package com.rsl.dictionary.viewmodels

import com.rsl.dictionary.errors.LessonRepositoryError
import com.rsl.dictionary.models.DataStatusReason
import com.rsl.dictionary.models.RepositoryDataStatus
import com.rsl.dictionary.models.ScreenDataStatus
import com.rsl.dictionary.repositories.protocols.LessonRepository
import com.rsl.dictionary.repositories.protocols.SignRepository
import com.rsl.dictionary.repositories.protocols.SignRepositoryRefreshState
import com.rsl.dictionary.testing.factories.TestDataFactory
import com.rsl.dictionary.testing.rules.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LessonsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_callsLoadLessons() = runTest {
        val lessons = listOf(TestDataFactory.lesson(id = "lesson-1", title = "Первый"))
        val lessonRepository = mockk<LessonRepository> {
            coEvery { getAllLessons() } returns lessons
        }

        val viewModel = LessonsViewModel(lessonRepository, mockSignRepository())
        advanceUntilIdle()

        coVerify(exactly = 1) { lessonRepository.getAllLessons() }
        assertEquals(lessons, viewModel.lessons.value)
    }

    @Test
    fun successfulLoad_updatesLessons() = runTest {
        val lessons = listOf(
            TestDataFactory.lesson(id = "lesson-1", title = "А"),
            TestDataFactory.lesson(id = "lesson-2", title = "Б")
        )
        val lessonRepository = mockk<LessonRepository> {
            coEvery { getAllLessons() } returns lessons
        }

        val viewModel = LessonsViewModel(lessonRepository, mockSignRepository())
        advanceUntilIdle()

        assertEquals(lessons, viewModel.lessons.value)
        assertNull(viewModel.error.value)
        assertEquals(ScreenDataStatus.Loaded, viewModel.screenStatus.value)
    }

    @Test
    fun loadFailure_mapsError() = runTest {
        val lessonRepository = mockk<LessonRepository> {
            coEvery { getAllLessons() } throws LessonRepositoryError.NetworkError(IOException("offline"))
        }

        val viewModel = LessonsViewModel(
            lessonRepository,
            mockSignRepository(
                MutableStateFlow(RepositoryDataStatus.NoData(DataStatusReason.NoInternet))
            )
        )
        advanceUntilIdle()

        assertEquals("Для первого запуска требуется интернет", viewModel.error.value)
        assertEquals(ScreenDataStatus.Error(DataStatusReason.NoInternet), viewModel.screenStatus.value)
    }

    @Test
    fun repeatedLoadLessons_replacesPreviousStateCleanly() = runTest {
        val firstLoad = listOf(TestDataFactory.lesson(id = "lesson-1", title = "Первый"))
        val secondLoad = listOf(
            TestDataFactory.lesson(id = "lesson-2", title = "Второй"),
            TestDataFactory.lesson(id = "lesson-3", title = "Третий")
        )
        val lessonRepository = mockk<LessonRepository> {
            coEvery { getAllLessons() } returnsMany listOf(firstLoad, secondLoad)
        }
        val viewModel = LessonsViewModel(lessonRepository, mockSignRepository())
        advanceUntilIdle()

        viewModel.loadLessons()
        advanceUntilIdle()

        assertEquals(secondLoad, viewModel.lessons.value)
        assertNull(viewModel.error.value)
    }

    @Test
    fun cachedDataWarning_isExposedThroughScreenStatus() = runTest {
        val lessons = listOf(TestDataFactory.lesson(id = "lesson-1", title = "Первый"))
        val dataStatus = MutableStateFlow<RepositoryDataStatus>(RepositoryDataStatus.Idle)
        val lessonRepository = mockk<LessonRepository> {
            coEvery { getAllLessons() } returns lessons
        }

        val viewModel = LessonsViewModel(lessonRepository, mockSignRepository(dataStatus))
        advanceUntilIdle()

        dataStatus.value = RepositoryDataStatus.UsingCachedData(DataStatusReason.ServerUnavailable)
        advanceUntilIdle()

        assertEquals(
            ScreenDataStatus.LoadedWithCachedWarning(DataStatusReason.ServerUnavailable),
            viewModel.screenStatus.value
        )
        assertEquals("Сервер недоступен. Показаны сохранённые данные.", viewModel.statusMessage.value)
    }

    private fun mockSignRepository(
        dataStatus: MutableStateFlow<RepositoryDataStatus> = MutableStateFlow(RepositoryDataStatus.Idle)
    ): SignRepository {
        return mockk {
            every { this@mockk.dataStatus } returns dataStatus
            every { syncData } returns MutableStateFlow(null)
            every { refreshState } returns MutableStateFlow(SignRepositoryRefreshState.Idle)
        }
    }
}
