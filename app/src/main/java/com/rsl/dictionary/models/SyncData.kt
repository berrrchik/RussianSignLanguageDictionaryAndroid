package com.rsl.dictionary.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SyncData(
    val categories: List<Category>,
    val signs: List<Sign>,
    val lessons: List<Lesson>,
    @SerialName("last_updated") val lastUpdated: Long
)
