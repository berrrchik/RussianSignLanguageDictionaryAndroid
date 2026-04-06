package com.rsl.dictionary.services.analytics

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import javax.inject.Inject

class PerformanceService @Inject constructor() {
    fun startTrace(name: String): Trace {
        val trace = FirebasePerformance.getInstance().newTrace(name)
        trace.start()
        return trace
    }

    fun stopTrace(trace: Trace) {
        trace.stop()
    }

    companion object {
        const val SIGNS_DATA_LOAD = "signs_data_load"
        const val SCREEN_SEARCH_LOAD = "screen_search_load"
        const val SCREEN_SIGN_DETAIL_LOAD = "screen_sign_detail_load"
        const val VIDEO_LOAD_FAVORITES_CACHE = "video_load_favorites_cache"
        const val VIDEO_DOWNLOAD_NETWORK = "video_download_network"
        const val HYBRID_SEARCH = "hybrid_search"
        const val SEARCH_EXACT_MATCH = "search_exact_match"
        const val SEARCH_SBERT = "search_sbert"
        const val SEARCH_TEXT = "search_text"
    }
}
