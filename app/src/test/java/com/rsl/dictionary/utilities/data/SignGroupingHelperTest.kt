package com.rsl.dictionary.utilities.data

import com.rsl.dictionary.models.Sign
import org.junit.Assert.assertEquals
import org.junit.Test

class SignGroupingHelperTest {
    @Test
    fun sortSignsAlphabetically_usesRussianAlphabetOrder() {
        val signs = listOf(
            sign("Яблоко"),
            sign("Ёж"),
            sign("Арбуз"),
            sign("Борщ")
        )

        val sorted = SignGroupingHelper.sortSignsAlphabetically(signs, SortOrder.ASCENDING)

        assertEquals(listOf("Арбуз", "Борщ", "Ёж", "Яблоко"), sorted.map { it.word })
    }

    @Test
    fun grouping_trimsLeadingSpacesForSectionKey() {
        val grouped = SignGroupingHelper.groupByFirstLetter(
            signs = listOf(sign("   арбуз")),
            sortOrder = SortOrder.ASCENDING
        )

        assertEquals(listOf("А"), grouped.keys.toList())
        assertEquals("   арбуз", grouped.getValue("А").single().word)
    }

    @Test
    fun grouping_movesNonLetterItemsToHashSection() {
        val grouped = SignGroupingHelper.groupByFirstLetter(
            signs = listOf(sign("1 жест"), sign("Арбуз")),
            sortOrder = SortOrder.ASCENDING
        )

        assertEquals(listOf("А", "#"), grouped.keys.toList())
        assertEquals("1 жест", grouped.getValue("#").single().word)
    }

    @Test
    fun descendingOrder_reversesSectionsAndItems_whenInputIsSortedDescending() {
        val sortedDescending = SignGroupingHelper.sortSignsAlphabetically(
            signs = listOf(sign("Аист"), sign("Арбуз"), sign("Борщ")),
            sortOrder = SortOrder.DESCENDING
        )

        val grouped = SignGroupingHelper.groupByFirstLetter(sortedDescending, SortOrder.DESCENDING)

        assertEquals(listOf("Б", "А"), grouped.keys.toList())
        assertEquals(listOf("Борщ"), grouped.getValue("Б").map { it.word })
        assertEquals(listOf("Арбуз", "Аист"), grouped.getValue("А").map { it.word })
    }

    private fun sign(word: String): Sign = Sign(
        id = word,
        word = word,
        description = "",
        categoryId = "category-1"
    )
}
