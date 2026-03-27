package com.rsl.dictionary.utilities.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DataLoadCoordinator<T> {
    private val mutex = Mutex()
    private var activeDeferred: Deferred<T>? = null

    suspend fun load(scope: CoroutineScope, loader: suspend () -> T): T {
        return mutex.withLock {
            activeDeferred?.takeIf { it.isActive } ?: scope.async { loader() }.also {
                activeDeferred = it
            }
        }.await()
    }
}
