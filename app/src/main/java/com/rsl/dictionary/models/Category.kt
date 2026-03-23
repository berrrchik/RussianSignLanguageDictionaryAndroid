package com.rsl.dictionary.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String,
    val name: String,
    val order: Int,
    @SerialName("sign_count") val signCount: Int,
    val icon: String? = null,
    val color: String? = null,
    @SerialName("created_at") val createdAt: Long? = null,
    @SerialName("updated_at") val updatedAt: Long? = null
)
