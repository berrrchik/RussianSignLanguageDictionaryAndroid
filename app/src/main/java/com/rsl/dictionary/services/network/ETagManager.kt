package com.rsl.dictionary.services.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ETagManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val sharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getETag(key: String): String? = sharedPreferences.getString(key, null)

    fun saveETag(key: String, rawETag: String) {
        val normalizedETag = rawETag
            .trim()
            .replace("\"", "")
            .substringBefore(":")

        if (normalizedETag.length != E_TAG_LENGTH) return

        sharedPreferences.edit().putString(key, normalizedETag).apply()
    }

    fun clearETag(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "rsl_network_preferences"
        const val E_TAG_LENGTH = 32
    }
}
