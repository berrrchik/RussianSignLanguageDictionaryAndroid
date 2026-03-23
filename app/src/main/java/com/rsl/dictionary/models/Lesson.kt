package com.rsl.dictionary.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Lesson(
    val id: String,
    val title: String,
    val description: String,
    @SerialName("video_url") val videoUrl: String,
    val order: Int,
    @SerialName("created_at") val createdAt: Long? = null,
    @SerialName("updated_at") val updatedAt: Long? = null
)
