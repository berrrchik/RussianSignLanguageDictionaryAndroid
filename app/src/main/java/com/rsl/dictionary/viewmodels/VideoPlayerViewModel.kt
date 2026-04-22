package com.rsl.dictionary.viewmodels

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsl.dictionary.models.SignVideo
import com.rsl.dictionary.repositories.protocols.VideoRepository
import com.rsl.dictionary.utilities.ErrorMessageMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val currentVideoIndex = MutableStateFlow(0)

    private val _videoUri = MutableStateFlow<Uri?>(null)
    val videoUri: StateFlow<Uri?> = _videoUri.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    @Suppress("UNCHECKED_CAST")
    private var currentVideos: List<SignVideo> =
        savedStateHandle.get<List<SignVideo>>("videos").orEmpty()
    private var currentIsFavorite: Boolean = savedStateHandle["isFavorite"] ?: false

    init {
        val startIndex = savedStateHandle.get<Int>("videoIndex") ?: 0
        currentVideoIndex.value = startIndex
        currentVideos.getOrNull(startIndex)?.let { initialVideo ->
            loadVideo(initialVideo, currentIsFavorite)
        }
    }

    fun loadVideo(video: SignVideo, isFavorite: Boolean) {
        currentIsFavorite = isFavorite
        if (currentVideos.isEmpty()) {
            currentVideos = listOf(video)
            currentVideoIndex.value = 0
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _videoUri.value = null
            runCatching { videoRepository.getVideoURL(video, isFavorite) }
                .onSuccess { _videoUri.value = it }
                .onFailure { _error.value = ErrorMessageMapper.map(it) }
            _isLoading.value = false
        }
    }

    fun navigateTo(index: Int) {
        currentVideoIndex.value = index
        currentVideos.getOrNull(index)?.let { video ->
            loadVideo(video, currentIsFavorite)
        }
    }

    fun clearVideo() {
        _videoUri.value = null
        _isLoading.value = false
        _error.value = null
    }
}
