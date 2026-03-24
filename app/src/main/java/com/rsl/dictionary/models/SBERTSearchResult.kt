package com.rsl.dictionary.models

import kotlinx.serialization.Serializable

@Serializable
data class SBERTSearchResult(
    val id: String,
    val word: String,
    val similarity: Double
)
