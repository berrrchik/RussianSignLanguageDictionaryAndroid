package com.rsl.dictionary.services.analytics

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject

class AnalyticsConsentService @Inject constructor() {
    fun applyConsent(context: Context) {
        FirebaseAnalytics.getInstance(context).setConsent(
            mapOf(
                FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to
                    FirebaseAnalytics.ConsentStatus.GRANTED,
                FirebaseAnalytics.ConsentType.AD_STORAGE to
                    FirebaseAnalytics.ConsentStatus.DENIED
            )
        )
    }
}
