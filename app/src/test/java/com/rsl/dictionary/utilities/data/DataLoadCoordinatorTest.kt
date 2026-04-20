package com.rsl.dictionary.utilities.data

import com.rsl.dictionary.testing.rules.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DataLoadCoordinatorTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun concurrentLoad_callsExecuteSingleLoader() = runTest {
        val coordinator = DataLoadCoordinator<Int>()
        val release = CompletableDeferred<Int>()
        var loaderCalls = 0

        val firstCaller = async {
            coordinator.load(backgroundScope) {
                loaderCalls += 1
                release.await()
            }
        }
        val secondCaller = async {
            coordinator.load(backgroundScope) {
                loaderCalls += 1
                -1
            }
        }

        runCurrent()
        assertEquals(1, loaderCalls)

        release.complete(42)

        assertEquals(42, firstCaller.await())
        assertEquals(42, secondCaller.await())
        assertEquals(1, loaderCalls)
    }

    @Test
    fun loaderError_isPropagatedToAllWaiters() = runTest {
        val coordinator = DataLoadCoordinator<Int>()
        val loaderScope = CoroutineScope(backgroundScope.coroutineContext + SupervisorJob())
        val release = CompletableDeferred<Unit>()
        val expected = IllegalStateException("load failed")
        var loaderCalls = 0

        val firstCaller = async {
            runCatching {
                coordinator.load(loaderScope) {
                    loaderCalls += 1
                    release.await()
                    throw expected
                }
            }.exceptionOrNull()
        }
        val secondCaller = async {
            runCatching {
                coordinator.load(loaderScope) {
                    loaderCalls += 1
                    -1
                }
            }.exceptionOrNull()
        }

        runCurrent()
        assertEquals(1, loaderCalls)

        release.complete(Unit)

        val firstError = firstCaller.await()
        val secondError = secondCaller.await()

        assertTrue(firstError is IllegalStateException)
        assertTrue(secondError is IllegalStateException)
        assertEquals("load failed", firstError?.message)
        assertEquals("load failed", secondError?.message)
    }

    @Test
    fun nextLoadAfterCompletion_startsNewLoader() = runTest {
        val coordinator = DataLoadCoordinator<Int>()
        var loaderCalls = 0

        val first = coordinator.load(backgroundScope) {
            loaderCalls += 1
            1
        }
        val second = coordinator.load(backgroundScope) {
            loaderCalls += 1
            2
        }

        assertEquals(1, first)
        assertEquals(2, second)
        assertEquals(2, loaderCalls)
    }
}
