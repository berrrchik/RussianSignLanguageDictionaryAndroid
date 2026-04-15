package com.rsl.dictionary.repositories.impl

import com.rsl.dictionary.errors.LessonRepositoryError
import com.rsl.dictionary.errors.SignRepositoryError
import com.rsl.dictionary.repositories.protocols.SignRepository
import com.rsl.dictionary.testing.fakes.FakeSignRepository
import com.rsl.dictionary.testing.factories.TestDataFactory
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonRepositoryImplTest {
    @Test
    fun getAllLessons_sortsByOrder() = runTest {
        val signRepository = FakeSignRepository(
            TestDataFactory.syncData(
                lessons = listOf(
                    TestDataFactory.lesson(id = "2", order = 2),
                    TestDataFactory.lesson(id = "1", order = 1)
                )
            )
        )
        val repository = LessonRepositoryImpl(signRepository)

        val result = repository.getAllLessons()

        assertEquals(listOf("1", "2"), result.map { it.id })
    }

    @Test
    fun getLesson_returnsRequestedLesson() = runTest {
        val signRepository = FakeSignRepository(
            TestDataFactory.syncData(
                lessons = listOf(
                    TestDataFactory.lesson(id = "lesson-1"),
                    TestDataFactory.lesson(id = "lesson-2")
                )
            )
        )
        val repository = LessonRepositoryImpl(signRepository)

        val result = repository.getLesson("lesson-2")

        assertEquals("lesson-2", result.id)
    }

    @Test
    fun unknownLessonId_throwsNotFound() = runTest {
        val repository = LessonRepositoryImpl(FakeSignRepository())

        val error = runCatching { repository.getLesson("missing") }.exceptionOrNull()

        assertTrue(error is LessonRepositoryError.NotFound)
    }

    @Test
    fun signRepositoryErrors_areMapped() = runTest {
        val signRepository = mockk<SignRepository> {
            coEvery { loadDataWithSync() } throws SignRepositoryError.NetworkError(IllegalStateException("offline"))
        }
        val repository = LessonRepositoryImpl(signRepository)

        val error = runCatching { repository.getAllLessons() }.exceptionOrNull()

        assertTrue(error is LessonRepositoryError.NetworkError)
        assertEquals("offline", error?.cause?.message)
    }

    @Test
    fun signRepositoryServerUnavailable_isMapped() = runTest {
        val signRepository = mockk<SignRepository> {
            coEvery { loadDataWithSync() } throws SignRepositoryError.ServerUnavailable
        }
        val repository = LessonRepositoryImpl(signRepository)

        val error = runCatching { repository.getAllLessons() }.exceptionOrNull()

        assertTrue(error is LessonRepositoryError.ServerUnavailable)
    }
}
