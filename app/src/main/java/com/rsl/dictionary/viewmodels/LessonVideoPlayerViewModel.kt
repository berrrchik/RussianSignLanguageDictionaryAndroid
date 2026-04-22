package com.rsl.dictionary.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsl.dictionary.repositories.protocols.LessonVideoRepository
import com.rsl.dictionary.utilities.ErrorMessageMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LessonVideoPlayerViewModel @Inject constructor(
    private val lessonVideoRepository: LessonVideoRepository
) : ViewModel() {

    private val _videoUri = MutableStateFlow<Uri?>(null)
    val videoUri: StateFlow<Uri?> = _videoUri.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadVideo(videoUrl: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _videoUri.value = null
            runCatching { lessonVideoRepository.getLessonVideoUri(videoUrl) }
                .onSuccess { _videoUri.value = it }
                .onFailure { _error.value = ErrorMessageMapper.map(it) }
            _isLoading.value = false
        }
    }

    fun clearVideo() {
        _videoUri.value = null
        _isLoading.value = false
        _error.value = null
    }
}
