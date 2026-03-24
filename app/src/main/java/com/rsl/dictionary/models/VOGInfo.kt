package com.rsl.dictionary.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SocialNetwork(
    val name: String,
    val url: String
)

@Serializable
data class VOGInfo(
    val name: String,
    val description: String,
    @SerialName("website_url") val websiteURL: String,
    @SerialName("contacts_url") val contactsURL: String,
    val phone: String,
    @SerialName("social_networks") val socialNetworks: List<SocialNetwork>
)
