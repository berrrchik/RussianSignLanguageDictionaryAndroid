package com.rsl.dictionary.repositories.impl

import android.content.SharedPreferences
import app.cash.turbine.test
import com.rsl.dictionary.models.FavoriteOfflineStatus
import com.rsl.dictionary.models.FavoriteOfflineVideo
import com.rsl.dictionary.testing.factories.TestDataFactory
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FavoritesRepositoryImplTest {
    private lateinit var repository: FavoritesRepositoryImpl
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var storedStringSets: MutableMap<String, LinkedHashSet<String>>
    private lateinit var storedStrings: MutableMap<String, String>

    @Before
    fun setUp() {
        storedStringSets = mutableMapOf()
        storedStrings = mutableMapOf()

        val editor = object : SharedPreferences.Editor {
            override fun putStringSet(key: String?, values: Set<String>?): SharedPreferences.Editor {
                requireNotNull(key)
                storedStringSets[key] = LinkedHashSet(values.orEmpty())
                return this
            }

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                requireNotNull(key)
                if (value == null) {
                    storedStrings.remove(key)
                } else {
                    storedStrings[key] = value
                }
                return this
            }

            override fun remove(key: String?): SharedPreferences.Editor {
                requireNotNull(key)
                storedStringSets.remove(key)
                storedStrings.remove(key)
                return this
            }

            override fun clear(): SharedPreferences.Editor = this
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = this
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = this
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = this
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = this
            override fun commit(): Boolean = true
            override fun apply() = Unit
        }

        sharedPreferences = mockk {
            every { getStringSet(any(), any()) } answers {
                storedStringSets[firstArg<String>()] ?: LinkedHashSet(secondArg<Set<String>>().orEmpty())
            }
            every { getString(any(), any()) } answers {
                storedStrings[firstArg<String>()] ?: secondArg<String?>()
            }
            every { edit() } returns editor
        }

        repository = FavoritesRepositoryImpl(sharedPreferences)
    }

    @Test
    fun markFavoritePending_persistsEntryAndSnapshot() = runTest {
        val sign = TestDataFactory.sign(id = "sign-1", word = "Арбуз")

        repository.markFavoritePending(sign)

        assertEquals(listOf("sign-1"), repository.getAll())
        assertEquals(FavoriteOfflineStatus.PENDING, repository.getOfflineStatus("sign-1"))
        assertEquals(listOf("Арбуз"), repository.getCachedSigns().map { it.word })
    }

    @Test
    fun markFavoriteReady_updatesTrackedFilesWithoutChangingOrder() = runTest {
        val sign = TestDataFactory.sign(
            id = "sign-1",
            videos = listOf(
                TestDataFactory.video(id = 1),
                TestDataFactory.video(id = 2)
            )
        )
        repository.markFavoritePending(sign)

        repository.markFavoriteReady(
            signId = "sign-1",
            downloadedVideos = listOf(
                FavoriteOfflineVideo(videoId = 1, fileName = "video_1.mp4"),
                FavoriteOfflineVideo(videoId = 2, fileName = "video_2.mp4")
            )
        )

        val entry = repository.getEntry("sign-1")
        assertEquals(FavoriteOfflineStatus.READY_OFFLINE, entry?.status)
        assertEquals(listOf("video_1.mp4", "video_2.mp4"), entry?.downloadedVideos?.map { it.fileName })
    }

    @Test
    fun markFavoriteFailed_keepsFavoriteButChangesStatus() = runTest {
        val sign = TestDataFactory.sign(id = "sign-1", videos = listOf(TestDataFactory.video(id = 1)))
        repository.markFavoritePending(sign)

        repository.markFavoriteFailed(
            signId = "sign-1",
            downloadedVideos = listOf(FavoriteOfflineVideo(videoId = 1, fileName = "video_1.mp4"))
        )

        assertEquals(listOf("sign-1"), repository.getAll())
        assertEquals(FavoriteOfflineStatus.FAILED, repository.getOfflineStatus("sign-1"))
    }

    @Test
    fun remove_deletesEntryAndCachedSnapshot() = runTest {
        val sign = TestDataFactory.sign(id = "sign-1", word = "Арбуз")
        repository.markFavoritePending(sign)

        repository.remove("sign-1")

        assertEquals(emptyList<String>(), repository.getAll())
        assertEquals(emptyList<String>(), repository.getCachedSigns().map { it.id })
        assertNull(repository.getEntry("sign-1"))
    }

    @Test
    fun favoriteEntriesFlow_emitsStateChanges() = runTest {
        val sign = TestDataFactory.sign(id = "sign-1", videos = listOf(TestDataFactory.video(id = 1)))

        repository.favoriteEntriesFlow.test {
            assertEquals(emptyList<Any>(), awaitItem())

            repository.markFavoritePending(sign)
            assertEquals(FavoriteOfflineStatus.PENDING, awaitItem().single().status)

            repository.markFavoriteReady(
                signId = "sign-1",
                downloadedVideos = listOf(FavoriteOfflineVideo(videoId = 1, fileName = "video_1.mp4"))
            )
            assertEquals(FavoriteOfflineStatus.READY_OFFLINE, awaitItem().single().status)

            repository.remove("sign-1")
            assertEquals(emptyList<Any>(), awaitItem())
        }
    }

    @Test
    fun clearAll_clearsEntriesSnapshotsAndFlows() = runTest {
        val sign = TestDataFactory.sign(id = "sign-1")
        repository.markFavoritePending(sign)

        repository.favoriteEntriesFlow.test {
            assertEquals(1, awaitItem().size)

            repository.clearAll()

            assertEquals(emptyList<Any>(), awaitItem())
            assertEquals(emptyList<String>(), repository.getAll())
            assertEquals(emptyList<String>(), repository.getCachedSigns().map { it.id })
        }
    }

    @Test
    fun legacyFavoriteIds_areMigratedIntoFailedEntries() = runTest {
        storedStringSets["favorite_sign_ids"] = linkedSetOf("sign-1")
        repository = FavoritesRepositoryImpl(sharedPreferences)

        val entry = repository.getEntry("sign-1")

        assertEquals(FavoriteOfflineStatus.FAILED, entry?.status)
        assertEquals(listOf("sign-1"), repository.getAll())
    }
}
