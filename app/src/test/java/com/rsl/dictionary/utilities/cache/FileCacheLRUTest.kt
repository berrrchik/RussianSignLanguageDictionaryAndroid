package com.rsl.dictionary.utilities.cache

import com.rsl.dictionary.testing.rules.MainDispatcherRule
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FileCacheLRUTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun belowLimit_doesNotDeleteFiles() = runTest {
        val directory = createTempDirectory()
        val first = createFile(directory, "first.bin", 4, lastModified = 100)
        val second = createFile(directory, "second.bin", 5, lastModified = 200)

        FileCacheLRU.enforceSizeLimit(directory, maxSizeBytes = 10, targetPercent = 100)

        assertTrue(first.exists())
        assertTrue(second.exists())
    }

    @Test
    fun exceedingLimitByOneByte_triggersCleanup() = runTest {
        val directory = createTempDirectory()
        val oldest = createFile(directory, "oldest.bin", 6, lastModified = 100)
        val newest = createFile(directory, "newest.bin", 5, lastModified = 200)

        FileCacheLRU.enforceSizeLimit(directory, maxSizeBytes = 10, targetPercent = 100)

        assertFalse(oldest.exists())
        assertTrue(newest.exists())
        assertEquals(5L, directory.listFiles()?.filter { it.isFile }?.sumOf { it.length() })
    }

    @Test
    fun cleanup_removesOldestFilesFirst() = runTest {
        val directory = createTempDirectory()
        val oldest = createFile(directory, "oldest.bin", 4, lastModified = 100)
        val middle = createFile(directory, "middle.bin", 4, lastModified = 200)
        val newest = createFile(directory, "newest.bin", 4, lastModified = 300)

        FileCacheLRU.enforceSizeLimit(directory, maxSizeBytes = 8, targetPercent = 50)

        assertFalse(oldest.exists())
        assertFalse(middle.exists())
        assertTrue(newest.exists())
    }

    @Test
    fun emptyDirectory_doesNotFail() = runTest {
        val directory = createTempDirectory()

        FileCacheLRU.enforceSizeLimit(directory, maxSizeBytes = 1, targetPercent = 80)

        assertTrue(directory.exists())
        assertTrue(directory.listFiles().isNullOrEmpty())
    }

    private fun createTempDirectory(): File = Files.createTempDirectory("file-cache-lru-test").toFile()

    private fun createFile(directory: File, name: String, size: Int, lastModified: Long): File {
        return File(directory, name).apply {
            writeBytes(ByteArray(size) { 1 })
            setLastModified(lastModified)
        }
    }
}
