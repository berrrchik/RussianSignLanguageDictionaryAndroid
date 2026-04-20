package com.rsl.dictionary.utilities.data

import com.rsl.dictionary.models.Sign
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignTextSearchHelperTest {
    @Test
    fun filterSigns_searchesWordCaseInsensitively() {
        val signs = listOf(
            sign(word = "Привет"),
            sign(word = "Пока")
        )

        val results = SignTextSearchHelper.filterSigns(signs, "пРи")

        assertEquals(listOf("Привет"), results.map { it.word })
    }

    @Test
    fun filterSigns_searchesDescription_whenEnabled() {
        val signs = listOf(
            sign(word = "Жест", description = "Описание приветствия"),
            sign(word = "Пока", description = "Прощание")
        )

        val results = SignTextSearchHelper.filterSigns(
            signs = signs,
            query = "привет",
            includeDescription = true
        )

        assertEquals(listOf("Жест"), results.map { it.word })
    }

    @Test
    fun sortByRelevance_prioritizesExactMatch() {
        val signs = listOf(
            sign(word = "Приветствие"),
            sign(word = "Привет"),
            sign(word = "Супривет")
        )

        val sorted = SignTextSearchHelper.sortByRelevance(signs, "привет")

        assertEquals("Привет", sorted.first().word)
    }

    @Test
    fun sortByRelevance_prioritizesPrefixOverInfix() {
        val signs = listOf(
            sign(word = "Супривет"),
            sign(word = "Приветствие")
        )

        val sorted = SignTextSearchHelper.sortByRelevance(signs, "привет")

        assertEquals(listOf("Приветствие", "Супривет"), sorted.map { it.word })
    }

    @Test
    fun emptyQuery_returnsAllResults_andSortsAlphabeticallyInRelevanceOrder() {
        val signs = listOf(
            sign(word = "Яблоко"),
            sign(word = "Арбуз"),
            sign(word = "Борщ")
        )

        val filtered = SignTextSearchHelper.filterSigns(signs, "")
        val sorted = SignTextSearchHelper.sortByRelevance(filtered, "")

        assertEquals(3, filtered.size)
        assertEquals(listOf("Арбуз", "Борщ", "Яблоко"), sorted.map { it.word })
        assertTrue(sorted.isNotEmpty())
    }

    private fun sign(word: String, description: String = ""): Sign = Sign(
        id = word,
        word = word,
        description = description,
        categoryId = "category-1"
    )
}
