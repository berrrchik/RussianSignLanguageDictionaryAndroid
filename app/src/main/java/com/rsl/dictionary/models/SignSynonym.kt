package com.rsl.dictionary.models

import kotlinx.serialization.Serializable

@Serializable
data class SignSynonym(
    val id: String,
    val word: String
)
