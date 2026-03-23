package com.rsl.dictionary.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SyncMetadata(
    @SerialName("last_updated") val lastUpdated: Long,
    @SerialName("has_updates") val hasUpdates: Boolean
)
