package com.rsl.dictionary.repositories.impl

import android.net.Uri
import com.rsl.dictionary.config.ApiConfig
import com.rsl.dictionary.errors.LessonVideoError
import com.rsl.dictionary.repositories.protocols.LessonVideoRepository
import com.rsl.dictionary.services.network.NetworkMonitor
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class LessonVideoRepositoryImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val networkMonitor: NetworkMonitor
) : LessonVideoRepository {

    override suspend fun getLessonVideoUri(videoUrl: String): Uri {
        if (!networkMonitor.isConnected()) {
            throw LessonVideoError.NoInternet
        }

        val resolvedUrl = ApiConfig.videoUrl(videoUrl) ?: throw LessonVideoError.UrlNotFound

        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(resolvedUrl)
                .get()
                .header("Range", "bytes=0-0")
                .build()

            try {
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw LessonVideoError.ServerUnavailable
                    }
                    Uri.parse(resolvedUrl)
                }
            } catch (error: LessonVideoError) {
                throw error
            } catch (error: IOException) {
                throw LessonVideoError.ServerUnavailable
            } catch (error: Exception) {
                throw LessonVideoError.UnknownError(error)
            }
        }
    }
}
