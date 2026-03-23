package com.rsl.dictionary.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Sign(
    val id: String,
    val word: String,
    val description: String,
    @SerialName("category_id") val categoryId: String,
    val videos: List<SignVideo>? = null,
    val synonyms: List<SignSynonym>? = null
) {
    val firstVideo: SignVideo? get() = videos?.firstOrNull()
    val primaryVideoURL: String? get() = videos?.firstOrNull()?.url
    val videosArray: List<SignVideo> get() = videos.orEmpty()
}
