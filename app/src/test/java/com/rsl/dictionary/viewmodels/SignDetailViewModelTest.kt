package com.rsl.dictionary.viewmodels

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.rsl.dictionary.errors.SignRepositoryError
import com.rsl.dictionary.errors.VideoRepositoryError
import com.rsl.dictionary.models.FavoriteOfflineStatus
import com.rsl.dictionary.models.FavoriteOfflineVideo
import com.rsl.dictionary.repositories.protocols.SignRepository
import com.rsl.dictionary.repositories.protocols.FavoriteOfflinePreparationResult
import com.rsl.dictionary.services.analytics.AnalyticsService
import com.rsl.dictionary.testing.factories.TestDataFactory
import com.rsl.dictionary.testing.fakes.FakeFavoritesRepository
import com.rsl.dictionary.testing.fakes.FakeVideoRepository
import com.rsl.dictionary.testing.rules.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SignDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sign = TestDataFactory.sign(id = "sign-1", word = "Привет", categoryId = "category-1")
    private val synonym = TestDataFactory.sign(id = "sign-2", word = "Здравствуйте", categoryId = "category-1")
    private val analyticsService = mockk<AnalyticsService>(relaxed = true)

    @Test
    fun init_loadsSignFromSavedStateAndTracksVisitedIds() = runTest {
        val signRepository = mockk<SignRepository> {
            coEvery { getSign("sign-1") } returns sign
        }
        val encodedVisited = Uri.encode(Json.encodeToString(listOf("sign-9", "sign-8")))

        val viewModel = SignDetailViewModel(
            signRepository = signRepository,
            favoritesRepository = FakeFavoritesRepository(),
            videoRepository = FakeVideoRepository(),
            analyticsService = analyticsService,
            savedStateHandle = SavedStateHandle(
                mapOf(
                    "signId" to "sign-1",
                    "visitedSignIds" to encodedVisited
                )
            )
        )
        advanceUntilIdle()

        assertEquals("sign-1", viewModel.sign.value?.id)
        assertEquals(setOf("sign-9", "sign-8", "sign-1"), viewModel.visitedSignIds.value)
    }

    @Test
    fun invalidVisitedIdsPayload_fallsBackToEmptySet() = runTest {
        val viewModel = SignDetailViewModel(
            signRepository = mockk(relaxed = true),
            favoritesRepository = FakeFavoritesRepository(),
            videoRepository = FakeVideoRepository(),
            analyticsService = analyticsService,
            savedStateHandle = SavedStateHandle(mapOf("visitedSignIds" to "%E0%A4%A"))
        )
        advanceUntilIdle()

        assertEquals(emptySet<String>(), viewModel.visitedSignIds.value)
    }

    @Test
    fun favoriteEntriesFlow_updatesIsFavoriteReactively() = runTest {
        val favoritesRepository = FakeFavoritesRepository()
        val signRepository = mockk<SignRepository> {
            coEvery { getSign("sign-1") } returns sign
        }
        val viewModel = SignDetailViewModel(
            signRepository = signRepository,
            favoritesRepository = favoritesRepository,
            videoRepository = FakeVideoRepository(),
            analyticsService = analyticsService,
            savedStateHandle = SavedStateHandle(mapOf("signId" to "sign-1"))
        )
        advanceUntilIdle()

        assertFalse(viewModel.isFavorite.value)

        favoritesRepository.markFavoritePending(sign)
        advanceUntilIdle()

        assertTrue(viewModel.isFavorite.value)
        assertEquals(FavoriteOfflineStatus.PENDING, viewModel.favoriteOfflineStatus.value)
    }

    @Test
    fun toggleFavorite_addBranchMarksReadyOnlyAfterOfflinePreparation() = runTest {
        val favoritesRepository = FakeFavoritesRepository()
        val videoRepository = FakeVideoRepository()
        val signRepository = mockk<SignRepository> {
            coEvery { getSign("sign-1") } returns sign
        }
        val viewModel = SignDetailViewModel(
            signRepository = signRepository,
            favoritesRepository = favoritesRepository,
            videoRepository = videoRepository,
            analyticsService = analyticsService,
            savedStateHandle = SavedStateHandle(mapOf("signId" to "sign-1"))
        )
        advanceUntilIdle()

        viewModel.toggleFavorite()
        advanceUntilIdle()

        assertEquals(listOf("sign-1"), favoritesRepository.getAll())
        assertEquals(FavoriteOfflineStatus.READY_OFFLINE, favoritesRepository.getOfflineStatus("sign-1"))
        assertEquals(listOf("sign-1"), favoritesRepository.getCachedSigns().map { it.id })
        assertEquals(listOf("sign-1"), videoRepository.preparedFavoriteSignIds)
    }

    @Test
    fun toggleFavorite_failedPreparationKeepsFavoriteButMarksFailed() = runTest {
        val favoritesRepository = FakeFavoritesRepository()
        val videoRepository = FakeVideoRepository().apply {
            prepareFavoriteOfflineResultFactory = {
                FavoriteOfflinePreparationResult.Failed(
                    downloadedVideos = listOf(FavoriteOfflineVideo(videoId = 1, fileName = "video_1.mp4")),
                    error = VideoRepositoryError.DownloadFailed(IOException("offline failed"))
                )
            }
        }
        val signRepository = mockk<SignRepository> {
            coEvery { getSign("sign-1") } returns sign
        }
        val viewModel = SignDetailViewModel(
            signRepository = signRepository,
            favoritesRepository = favoritesRepository,
            videoRepository = videoRepository,
            analyticsService = analyticsService,
            savedStateHandle = SavedStateHandle(mapOf("signId" to "sign-1"))
        )
        advanceUntilIdle()

        viewModel.toggleFavorite()
        advanceUntilIdle()

        assertTrue(viewModel.isFavorite.value)
        assertEquals(FavoriteOfflineStatus.FAILED, favoritesRepository.getOfflineStatus("sign-1"))
        assertEquals("Видео сейчас недоступно", viewModel.error.value)
    }

    @Test
    fun toggleFavorite_removeBranchClearsFavoritesCacheFromManifest() = runTest {
        val favoritesRepository = FakeFavoritesRepository(listOf("sign-1"))
        val videoRepository = FakeVideoRepository()
        val signRepository = mockk<SignRepository> {
            coEvery { getSign("sign-1") } returns sign
        }
        val viewModel = SignDetailViewModel(
            signRepository = signRepository,
            favoritesRepository = favoritesRepository,
            videoRepository = videoRepository,
            analyticsService = analyticsService,
            savedStateHandle = SavedStateHandle(mapOf("signId" to "sign-1"))
        )
        advanceUntilIdle()

        viewModel.toggleFavorite()
        advanceUntilIdle()

        assertEquals(emptyList<String>(), favoritesRepository.getAll())
        assertEquals(listOf("sign-1"), videoRepository.clearedFavoritesSignIds)
    }

    @Test
    fun loadSynonymSign_callsSuccessCallbackOnSuccess() = runTest {
        val signRepository = mockk<SignRepository> {
            coEvery { getSign("sign-2") } returns synonym
        }
        val viewModel = SignDetailViewModel(
            signRepository = signRepository,
            favoritesRepository = FakeFavoritesRepository(),
            videoRepository = FakeVideoRepository(),
            analyticsService = analyticsService,
            savedStateHandle = SavedStateHandle()
        )
        var success: String? = null
        var failure: String? = null

        viewModel.loadSynonymSign(
            signId = "sign-2",
            onSuccess = { success = it.id },
            onFailure = { failure = it }
        )
        advanceUntilIdle()

        assertEquals("sign-2", success)
        assertNull(failure)
    }

    @Test
    fun loadSynonymSign_callsFailureCallbackOnError() = runTest {
        val signRepository = mockk<SignRepository> {
            coEvery { getSign("sign-2") } throws SignRepositoryError.NotFound
        }
        val viewModel = SignDetailViewModel(
            signRepository = signRepository,
            favoritesRepository = FakeFavoritesRepository(),
            videoRepository = FakeVideoRepository(),
            analyticsService = analyticsService,
            savedStateHandle = SavedStateHandle()
        )
        var success: String? = null
        var failure: String? = null

        viewModel.loadSynonymSign(
            signId = "sign-2",
            onSuccess = { success = it.id },
            onFailure = { failure = it }
        )
        advanceUntilIdle()

        assertNull(success)
        assertEquals("Жест не найден", failure)
    }

    @Test
    fun loadFailure_mapsErrorState() = runTest {
        val signRepository = mockk<SignRepository> {
            coEvery { getSign("sign-1") } throws SignRepositoryError.DecodingError(IOException("bad"))
        }
        val viewModel = SignDetailViewModel(
            signRepository = signRepository,
            favoritesRepository = FakeFavoritesRepository(),
            videoRepository = FakeVideoRepository(),
            analyticsService = analyticsService,
            savedStateHandle = SavedStateHandle(mapOf("signId" to "sign-1"))
        )
        advanceUntilIdle()

        assertEquals("Ошибка обработки данных", viewModel.error.value)
    }
}
