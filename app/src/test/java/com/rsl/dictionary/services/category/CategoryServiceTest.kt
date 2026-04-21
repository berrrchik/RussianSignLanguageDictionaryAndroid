package com.rsl.dictionary.services.category

import app.cash.turbine.test
import com.rsl.dictionary.testing.fakes.FakeSignRepository
import com.rsl.dictionary.testing.factories.TestDataFactory
import com.rsl.dictionary.testing.rules.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryServiceTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun getCategories_sortsByOrder() = runTest {
        val repository = FakeSignRepository(
            TestDataFactory.syncData(
                categories = listOf(
                    TestDataFactory.category(id = "2", order = 2),
                    TestDataFactory.category(id = "1", order = 1)
                )
            )
        )
        val service = CategoryService(repository)

        val result = service.getCategories()

        assertEquals(listOf("1", "2"), result.map { it.id })
    }

    @Test
    fun categoriesFlow_updatesAfterLoading() = runTest {
        val repository = FakeSignRepository(
            TestDataFactory.syncData(
                categories = listOf(
                    TestDataFactory.category(id = "1", order = 2),
                    TestDataFactory.category(id = "2", order = 1)
                )
            )
        )
        val service = CategoryService(repository)

        service.categoriesFlow.test {
            assertEquals(emptyList<String>(), awaitItem().map { it.id })
            assertEquals(listOf("2", "1"), service.getCategories().map { it.id })
            assertEquals(listOf("2", "1"), awaitItem().map { it.id })
        }
    }

    @Test
    fun getCategory_returnsNullWhenNotFound() = runTest {
        val service = CategoryService(FakeSignRepository())

        assertNull(service.getCategory("missing"))
    }

    @Test
    fun categoriesFlow_updatesWhenRepositoryPublishesNewData() = runTest {
        val repository = FakeSignRepository(
            TestDataFactory.syncData(
                categories = listOf(TestDataFactory.category(id = "1", order = 1))
            )
        )
        val service = CategoryService(repository)
        service.getCategories()

        repository.replaceData(
            TestDataFactory.syncData(
                categories = listOf(
                    TestDataFactory.category(id = "3", order = 3),
                    TestDataFactory.category(id = "2", order = 2)
                )
            )
        )
        advanceUntilIdle()

        assertEquals(listOf("2", "3"), service.categoriesFlow.value.map { it.id })
    }
}
