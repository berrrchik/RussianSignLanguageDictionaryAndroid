package com.rsl.dictionary.models

import kotlinx.serialization.Serializable

@Serializable
data class SBERTSearchResponse(
    val success: Boolean,
    val data: SBERTSearchData? = null,
    val error: ApiError? = null
)
