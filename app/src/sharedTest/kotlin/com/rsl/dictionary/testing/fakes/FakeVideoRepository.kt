package com.rsl.dictionary.testing.fakes

import android.net.Uri
import com.rsl.dictionary.models.FavoriteEntry
import com.rsl.dictionary.models.FavoriteOfflineVideo
import com.rsl.dictionary.models.Sign
import com.rsl.dictionary.models.SignVideo
import com.rsl.dictionary.repositories.protocols.FavoriteOfflinePreparationResult
import com.rsl.dictionary.repositories.protocols.VideoRepository

class FakeVideoRepository : VideoRepository {
    val preparedFavoriteSignIds = mutableListOf<String>()
    var clearCacheCallCount = 0
    val clearedFavoritesSignIds = mutableListOf<String>()
    var uriResolver: (SignVideo, Boolean) -> Uri = { video, _ -> Uri.parse(video.url) }
    var prepareFavoriteOfflineResultFactory: (Sign) -> FavoriteOfflinePreparationResult = { sign ->
        FavoriteOfflinePreparationResult.Ready(
            downloadedVideos = sign.videosArray.map { video ->
                FavoriteOfflineVideo(videoId = video.id, fileName = "video_${video.id}.mp4")
            }
        )
    }

    override suspend fun getVideoURL(video: SignVideo, useFavoritesCache: Boolean): Uri {
        return uriResolver(video, useFavoritesCache)
    }

    override suspend fun prepareFavoriteOffline(sign: Sign): FavoriteOfflinePreparationResult {
        preparedFavoriteSignIds += sign.id
        return prepareFavoriteOfflineResultFactory(sign)
    }

    override suspend fun clearCache() {
        clearCacheCallCount += 1
    }

    override suspend fun removeFavoriteOffline(entry: FavoriteEntry) {
        clearedFavoritesSignIds += entry.signId
    }
}
