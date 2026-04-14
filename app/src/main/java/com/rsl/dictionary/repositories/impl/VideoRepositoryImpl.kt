package com.rsl.dictionary.repositories.impl

import android.content.Context
import android.net.Uri
import com.rsl.dictionary.errors.VideoRepositoryError
import com.rsl.dictionary.models.Sign
import com.rsl.dictionary.models.SignVideo
import com.rsl.dictionary.repositories.protocols.VideoRepository
import com.rsl.dictionary.services.cache.VideoCacheDirectoryManager
import com.rsl.dictionary.services.cache.VideoCacheService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VideoRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val videoCacheService: VideoCacheService
) : VideoRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun getVideoURL(video: SignVideo, useFavoritesCache: Boolean): Uri {
        return try {
            val shortTermDir = VideoCacheDirectoryManager.shortTermDir(context)
            val favoritesDir = VideoCacheDirectoryManager.favoritesDir(context)

            if (useFavoritesCache) {
                return videoCacheService.getOrDownload(video, favoritesDir, context)
            }

            videoCacheService.getFromMemory(video)?.let { return it }

            if (videoCacheService.isCached(video, shortTermDir)) {
                return videoCacheService.getOrDownload(video, shortTermDir, context)
            }

            if (videoCacheService.isCached(video, favoritesDir)) {
                return videoCacheService.getOrDownload(video, favoritesDir, context)
            }

            videoCacheService.getOrDownload(video, shortTermDir, context)
        } catch (error: VideoRepositoryError) {
            throw error
        } catch (error: IOException) {
            throw VideoRepositoryError.DownloadFailed(error)
        } catch (error: Exception) {
            throw VideoRepositoryError.CacheError(error)
        }
    }

    override suspend fun prefetchVideos(sign: Sign) {
        sign.videosArray.forEach { video ->
            repositoryScope.launch {
                runCatching { getVideoURL(video, useFavoritesCache = true) }
            }
        }
    }

    override suspend fun clearCache() {
        val shortTermDir = VideoCacheDirectoryManager.shortTermDir(context)
        videoCacheService.clearInMemoryCache()
        videoCacheService.clearDirectory(shortTermDir)
    }

    override suspend fun clearFavoritesCache(sign: Sign) {
        val favoritesDir = VideoCacheDirectoryManager.favoritesDir(context)
        sign.videosArray.forEach { video ->
            val file = File(favoritesDir, "video_${video.id}.mp4")
            VideoCacheDirectoryManager.deleteFile(file)
        }
    }
}
