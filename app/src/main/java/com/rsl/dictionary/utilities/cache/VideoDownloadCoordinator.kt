package com.rsl.dictionary.utilities.cache

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class VideoDownloadCoordinator {
    private val mutex = Mutex()
    private val activeDownloads = mutableMapOf<Int, Deferred<Uri>>()

    suspend fun download(
        videoId: Int,
        scope: CoroutineScope,
        downloader: suspend () -> Uri
    ): Uri {
        val deferred = mutex.withLock {
            activeDownloads[videoId]?.takeIf { it.isActive } ?: scope.async {
                downloader()
            }.also { createdDeferred ->
                activeDownloads[videoId] = createdDeferred
            }
        }

        return try {
            deferred.await()
        } finally {
            mutex.withLock {
                if (activeDownloads[videoId] === deferred && !deferred.isActive) {
                    activeDownloads.remove(videoId)
                }
            }
        }
    }
}
