package com.rsl.dictionary.services.search

import com.rsl.dictionary.config.ApiConfig
import com.rsl.dictionary.errors.SBERTSearchError
import com.rsl.dictionary.models.SBERTSearchResponse
import com.rsl.dictionary.models.SBERTSearchResult
import com.rsl.dictionary.services.network.http.ApiJsonDecoder
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class SBERTSearchService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun search(
        query: String,
        limit: Int,
        minSimilarity: Double = 0.7
    ): List<SBERTSearchResult> {
        return try {
            val requestBody = """
                {"text":"$query","limit":$limit,"min_similarity":$minSimilarity}
            """.trimIndent().toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url(ApiConfig.Endpoints.sbertSearch)
                .post(requestBody)
                .build()

            withContext(Dispatchers.IO) {
                okHttpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    val decodedResponse = ApiJsonDecoder.json.decodeFromString<SBERTSearchResponse>(body)

                    if (!decodedResponse.success) {
                        throw SBERTSearchError.ServerError(decodedResponse.error?.code ?: response.code)
                    }

                    decodedResponse.data?.results.orEmpty()
                }
            }
        } catch (error: IOException) {
            throw SBERTSearchError.NetworkError(error)
        } catch (error: SerializationException) {
            throw SBERTSearchError.DecodingError(error)
        } catch (error: SBERTSearchError) {
            throw error
        } catch (_: Exception) {
            throw SBERTSearchError.UnknownError
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
