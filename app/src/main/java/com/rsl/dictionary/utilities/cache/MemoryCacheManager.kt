package com.rsl.dictionary.utilities.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MemoryCacheManager<T> {
    private val mutex = Mutex()
    private var cached: T? = null

    suspend fun get(): T? = mutex.withLock { cached }
    suspend fun set(data: T) = mutex.withLock { cached = data }
    suspend fun clear() = mutex.withLock { cached = null }
}
