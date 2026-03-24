package com.rsl.dictionary.models

import kotlinx.serialization.Serializable

@Serializable
data class SignsData(
    val signs: List<Sign>,
    val categories: List<Category>
)
