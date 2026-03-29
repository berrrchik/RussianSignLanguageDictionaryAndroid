package com.rsl.dictionary.repositories.impl

import com.rsl.dictionary.errors.LessonRepositoryError
import com.rsl.dictionary.errors.SignRepositoryError
import com.rsl.dictionary.models.Lesson
import com.rsl.dictionary.repositories.protocols.LessonRepository
import com.rsl.dictionary.repositories.protocols.SignRepository
import javax.inject.Inject

class LessonRepositoryImpl @Inject constructor(
    private val signRepository: SignRepository
) : LessonRepository {

    override suspend fun getAllLessons(): List<Lesson> {
        return try {
            signRepository.loadDataWithSync().lessons.sortedBy { it.order }
        } catch (error: SignRepositoryError.NoDataAvailable) {
            throw LessonRepositoryError.NoDataAvailable
        } catch (error: SignRepositoryError.NetworkError) {
            throw LessonRepositoryError.NetworkError(error.cause)
        } catch (_: SignRepositoryError) {
            throw LessonRepositoryError.UnknownError
        }
    }

    override suspend fun getLesson(byId: String): Lesson {
        return getAllLessons().firstOrNull { it.id == byId }
            ?: throw LessonRepositoryError.NotFound
    }
}
