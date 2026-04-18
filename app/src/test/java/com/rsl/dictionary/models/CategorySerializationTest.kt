package com.rsl.dictionary.models

import com.rsl.dictionary.services.network.http.ApiJsonDecoder
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategorySerializationTest {
    @Test
    fun decode_mapsSerialNamesAndLongTimestamps() {
        val json = """
            {
              "id": "category-1",
              "name": "Быт",
              "order": 5,
              "sign_count": 12,
              "icon": "house",
              "color": "#FF8A00",
              "created_at": 1700000000,
              "updated_at": 1700000100
            }
        """.trimIndent()

        val category = ApiJsonDecoder.json.decodeFromString<Category>(json)

        assertEquals(12, category.signCount)
        assertEquals(Long::class.javaObjectType, category.createdAt?.javaClass)
        assertEquals(Long::class.javaObjectType, category.updatedAt?.javaClass)
    }

    @Test
    fun decode_handlesNullOptionalFields_andIgnoresUnknownFields() {
        val json = """
            {
              "id": "category-1",
              "name": "Быт",
              "order": 5,
              "sign_count": 12,
              "icon": null,
              "color": null,
              "unknown": "ignored"
            }
        """.trimIndent()

        val category = ApiJsonDecoder.json.decodeFromString<Category>(json)

        assertNull(category.icon)
        assertNull(category.color)
        assertNull(category.createdAt)
        assertNull(category.updatedAt)
    }
}
