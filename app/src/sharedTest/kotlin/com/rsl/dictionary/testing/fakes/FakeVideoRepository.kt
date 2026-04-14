package com.rsl.dictionary.testing.fakes

import android.net.Uri
import com.rsl.dictionary.models.Sign
import com.rsl.dictionary.models.SignVideo
import com.rsl.dictionary.repositories.protocols.VideoRepository

class FakeVideoRepository : VideoRepository {
    val prefetchedSignIds = mutableListOf<String>()
    var clearCacheCallCount = 0
    val clearedFavoritesSignIds = mutableListOf<String>()
    var uriResolver: (SignVideo, Boolean) -> Uri = { video, _ -> Uri.parse(video.url) }

    override suspend fun getVideoURL(video: SignVideo, useFavoritesCache: Boolean): Uri {
        return uriResolver(video, useFavoritesCache)
    }

    override suspend fun prefetchVideos(sign: Sign) {
        prefetchedSignIds += sign.id
    }

    override suspend fun clearCache() {
        clearCacheCallCount += 1
    }

    override suspend fun clearFavoritesCache(sign: Sign) {
        clearedFavoritesSignIds += sign.id
    }
}
