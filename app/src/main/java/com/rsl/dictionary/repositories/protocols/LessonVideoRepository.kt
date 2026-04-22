package com.rsl.dictionary.repositories.protocols

import android.net.Uri

interface LessonVideoRepository {
    suspend fun getLessonVideoUri(videoUrl: String): Uri
}
