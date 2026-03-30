package com.rsl.dictionary.services.search

import com.rsl.dictionary.models.Sign
import com.rsl.dictionary.utilities.data.SignTextSearchHelper
import com.rsl.dictionary.services.network.NetworkMonitor
import javax.inject.Inject

class HybridSearchService @Inject constructor(
    private val sbertSearchService: SBERTSearchService,
    private val networkMonitor: NetworkMonitor
) {
    suspend fun performHybridSearch(
        query: String,
        allSigns: List<Sign>,
        limit: Int = 20
    ): List<Sign> {
        val normalizedQuery = query.lowercase()
        val results = mutableListOf<Sign>()

        results += allSigns.filter { it.word.lowercase() == normalizedQuery }

        if (results.size < limit && networkMonitor.isConnected()) {
            val signsById = allSigns.associateBy { it.id }
            val sbertResults = runCatching {
                sbertSearchService.search(query, limit)
            }.getOrDefault(emptyList())

            results += sbertResults.mapNotNull { result ->
                signsById[result.id]
            }
        }

        if (results.size < limit) {
            val textResults = SignTextSearchHelper.sortByRelevance(
                SignTextSearchHelper.filterSigns(allSigns, query),
                query
            )
            results += textResults
        }

        return results
            .distinctBy { it.id }
            .take(limit)
    }
}
