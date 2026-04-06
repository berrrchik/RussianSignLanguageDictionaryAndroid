package com.rsl.dictionary.services.analytics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.rsl.dictionary.errors.SyncError
import com.rsl.dictionary.errors.VideoRepositoryError
import javax.inject.Inject
import timber.log.Timber

class CrashlyticsErrorReporter @Inject constructor() {
    fun capture(
        error: Throwable,
        context: Map<String, String> = emptyMap(),
        subsystem: String = ""
    ) {
        if (error is SyncError.NoInternet ||
            error is SyncError.ServerUnavailable ||
            error is VideoRepositoryError.UrlNotFound
        ) {
            Timber.d("Expected error filtered from Crashlytics: $error")
            return
        }

        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCustomKey("subsystem", subsystem)
        context.forEach { (key, value) -> crashlytics.setCustomKey(key, value) }
        crashlytics.recordException(error)
    }
}
