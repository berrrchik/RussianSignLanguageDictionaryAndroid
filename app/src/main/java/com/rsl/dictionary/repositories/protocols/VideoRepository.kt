package com.rsl.dictionary.repositories.protocols

import android.net.Uri
import com.rsl.dictionary.models.FavoriteEntry
import com.rsl.dictionary.models.Sign
import com.rsl.dictionary.models.SignVideo

interface VideoRepository {
    suspend fun getVideoURL(video: SignVideo, useFavoritesCache: Boolean): Uri
    suspend fun prepareFavoriteOffline(sign: Sign): FavoriteOfflinePreparationResult
    suspend fun clearCache()
    suspend fun removeFavoriteOffline(entry: FavoriteEntry)
}
