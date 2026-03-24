package com.rsl.dictionary.models

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    val message: String,
    val code: Int? = null
)
