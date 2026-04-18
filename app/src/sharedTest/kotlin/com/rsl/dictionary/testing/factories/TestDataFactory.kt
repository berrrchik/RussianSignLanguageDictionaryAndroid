package com.rsl.dictionary.testing.factories

import com.rsl.dictionary.models.Category
import com.rsl.dictionary.models.Lesson
import com.rsl.dictionary.models.Sign
import com.rsl.dictionary.models.SignSynonym
import com.rsl.dictionary.models.SignVideo
import com.rsl.dictionary.models.SyncData

object TestDataFactory {
    private const val defaultTimestamp = 1_710_000_000L

    fun category(
        id: String = "category-1",
        name: String = "Быт",
        order: Int = 0,
        signCount: Int = 1,
        icon: String? = "house",
        color: String? = "#FF8A00",
        createdAt: Long? = defaultTimestamp,
        updatedAt: Long? = defaultTimestamp
    ): Category = Category(
        id = id,
        name = name,
        order = order,
        signCount = signCount,
        icon = icon,
        color = color,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun video(
        id: Int = 1,
        url: String = "https://example.com/videos/sign-1.mp4",
        contextDescription: String = "Базовое использование жеста",
        order: Int = 0,
        createdAt: Long? = defaultTimestamp,
        updatedAt: Long? = defaultTimestamp
    ): SignVideo = SignVideo(
        id = id,
        url = url,
        contextDescription = contextDescription,
        order = order,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun synonym(
        id: String = "sign-2",
        word: String = "Приветствие"
    ): SignSynonym = SignSynonym(
        id = id,
        word = word
    )

    fun sign(
        id: String = "sign-1",
        word: String = "Привет",
        description: String = "Жест приветствия",
        categoryId: String = "category-1",
        videos: List<SignVideo> = listOf(video()),
        synonyms: List<SignSynonym> = listOf(synonym())
    ): Sign = Sign(
        id = id,
        word = word,
        description = description,
        categoryId = categoryId,
        videos = videos,
        synonyms = synonyms
    )

    fun lesson(
        id: String = "lesson-1",
        title: String = "Базовые жесты",
        description: String = "Вводный урок",
        videoUrl: String = "https://example.com/videos/lesson-1.mp4",
        order: Int = 0,
        createdAt: Long? = defaultTimestamp,
        updatedAt: Long? = defaultTimestamp
    ): Lesson = Lesson(
        id = id,
        title = title,
        description = description,
        videoUrl = videoUrl,
        order = order,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun syncData(
        categories: List<Category> = listOf(category()),
        signs: List<Sign> = listOf(sign()),
        lessons: List<Lesson> = listOf(lesson()),
        lastUpdated: Long = defaultTimestamp
    ): SyncData = SyncData(
        categories = categories,
        signs = signs,
        lessons = lessons,
        lastUpdated = lastUpdated
    )
}
