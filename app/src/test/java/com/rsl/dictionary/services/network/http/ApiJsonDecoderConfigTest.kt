package com.rsl.dictionary.services.network.http

import com.rsl.dictionary.models.Category
import com.rsl.dictionary.models.Sign
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiJsonDecoderConfigTest {
    @Test
    fun ignoreUnknownKeys_allowsCurrentModelsToDecodeUnexpectedFields() {
        val json = """
            {
              "id": "category-1",
              "name": "Быт",
              "order": 0,
              "sign_count": 3,
              "unknown_object": {
                "nested": true
              }
            }
        """.trimIndent()

        val category = ApiJsonDecoder.json.decodeFromString<Category>(json)

        assertEquals("category-1", category.id)
        assertEquals(3, category.signCount)
    }

    @Test
    fun coerceInputValues_currentOptionalModelFieldsDecodeNullsWithoutFailure() {
        val json = """
            {
              "id": "sign-1",
              "word": "Привет",
              "description": "Жест приветствия",
              "category_id": "category-1",
              "videos": null,
              "synonyms": null
            }
        """.trimIndent()

        val sign = ApiJsonDecoder.json.decodeFromString<Sign>(json)

        assertNull(sign.videos)
        assertNull(sign.synonyms)
        assertTrue(sign.videosArray.isEmpty())
    }

    @Test
    fun incompatibleRequiredFieldType_throwsSerializationException() {
        val json = """
            {
              "id": "category-1",
              "name": "Быт",
              "order": "invalid",
              "sign_count": 3
            }
        """.trimIndent()

        assertThrows(SerializationException::class.java) {
            ApiJsonDecoder.json.decodeFromString<Category>(json)
        }
    }
}
