package com.rsl.dictionary

import android.app.Application
import com.rsl.dictionary.di.DIStartupValidation
import com.rsl.dictionary.services.analytics.AnalyticsConsentService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class RslDictionaryApp : Application() {
    @Inject
    lateinit var analyticsConsentService: AnalyticsConsentService

    @Inject
    lateinit var diStartupValidation: DIStartupValidation

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        analyticsConsentService.applyConsent(this)
        diStartupValidation.validate()
    }
}
