package com.rsl.dictionary.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignMetadata(
    val duration: Double? = null,
    @SerialName("file_size") val fileSize: Int? = null,
    val resolution: String? = null,
    val format: String? = null,
    val fps: Int? = null
)
