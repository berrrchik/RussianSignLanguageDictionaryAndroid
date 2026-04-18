package com.rsl.dictionary.models

import com.rsl.dictionary.services.network.http.ApiJsonDecoder
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncAndSbertSerializationTest {
    @Test
    fun syncData_decodesNestedCollectionsAndLastUpdated() {
        val json = """
            {
              "categories": [
                {
                  "id": "category-1",
                  "name": "Быт",
                  "order": 0,
                  "sign_count": 1
                }
              ],
              "signs": [
                {
                  "id": "sign-1",
                  "word": "Привет",
                  "description": "Жест приветствия",
                  "category_id": "category-1",
                  "videos": [
                    {
                      "id": 101,
                      "url": "https://example.com/videos/hello.mp4",
                      "context_description": "Основной вариант",
                      "order": 0
                    }
                  ]
                }
              ],
              "lessons": [
                {
                  "id": "lesson-1",
                  "title": "Базовые жесты",
                  "description": "Вводный урок",
                  "video_url": "https://example.com/videos/lesson-1.mp4",
                  "order": 0
                }
              ],
              "last_updated": 1700000200
            }
        """.trimIndent()

        val syncData = ApiJsonDecoder.json.decodeFromString<SyncData>(json)

        assertEquals(1, syncData.categories.size)
        assertEquals(1, syncData.signs.size)
        assertEquals(1, syncData.lessons.size)
        assertEquals(1700000200L, syncData.lastUpdated)
        assertEquals("category-1", syncData.signs.first().categoryId)
    }

    @Test
    fun syncMetadata_decodesHasUpdatesSerialName() {
        val json = """
            {
              "last_updated": 1700000200,
              "has_updates": true
            }
        """.trimIndent()

        val metadata = ApiJsonDecoder.json.decodeFromString<SyncMetadata>(json)

        assertEquals(1700000200L, metadata.lastUpdated)
        assertTrue(metadata.hasUpdates)
    }

    @Test
    fun sbertResponse_decodesSuccessDataAndError() {
        val json = """
            {
              "success": false,
              "data": {
                "results": [
                  {
                    "id": "sign-1",
                    "word": "Привет",
                    "similarity": 0.98
                  }
                ]
              },
              "error": {
                "message": "Service unavailable",
                "code": 503
              }
            }
        """.trimIndent()

        val response = ApiJsonDecoder.json.decodeFromString<SBERTSearchResponse>(json)

        assertEquals(false, response.success)
        assertNotNull(response.data)
        assertEquals(1, response.data?.results?.size)
        assertEquals("sign-1", response.data?.results?.first()?.id)
        assertEquals(0.98, response.data?.results?.first()?.similarity ?: 0.0, 0.0)
        assertNotNull(response.error)
        assertEquals("Service unavailable", response.error?.message)
        assertEquals(503, response.error?.code)
    }

    @Test
    fun sbertResponse_allowsNullDataAndError() {
        val json = """
            {
              "success": true,
              "data": null,
              "error": null
            }
        """.trimIndent()

        val response = ApiJsonDecoder.json.decodeFromString<SBERTSearchResponse>(json)

        assertTrue(response.success)
        assertNull(response.data)
        assertNull(response.error)
    }
}
