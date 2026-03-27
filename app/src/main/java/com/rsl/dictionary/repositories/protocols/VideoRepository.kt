package com.rsl.dictionary.repositories.protocols

import android.net.Uri
import com.rsl.dictionary.models.Sign
import com.rsl.dictionary.models.SignVideo

interface VideoRepository {
    suspend fun getVideoURL(video: SignVideo, useFavoritesCache: Boolean): Uri
    suspend fun prefetchVideos(sign: Sign)
    suspend fun clearFavoritesCache(sign: Sign)
}
