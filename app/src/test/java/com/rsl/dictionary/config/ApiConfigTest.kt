package com.rsl.dictionary.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApiConfigTest {
    @Test
    fun videoUrl_emptyPathReturnsNull() {
        assertNull(ApiConfig.videoUrl(""))
    }

    @Test
    fun videoUrl_addsLeadingSlashWhenNeeded() {
        assertEquals(
            "http://93.77.177.114:5001/videos/path.mp4",
            ApiConfig.videoUrl("path.mp4")
        )
    }

    @Test
    fun videoUrl_doesNotDuplicateLeadingSlash() {
        assertEquals(
            "http://93.77.177.114:5001/videos/path.mp4",
            ApiConfig.videoUrl("/path.mp4")
        )
    }

    @Test
    fun endpoints_syncCheckBuildsExpectedUrl() {
        assertEquals(
            "http://93.77.177.114:5001/api/v1/sync/check/raw?last_updated=123",
            ApiConfig.Endpoints.syncCheck(123)
        )
    }

    @Test
    fun endpoints_syncDataAndSbertSearchUseExpectedPaths() {
        assertEquals(
            "http://93.77.177.114:5001/api/v1/sync/data/raw",
            ApiConfig.Endpoints.syncData
        )
        assertEquals(
            "http://93.77.177.114:5001/api/v1/search/sbert",
            ApiConfig.Endpoints.sbertSearch
        )
    }
}
