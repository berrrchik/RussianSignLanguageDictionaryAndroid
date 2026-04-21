package com.rsl.dictionary.repositories.impl

import com.rsl.dictionary.config.ApiConfig
import com.rsl.dictionary.errors.SyncError
import com.rsl.dictionary.models.SyncData
import com.rsl.dictionary.models.SyncMetadata
import com.rsl.dictionary.repositories.protocols.SyncFetchResult
import com.rsl.dictionary.repositories.protocols.SyncRepository
import com.rsl.dictionary.services.network.ETagManager
import com.rsl.dictionary.services.network.http.ApiJsonDecoder
import com.rsl.dictionary.services.network.http.HttpResponseHandler
import com.rsl.dictionary.services.network.http.HttpResult
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import timber.log.Timber

class SyncRepositoryImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val etagManager: ETagManager,
    private val httpResponseHandler: HttpResponseHandler
) : SyncRepository {

    override suspend fun checkForUpdates(lastUpdated: Long): SyncMetadata {
        return try {
            val request = buildRequest(
                url = ApiConfig.Endpoints.syncCheck(lastUpdated),
                etag = etagManager.getETag(SYNC_CHECK_ETAG_KEY)
            )

            withContext(Dispatchers.IO) {
                okHttpClient.newCall(request).execute().use { response ->
                    when (
                        val result = httpResponseHandler.handle(response) { body ->
                            ApiJsonDecoder.json.decodeFromString<SyncMetadata>(body)
                        }
                    ) {
                        is HttpResult.NotModified -> SyncMetadata(lastUpdated = 0, hasUpdates = false)
                        is HttpResult.Success -> {
                            result.etag?.let { etagManager.saveETag(SYNC_CHECK_ETAG_KEY, it) }
                            result.data
                        }

                        is HttpResult.Error -> {
                            Timber.e(
                                "Sync check HTTP error: code=%d, message=%s",
                                result.code,
                                result.message
                            )
                            throw SyncError.UnknownError(
                                IllegalStateException("Sync check failed with HTTP ${result.code}: ${result.message}")
                            )
                        }
                    }
                }
            }
        } catch (error: IOException) {
            throw SyncError.NetworkError(error)
        } catch (error: SerializationException) {
            throw SyncError.DecodingError(error)
        } catch (error: SyncError) {
            throw error
        } catch (error: Exception) {
            Timber.e(error, "Unexpected sync check failure")
            throw SyncError.UnknownError(error)
        }
    }

    override suspend fun fetchAllData(cachedDataProvider: (() -> SyncData?)?): SyncFetchResult {
        return try {
            val request = buildRequest(
                url = ApiConfig.Endpoints.syncData,
                etag = etagManager.getETag(SYNC_DATA_ETAG_KEY)
            )

            withContext(Dispatchers.IO) {
                okHttpClient.newCall(request).execute().use { response ->
                    when (
                        val result = httpResponseHandler.handle(response) { body ->
                            ApiJsonDecoder.json.decodeFromString<SyncData>(body)
                        }
                    ) {
                        is HttpResult.NotModified -> {
                            val cachedData = cachedDataProvider?.invoke() ?: throw SyncError.NoInternet
                            SyncFetchResult.NotModified(cachedData)
                        }

                        is HttpResult.Success -> {
                            result.etag?.let { etagManager.saveETag(SYNC_DATA_ETAG_KEY, it) }
                            SyncFetchResult.Updated(result.data)
                        }

                        is HttpResult.Error -> {
                            Timber.e(
                                "Sync data HTTP error: code=%d, message=%s",
                                result.code,
                                result.message
                            )
                            throw SyncError.UnknownError(
                                IllegalStateException("Sync data failed with HTTP ${result.code}: ${result.message}")
                            )
                        }
                    }
                }
            }
        } catch (error: IOException) {
            throw SyncError.NetworkError(error)
        } catch (error: SerializationException) {
            throw SyncError.DecodingError(error)
        } catch (error: SyncError) {
            throw error
        } catch (error: Exception) {
            Timber.e(error, "Unexpected sync data failure")
            throw SyncError.UnknownError(error)
        }
    }

    private fun buildRequest(url: String, etag: String?): Request {
        val builder = Request.Builder()
            .url(url)
            .get()

        if (!etag.isNullOrBlank()) {
            builder.header("If-None-Match", etag)
        }

        return builder.build()
    }

    private companion object {
        const val SYNC_DATA_ETAG_KEY = "sync_data_etag"
        const val SYNC_CHECK_ETAG_KEY = "sync_check_etag"
    }
}
