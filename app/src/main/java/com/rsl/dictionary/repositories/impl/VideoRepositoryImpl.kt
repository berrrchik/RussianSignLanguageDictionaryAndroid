package com.rsl.dictionary.repositories.impl

import android.content.Context
import android.net.Uri
import com.rsl.dictionary.errors.VideoRepositoryError
import com.rsl.dictionary.models.FavoriteEntry
import com.rsl.dictionary.models.FavoriteOfflineVideo
import com.rsl.dictionary.models.Sign
import com.rsl.dictionary.models.SignVideo
import com.rsl.dictionary.repositories.protocols.FavoriteOfflinePreparationResult
import com.rsl.dictionary.repositories.protocols.VideoRepository
import com.rsl.dictionary.services.cache.VideoCacheDirectoryManager
import com.rsl.dictionary.services.cache.VideoCacheService
import com.rsl.dictionary.services.network.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import timber.log.Timber

class VideoRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val videoCacheService: VideoCacheService,
    private val networkMonitor: NetworkMonitor
) : VideoRepository {

    override suspend fun getVideoURL(video: SignVideo, useFavoritesCache: Boolean): Uri {
        return try {
            val shortTermDir = VideoCacheDirectoryManager.shortTermDir(context)
            val favoritesDir = VideoCacheDirectoryManager.favoritesDir(context)
            Timber.d(
                "VideoRepository: request videoId=%d, useFavoritesCache=%s",
                video.id,
                useFavoritesCache
            )

            videoCacheService.getFromMemory(video)?.let {
                Timber.d("VideoRepository: memory cache hit for videoId=%d", video.id)
                return it
            }

            if (useFavoritesCache) {
                if (videoCacheService.isCached(video, favoritesDir)) {
                    Timber.d(
                        "VideoRepository: favorites disk cache hit for videoId=%d dir=%s",
                        video.id,
                        favoritesDir.absolutePath
                    )
                    return videoCacheService.getOrDownload(video, favoritesDir, context)
                }
                ensureNetworkAvailable()
                Timber.d(
                    "VideoRepository: downloading videoId=%d into favorites cache dir=%s",
                    video.id,
                    favoritesDir.absolutePath
                )
                return videoCacheService.getOrDownload(video, favoritesDir, context)
            }

            if (videoCacheService.isCached(video, shortTermDir)) {
                Timber.d(
                    "VideoRepository: short-term disk cache hit for videoId=%d dir=%s",
                    video.id,
                    shortTermDir.absolutePath
                )
                return videoCacheService.getOrDownload(video, shortTermDir, context)
            }

            if (videoCacheService.isCached(video, favoritesDir)) {
                Timber.d(
                    "VideoRepository: fallback favorites disk cache hit for videoId=%d dir=%s",
                    video.id,
                    favoritesDir.absolutePath
                )
                return videoCacheService.getOrDownload(video, favoritesDir, context)
            }

            ensureNetworkAvailable()
            Timber.d(
                "VideoRepository: downloading videoId=%d into short-term cache dir=%s",
                video.id,
                shortTermDir.absolutePath
            )
            videoCacheService.getOrDownload(video, shortTermDir, context)
        } catch (error: VideoRepositoryError) {
            Timber.e(error, "VideoRepository: repository error for videoId=%d", video.id)
            throw error
        } catch (error: IOException) {
            Timber.e(error, "VideoRepository: IO error for videoId=%d", video.id)
            throw VideoRepositoryError.DownloadFailed(error)
        } catch (error: Exception) {
            Timber.e(error, "VideoRepository: unexpected cache error for videoId=%d", video.id)
            throw VideoRepositoryError.CacheError(error)
        }
    }

    override suspend fun prepareFavoriteOffline(sign: Sign): FavoriteOfflinePreparationResult {
        if (sign.videosArray.isEmpty()) {
            return FavoriteOfflinePreparationResult.Ready(emptyList())
        }
        val favoritesDir = VideoCacheDirectoryManager.favoritesDir(context)
        val shortTermDir = VideoCacheDirectoryManager.shortTermDir(context)
        val downloadedVideos = mutableListOf<FavoriteOfflineVideo>()
        Timber.d(
            "VideoRepository: preparing favorite offline media signId=%s, videos=%d",
            sign.id,
            sign.videosArray.size,
        )
        return try {
            sign.videosArray.forEach { video ->
                val favoriteFile = ensureVideoInFavoritesDir(video, favoritesDir, shortTermDir)
                downloadedVideos += FavoriteOfflineVideo(
                    videoId = video.id,
                    fileName = favoriteFile.name
                )
                Timber.d(
                    "VideoRepository: favorite file ready signId=%s videoId=%d path=%s",
                    sign.id,
                    video.id,
                    favoriteFile.absolutePath
                )
            }
            FavoriteOfflinePreparationResult.Ready(downloadedVideos)
        } catch (error: Throwable) {
            val mappedError = when (error) {
                is VideoRepositoryError -> error
                is IOException -> VideoRepositoryError.DownloadFailed(error)
                else -> VideoRepositoryError.CacheError(error)
            }
            Timber.e(
                mappedError,
                "VideoRepository: failed to prepare favorite offline media for signId=%s",
                sign.id
            )
            FavoriteOfflinePreparationResult.Failed(
                downloadedVideos = downloadedVideos,
                error = mappedError
            )
        }
    }

    /**
     * Гарантирует наличие файла видео в папке favorites_videos.
     * Приоритет: 1) уже есть в favorites_videos на диске, 2) есть в short_term_cache на диске —
     * копируем оттуда, 3) скачиваем из сети (требует соединения).
     */
    private suspend fun ensureVideoInFavoritesDir(
        video: SignVideo,
        favoritesDir: File,
        shortTermDir: File
    ): File {
        val targetFile = File(favoritesDir, "video_${video.id}.mp4")

        // 1. Уже лежит в favorites — готово
        if (targetFile.exists()) {
            Timber.d(
                "VideoRepository: favorites disk cache hit for videoId=%d path=%s",
                video.id,
                targetFile.absolutePath
            )
            return targetFile
        }

        favoritesDir.mkdirs()

        // 2. Есть в short-term cache — копируем, сеть не нужна
        val shortTermFile = File(shortTermDir, "video_${video.id}.mp4")
        if (shortTermFile.exists()) {
            Timber.d(
                "VideoRepository: copying videoId=%d from short-term to favorites cache",
                video.id
            )
            shortTermFile.copyTo(targetFile, overwrite = true)
            targetFile.setLastModified(System.currentTimeMillis())
            return targetFile
        }

        // 3. Нигде нет — нужна сеть
        ensureNetworkAvailable()
        Timber.d(
            "VideoRepository: downloading videoId=%d into favorites cache dir=%s",
            video.id,
            favoritesDir.absolutePath
        )
        val uri = videoCacheService.getOrDownload(video, favoritesDir, context)
        return uri.path?.let(::File)?.takeIf { it.exists() }
            ?: throw VideoRepositoryError.CacheError(
                IllegalStateException("Favorite file missing after download for videoId=${video.id}")
            )
    }

    override suspend fun clearCache() {
        val shortTermDir = VideoCacheDirectoryManager.shortTermDir(context)
        videoCacheService.clearInMemoryCache()
        videoCacheService.clearDirectory(shortTermDir)
        Timber.d("VideoRepository: cleared short-term video cache dir=%s", shortTermDir.absolutePath)
    }

    override suspend fun removeFavoriteOffline(entry: FavoriteEntry) {
        val favoritesDir = VideoCacheDirectoryManager.favoritesDir(context)
        entry.downloadedVideos.forEach { video ->
            val file = File(favoritesDir, video.fileName)
            VideoCacheDirectoryManager.deleteFile(file)
            Timber.d(
                "VideoRepository: removed favorite offline file signId=%s videoId=%d path=%s",
                entry.signId,
                video.videoId,
                file.absolutePath
            )
        }
    }

    private fun ensureNetworkAvailable() {
        if (!networkMonitor.isConnected()) {
            Timber.w("VideoRepository: network unavailable, failing fast before download")
            throw VideoRepositoryError.NoInternet
        }
    }
}