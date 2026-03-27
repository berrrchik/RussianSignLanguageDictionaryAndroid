package com.rsl.dictionary.repositories.protocols

import com.rsl.dictionary.models.Lesson

interface LessonRepository {
    suspend fun getAllLessons(): List<Lesson>
    suspend fun getLesson(byId: String): Lesson
}
