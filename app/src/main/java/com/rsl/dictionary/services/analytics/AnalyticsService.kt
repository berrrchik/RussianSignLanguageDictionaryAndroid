package com.rsl.dictionary.services.analytics

import android.content.Context
import android.os.Build
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.rsl.dictionary.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import java.util.Locale

class AnalyticsService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun Bundle.addCommonParams() {
        putString("app_version", BuildConfig.VERSION_NAME)
        putString("android_version", Build.VERSION.RELEASE)
        putString("locale_id", Locale.getDefault().toLanguageTag())
    }

    fun logScreenView(screenName: String, screenClass: String) {
        val bundle = Bundle().apply {
            putString("screen_name", screenName)
            putString("screen_class", screenClass)
            addCommonParams()
        }
        FirebaseAnalytics.getInstance(context).logEvent("screen_view", bundle)
    }

    fun logSignViewed(signId: String, word: String, categoryId: String) {
        val bundle = Bundle().apply {
            putString("sign_id", signId)
            putString("word", word)
            putString("category_id", categoryId)
            addCommonParams()
        }
        FirebaseAnalytics.getInstance(context).logEvent("sign_viewed", bundle)
    }

    fun logSignFavorited(signId: String, word: String) {
        val bundle = Bundle().apply {
            putString("sign_id", signId)
            putString("word", word)
            addCommonParams()
        }
        FirebaseAnalytics.getInstance(context).logEvent("sign_favorited", bundle)
    }

    fun logSignUnfavorited(signId: String, word: String) {
        val bundle = Bundle().apply {
            putString("sign_id", signId)
            putString("word", word)
            addCommonParams()
        }
        FirebaseAnalytics.getInstance(context).logEvent("sign_unfavorited", bundle)
    }

    fun logSearchPerformed(query: String, resultsCount: Int, searchType: String) {
        val bundle = Bundle().apply {
            putString("query", query)
            putInt("results_count", resultsCount)
            putString("search_type", searchType)
            addCommonParams()
        }
        FirebaseAnalytics.getInstance(context).logEvent("search_performed", bundle)
    }

    fun logCategoryOpened(categoryId: String, categoryName: String) {
        val bundle = Bundle().apply {
            putString("category_id", categoryId)
            putString("category_name", categoryName)
            addCommonParams()
        }
        FirebaseAnalytics.getInstance(context).logEvent("category_opened", bundle)
    }

    fun logLessonViewed(lessonId: String, title: String) {
        val bundle = Bundle().apply {
            putString("lesson_id", lessonId)
            putString("title", title)
            addCommonParams()
        }
        FirebaseAnalytics.getInstance(context).logEvent("lesson_viewed", bundle)
    }

    fun logSyncCompleted() {
        val bundle = Bundle().apply {
            addCommonParams()
        }
        FirebaseAnalytics.getInstance(context).logEvent("sync_completed", bundle)
    }

    fun logSyncFailed(reason: String) {
        val bundle = Bundle().apply {
            putString("reason", reason)
            addCommonParams()
        }
        FirebaseAnalytics.getInstance(context).logEvent("sync_failed", bundle)
    }
}
