package com.rsl.dictionary.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthorInfo(
    val name: String,
    val email: String,
    val github: String
)

@Serializable
data class AppInfo(
    val name: String,
    val version: String,
    @SerialName("build_number") val buildNumber: String,
    val description: String,
    val author: AuthorInfo
)
