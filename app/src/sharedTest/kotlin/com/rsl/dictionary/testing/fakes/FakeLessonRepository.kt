package com.rsl.dictionary.testing.fakes

import com.rsl.dictionary.models.Lesson
import com.rsl.dictionary.repositories.protocols.LessonRepository
import com.rsl.dictionary.testing.factories.TestDataFactory

class FakeLessonRepository(
    private var lessons: List<Lesson> = listOf(TestDataFactory.lesson())
) : LessonRepository {
    override suspend fun getAllLessons(): List<Lesson> = lessons

    override suspend fun getLesson(byId: String): Lesson {
        return lessons.firstOrNull { it.id == byId }
            ?: error("Lesson with id=$byId was not found in FakeLessonRepository")
    }

    fun replaceLessons(updatedLessons: List<Lesson>) {
        lessons = updatedLessons
    }
}
