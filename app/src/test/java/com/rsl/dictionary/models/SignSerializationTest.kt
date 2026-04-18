package com.rsl.dictionary.models

import com.rsl.dictionary.services.network.http.ApiJsonDecoder
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SignSerializationTest {
    @Test
    fun decode_mapsCategoryIdVideosSynonyms_andIgnoresUnknownFields() {
        val json = """
            {
              "id": "sign-1",
              "word": "Привет",
              "description": "Жест приветствия",
              "category_id": "category-42",
              "videos": [
                {
                  "id": 101,
                  "url": "https://example.com/videos/hello.mp4",
                  "context_description": "Основной вариант",
                  "order": 0,
                  "created_at": 1700000000,
                  "updated_at": 1700000100
                },
                {
                  "id": 102,
                  "url": "https://example.com/videos/hello-alt.mp4",
                  "context_description": "Альтернативный вариант",
                  "order": 1
                }
              ],
              "synonyms": [
                {
                  "id": "sign-2",
                  "word": "Здравствуйте"
                }
              ],
              "unexpected_field": "ignored"
            }
        """.trimIndent()

        val sign = ApiJsonDecoder.json.decodeFromString<Sign>(json)

        assertEquals("category-42", sign.categoryId)
        assertNotNull(sign.videos)
        assertEquals(2, sign.videos?.size)
        assertEquals(101, sign.videos?.first()?.id)
        assertEquals("https://example.com/videos/hello.mp4", sign.videos?.first()?.url)
        assertEquals("Основной вариант", sign.videos?.first()?.contextDescription)
        assertEquals(Long::class.javaObjectType, sign.videos?.first()?.createdAt?.javaClass)
        assertEquals(Long::class.javaObjectType, sign.videos?.first()?.updatedAt?.javaClass)
        assertNotNull(sign.synonyms)
        assertEquals(1, sign.synonyms?.size)
        assertEquals("sign-2", sign.synonyms?.first()?.id)
        assertEquals("Здравствуйте", sign.synonyms?.first()?.word)
    }

    @Test
    fun computedProperties_returnNullAndEmptyList_whenVideosAreNull() {
        val json = """
            {
              "id": "sign-1",
              "word": "Привет",
              "description": "Жест приветствия",
              "category_id": "category-1",
              "videos": null
            }
        """.trimIndent()

        val sign = ApiJsonDecoder.json.decodeFromString<Sign>(json)

        assertNull(sign.videos)
        assertTrue(sign.videosArray.isEmpty())
        assertNull(sign.firstVideo)
        assertNull(sign.primaryVideoURL)
    }

    @Test
    fun computedProperties_returnNullAndEmptyList_whenVideosAreEmpty() {
        val json = """
            {
              "id": "sign-1",
              "word": "Привет",
              "description": "Жест приветствия",
              "category_id": "category-1",
              "videos": []
            }
        """.trimIndent()

        val sign = ApiJsonDecoder.json.decodeFromString<Sign>(json)

        assertEquals(emptyList<SignVideo>(), sign.videos)
        assertTrue(sign.videosArray.isEmpty())
        assertNull(sign.firstVideo)
        assertNull(sign.primaryVideoURL)
    }

    @Test
    fun computedProperties_returnFirstVideoAndItsUrl_whenVideosArePresent() {
        val json = """
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
                },
                {
                  "id": 102,
                  "url": "https://example.com/videos/hello-alt.mp4",
                  "context_description": "Альтернативный вариант",
                  "order": 1
                }
              ]
            }
        """.trimIndent()

        val sign = ApiJsonDecoder.json.decodeFromString<Sign>(json)

        assertEquals(101, sign.firstVideo?.id)
        assertEquals("https://example.com/videos/hello.mp4", sign.primaryVideoURL)
        assertEquals(2, sign.videosArray.size)
    }
}
