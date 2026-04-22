package com.rsl.dictionary.config

object ApiConfig {
    private const val SERVER_BASE_URL = "http://93.77.177.114:5001"
    private const val API_VERSION = "v1"

    val apiBaseUrl: String = "$SERVER_BASE_URL/api/$API_VERSION"
    val videoBaseUrl: String = "$SERVER_BASE_URL/videos"

    fun videoUrl(relativePath: String): String? {
        if (relativePath.isEmpty()) return null
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return relativePath
        }
        val path = if (relativePath.startsWith("/")) relativePath else "/$relativePath"
        return "$SERVER_BASE_URL/videos$path"
    }

    object Endpoints {
        fun syncCheck(lastUpdated: Long) =
            "$apiBaseUrl/sync/check/raw?last_updated=$lastUpdated"

        val syncData = "$apiBaseUrl/sync/data/raw"
        val sbertSearch = "$apiBaseUrl/search/sbert"
    }
}
