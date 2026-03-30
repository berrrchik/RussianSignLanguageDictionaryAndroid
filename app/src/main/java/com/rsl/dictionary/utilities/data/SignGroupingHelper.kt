package com.rsl.dictionary.utilities.data

import com.rsl.dictionary.models.Sign
import java.util.Locale

enum class SortOrder {
    ASCENDING,
    DESCENDING
}

object SignGroupingHelper {
    private const val RussianAlphabet = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
    private val russianAlphabetIndex = RussianAlphabet.withIndex().associate { (index, char) ->
        char to index
    }
    private val russianLocale = Locale("ru", "RU")

    fun sortSignsAlphabetically(
        signs: List<Sign>,
        sortOrder: SortOrder
    ): List<Sign> {
        val sorted = signs.sortedWith { first, second ->
            compareWords(first.word, second.word)
        }
        return if (sortOrder == SortOrder.DESCENDING) sorted.reversed() else sorted
    }

    fun groupByFirstLetter(
        signs: List<Sign>,
        sortOrder: SortOrder
    ): LinkedHashMap<String, List<Sign>> {
        val groupedSigns = signs.groupBy { sign ->
            val firstChar = sign.word.trimStart().firstOrNull()?.uppercaseChar()
            if (firstChar != null && firstChar.isLetter()) {
                firstChar.toString()
            } else {
                "#"
            }
        }

        val sortedKeys = groupedSigns.keys
            .filter { it != "#" }
            .sortedWith(::compareSectionKeys)
            .let { keys ->
                if (sortOrder == SortOrder.DESCENDING) keys.reversed() else keys
            } + listOfNotNull(groupedSigns.keys.find { it == "#" })

        return LinkedHashMap<String, List<Sign>>().apply {
            sortedKeys.forEach { key ->
                put(key, groupedSigns.getValue(key))
            }
        }
    }

    private fun compareSectionKeys(first: String, second: String): Int {
        val firstChar = first.firstOrNull()?.uppercaseChar()
        val secondChar = second.firstOrNull()?.uppercaseChar()
        return compareCharacters(firstChar, secondChar)
    }

    private fun compareWords(first: String, second: String): Int {
        val normalizedFirst = first.trimStart().uppercase(russianLocale)
        val normalizedSecond = second.trimStart().uppercase(russianLocale)

        val minLength = minOf(normalizedFirst.length, normalizedSecond.length)
        for (index in 0 until minLength) {
            val result = compareCharacters(normalizedFirst[index], normalizedSecond[index])
            if (result != 0) return result
        }

        return normalizedFirst.length.compareTo(normalizedSecond.length)
    }

    private fun compareCharacters(first: Char?, second: Char?): Int {
        if (first == second) return 0
        if (first == null) return 1
        if (second == null) return -1

        val firstIndex = russianAlphabetIndex[first]
        val secondIndex = russianAlphabetIndex[second]

        return when {
            firstIndex != null && secondIndex != null -> firstIndex.compareTo(secondIndex)
            firstIndex != null -> -1
            secondIndex != null -> 1
            else -> first.compareTo(second)
        }
    }
}
