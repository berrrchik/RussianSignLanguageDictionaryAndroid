package com.rsl.dictionary.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignVideo(
    val id: Int,
    val url: String,
    @SerialName("context_description") val contextDescription: String,
    val order: Int,
    @SerialName("created_at") val createdAt: Long? = null,
    @SerialName("updated_at") val updatedAt: Long? = null
)
