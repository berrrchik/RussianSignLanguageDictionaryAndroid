package com.rsl.dictionary.services.cache

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VideoCacheDirectoryManagerTest {
    @Test
    fun shortTermDir_createsDirectory() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val directory = VideoCacheDirectoryManager.shortTermDir(context)

        assertTrue(directory.exists())
        assertTrue(directory.isDirectory)
    }

    @Test
    fun favoritesDir_createsDirectory() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val directory = VideoCacheDirectoryManager.favoritesDir(context)

        assertTrue(directory.exists())
        assertTrue(directory.isDirectory)
    }

    @Test
    fun videoId_isDeterministic() {
        val first = VideoCacheDirectoryManager.videoId("https://example.com/video.mp4")
        val second = VideoCacheDirectoryManager.videoId("https://example.com/video.mp4")
        val third = VideoCacheDirectoryManager.videoId("https://example.com/other.mp4")

        assertEquals(first, second)
        assertNotEquals(first, third)
    }

    @Test
    fun deleteFile_doesNotFailWhenFileIsMissing() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "missing-video-file.mp4")

        VideoCacheDirectoryManager.deleteFile(file)

        assertTrue(!file.exists())
    }
}
