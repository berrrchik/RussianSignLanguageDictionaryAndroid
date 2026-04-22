package com.rsl.dictionary.viewmodels

import androidx.lifecycle.SavedStateHandle
import com.rsl.dictionary.errors.SignRepositoryError
import com.rsl.dictionary.models.RepositoryDataStatus
import com.rsl.dictionary.repositories.protocols.SignRepository
import com.rsl.dictionary.repositories.protocols.SignRepositoryRefreshState
import com.rsl.dictionary.testing.factories.TestDataFactory
import com.rsl.dictionary.testing.fakes.FakeSignRepository
import com.rsl.dictionary.testing.rules.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialSelection_loadsRepositoryDataWhenSnapshotIsMissing() = runTest {
        val categoryId = "category-2"
        val sign = TestDataFactory.sign(id = "sign-1", categoryId = categoryId, word = "Дом")
        val syncData = TestDataFactory.syncData(signs = listOf(sign))
        val signRepository = FakeSignRepository(syncData, initialSyncData = null)

        val viewModel = CategoryDetailViewModel(
            signRepository,
            SavedStateHandle(mapOf("categoryId" to categoryId))
        )
        advanceUntilIdle()

        assertEquals(1, signRepository.loadDataWithSyncCalls)
        assertEquals(listOf(sign), viewModel.signs.value)
        assertEquals(listOf("Д"), viewModel.groupedSigns.value.keys.toList())
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.error.value)
    }

    @Test
    fun repositoryUpdates_refreshVisibleSignsWithoutManualReload() = runTest {
        val categoryId = "category-1"
        val initialSign = TestDataFactory.sign(id = "sign-1", word = "Банан", categoryId = categoryId)
        val updatedSign = TestDataFactory.sign(id = "sign-2", word = "Арбуз", categoryId = categoryId)
        val signRepository = FakeSignRepository(
            TestDataFactory.syncData(signs = listOf(initialSign))
        )
        val viewModel = CategoryDetailViewModel(signRepository, SavedStateHandle())

        viewModel.loadSigns(categoryId)
        advanceUntilIdle()

        signRepository.replaceData(
            TestDataFactory.syncData(signs = listOf(initialSign, updatedSign))
        )
        advanceUntilIdle()

        assertEquals(listOf("Банан", "Арбуз"), viewModel.signs.value.map { it.word })
        assertEquals(listOf("А", "Б"), viewModel.groupedSigns.value.keys.toList())
        assertEquals(0, signRepository.loadDataWithSyncCalls)
        assertNull(viewModel.error.value)
    }

    @Test
    fun loadFailure_withoutExistingData_mapsBlockingError() = runTest {
        val signRepository = mockk<SignRepository> {
            every { syncData } returns MutableStateFlow(null)
            every { dataStatus } returns MutableStateFlow(RepositoryDataStatus.Idle)
            every { refreshState } returns MutableStateFlow(SignRepositoryRefreshState.Idle)
            coEvery { loadDataWithSync() } throws
                SignRepositoryError.NetworkError(IOException("offline"))
        }

        val viewModel = CategoryDetailViewModel(
            signRepository,
            SavedStateHandle(mapOf("categoryId" to "category-1"))
        )
        advanceUntilIdle()

        assertEquals("Ошибка сети", viewModel.error.value)
        assertEquals(emptyList<Any>(), viewModel.signs.value)
        assertEquals(emptyMap<String, List<Any>>(), viewModel.groupedSigns.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun emptyCategory_isShownAsValidContentState() = runTest {
        val signRepository = FakeSignRepository(
            TestDataFactory.syncData(
                signs = listOf(
                    TestDataFactory.sign(id = "sign-1", categoryId = "category-other", word = "Книга")
                )
            )
        )
        val viewModel = CategoryDetailViewModel(signRepository, SavedStateHandle())

        viewModel.loadSigns("category-empty")
        advanceUntilIdle()

        assertEquals(emptyList<Any>(), viewModel.signs.value)
        assertEquals(emptyMap<String, List<Any>>(), viewModel.groupedSigns.value)
        assertNull(viewModel.error.value)
        assertFalse(viewModel.isLoading.value)
    }
}
