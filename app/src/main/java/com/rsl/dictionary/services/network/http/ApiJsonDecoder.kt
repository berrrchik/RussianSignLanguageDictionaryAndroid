package com.rsl.dictionary.services.network.http

import kotlinx.serialization.json.Json

object ApiJsonDecoder {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }
}
