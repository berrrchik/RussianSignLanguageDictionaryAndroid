package com.rsl.dictionary.viewmodels

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.rsl.dictionary.models.FavoriteEntry
import com.rsl.dictionary.models.Sign
import com.rsl.dictionary.models.SignVideo
import com.rsl.dictionary.repositories.protocols.FavoriteOfflinePreparationResult
import com.rsl.dictionary.repositories.protocols.VideoRepository
import com.rsl.dictionary.services.cache.VideoCacheDirectoryManager
import com.rsl.dictionary.testing.rules.MainDispatcherRule
import java.io.File
import java.io.RandomAccessFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        VideoCacheDirectoryManager.shortTermDir(context).deleteRecursively()
        VideoCacheDirectoryManager.favoritesDir(context).deleteRecursively()
    }

    @Test
    fun init_calculatesSeparateShortTermAndFavoritesSizes() = runTest {
        createSizedFile(VideoCacheDirectoryManager.shortTermDir(context), "short.bin", 1_048_576)
        createSizedFile(VideoCacheDirectoryManager.favoritesDir(context), "favorite.bin", 524_288)

        val viewModel = SettingsViewModel(context, NoOpVideoRepository())
        advanceUntilIdle()

        assertEquals("1.0 MB", viewModel.shortTermCacheSize.value)
        assertEquals("0.5 MB", viewModel.favoritesOfflineSize.value)
    }

    @Test
    fun shortTermCacheSize_isFormattedAsHumanReadableMegabytes() = runTest {
        createSizedFile(VideoCacheDirectoryManager.shortTermDir(context), "only.bin", 262_144)

        val viewModel = SettingsViewModel(context, NoOpVideoRepository())
        advanceUntilIdle()

        assertEquals("0.3 MB", viewModel.shortTermCacheSize.value)
        assertEquals("0.0 MB", viewModel.favoritesOfflineSize.value)
    }

    @Test
    fun clearCache_clearsOnlyShortTermDirectory() = runTest {
        val shortTermDir = VideoCacheDirectoryManager.shortTermDir(context)
        val favoritesDir = VideoCacheDirectoryManager.favoritesDir(context)
        createSizedFile(shortTermDir, "short.bin", 400_000)
        createSizedFile(favoritesDir, "favorite.bin", 600_000)

        val viewModel = SettingsViewModel(
            context,
            ClearingVideoRepository {
                shortTermDir.deleteRecursively()
                shortTermDir.mkdirs()
            }
        )
        advanceUntilIdle()

        viewModel.clearCache()
        runCurrent()

        assertEquals("0.0 MB", viewModel.shortTermCacheSize.value)
        assertEquals("0.6 MB", viewModel.favoritesOfflineSize.value)
        assertEquals(0, shortTermDir.listFiles().orEmpty().size)
        assertEquals(1, favoritesDir.listFiles().orEmpty().size)
    }

    @Test
    fun clearCache_emitsSuccessFlag() = runTest {
        val viewModel = SettingsViewModel(
            context,
            ClearingVideoRepository {
                VideoCacheDirectoryManager.shortTermDir(context).deleteRecursively()
                VideoCacheDirectoryManager.favoritesDir(context).deleteRecursively()
                VideoCacheDirectoryManager.shortTermDir(context).mkdirs()
                VideoCacheDirectoryManager.favoritesDir(context).mkdirs()
            }
        )
        advanceUntilIdle()

        viewModel.clearCache()
        runCurrent()

        assertTrue(viewModel.showCacheCleared.value)
        assertFalse(viewModel.isCacheClearing.value)

        advanceTimeBy(2_000)
        runCurrent()

        assertFalse(viewModel.showCacheCleared.value)
    }

    private fun createSizedFile(directory: File, name: String, sizeBytes: Long): File {
        directory.mkdirs()
        return File(directory, name).also { file ->
            RandomAccessFile(file, "rw").use { raf ->
                raf.setLength(sizeBytes)
            }
        }
    }

    private class NoOpVideoRepository : VideoRepository {
        override suspend fun getVideoURL(video: SignVideo, useFavoritesCache: Boolean): Uri {
            return Uri.parse(video.url)
        }

        override suspend fun prepareFavoriteOffline(sign: Sign): FavoriteOfflinePreparationResult {
            return FavoriteOfflinePreparationResult.Ready(emptyList())
        }

        override suspend fun clearCache() = Unit

        override suspend fun removeFavoriteOffline(entry: FavoriteEntry) = Unit
    }

    private class ClearingVideoRepository(
        private val clearAction: suspend () -> Unit
    ) : VideoRepository {
        override suspend fun getVideoURL(video: SignVideo, useFavoritesCache: Boolean): Uri {
            return Uri.parse(video.url)
        }

        override suspend fun prepareFavoriteOffline(sign: Sign): FavoriteOfflinePreparationResult {
            return FavoriteOfflinePreparationResult.Ready(emptyList())
        }

        override suspend fun clearCache() {
            clearAction()
        }

        override suspend fun removeFavoriteOffline(entry: FavoriteEntry) = Unit
    }
}
