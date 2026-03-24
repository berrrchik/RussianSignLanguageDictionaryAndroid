package com.rsl.dictionary.models

import kotlinx.serialization.Serializable

@Serializable
data class SBERTSearchData(
    val results: List<SBERTSearchResult>
)
