package com.rsl.dictionary.models

import com.rsl.dictionary.services.network.http.ApiJsonDecoder
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LessonSerializationTest {
    @Test
    fun decode_mapsVideoUrlAndTimestampFields() {
        val json = """
            {
              "id": "lesson-1",
              "title": "Базовые жесты",
              "description": "Вводный урок",
              "video_url": "https://example.com/videos/lesson-1.mp4",
              "order": 0,
              "created_at": 1700000000,
              "updated_at": 1700000100
            }
        """.trimIndent()

        val lesson = ApiJsonDecoder.json.decodeFromString<Lesson>(json)

        assertEquals("https://example.com/videos/lesson-1.mp4", lesson.videoUrl)
        assertEquals(Long::class.javaObjectType, lesson.createdAt?.javaClass)
        assertEquals(Long::class.javaObjectType, lesson.updatedAt?.javaClass)
    }

    @Test
    fun decode_allowsMissingOptionalTimestamps_andIgnoresUnknownFields() {
        val json = """
            {
              "id": "lesson-1",
              "title": "Базовые жесты",
              "description": "Вводный урок",
              "video_url": "https://example.com/videos/lesson-1.mp4",
              "order": 0,
              "unknown": "ignored"
            }
        """.trimIndent()

        val lesson = ApiJsonDecoder.json.decodeFromString<Lesson>(json)

        assertNull(lesson.createdAt)
        assertNull(lesson.updatedAt)
    }
}
