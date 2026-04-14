package com.rsl.dictionary.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsl.dictionary.repositories.protocols.VideoRepository
import com.rsl.dictionary.services.cache.VideoCacheDirectoryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val _cacheSize = MutableStateFlow("0.0 MB")
    val cacheSize: StateFlow<String> = _cacheSize.asStateFlow()

    private val _isCacheClearing = MutableStateFlow(false)
    val isCacheClearing: StateFlow<Boolean> = _isCacheClearing.asStateFlow()

    private val _showCacheCleared = MutableStateFlow(false)
    val showCacheCleared: StateFlow<Boolean> = _showCacheCleared.asStateFlow()

    init {
        refreshCacheSize()
    }

    fun clearCache() {
        viewModelScope.launch {
            _isCacheClearing.value = true
            videoRepository.clearCache()
            refreshCacheSize()
            _showCacheCleared.value = true
            Timber.i("Cache cleared by user")
            _isCacheClearing.value = false

            delay(2_000)
            _showCacheCleared.value = false
        }
    }

    fun dismissCacheClearedDialog() {
        _showCacheCleared.value = false
    }

    private fun refreshCacheSize() {
        viewModelScope.launch {
            val shortTermSize = directorySize(VideoCacheDirectoryManager.shortTermDir(context))
            val favoritesSize = directorySize(VideoCacheDirectoryManager.favoritesDir(context))
            val totalSizeMb = (shortTermSize + favoritesSize).toDouble() / (1024 * 1024)
            _cacheSize.value = String.format(Locale.US, "%.1f MB", totalSizeMb)
        }
    }

    private fun directorySize(directory: File): Long {
        return directory.listFiles()?.sumOf { file ->
            if (file.isDirectory) directorySize(file) else file.length()
        } ?: 0L
    }
}
