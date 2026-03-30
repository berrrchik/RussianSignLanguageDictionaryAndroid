package com.rsl.dictionary.utilities.data

import com.rsl.dictionary.models.Sign

object SignTextSearchHelper {
    fun filterSigns(
        signs: List<Sign>,
        query: String,
        includeDescription: Boolean = false
    ): List<Sign> {
        val normalizedQuery = query.lowercase()
        return signs.filter { sign ->
            sign.word.lowercase().contains(normalizedQuery) ||
                (includeDescription && sign.description.lowercase().contains(normalizedQuery))
        }
    }

    fun sortByRelevance(signs: List<Sign>, query: String): List<Sign> {
        val normalizedQuery = query.lowercase()
        return signs.sortedWith(
            compareBy<Sign>(
                { if (it.word.lowercase() == normalizedQuery) 0 else 1 },
                { if (it.word.lowercase().startsWith(normalizedQuery)) 0 else 1 },
                {
                    val index = it.word.lowercase().indexOf(normalizedQuery)
                    if (index >= 0) index else Int.MAX_VALUE
                },
                { it.word.lowercase() }
            )
        )
    }
}
