package com.rsl.dictionary.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsl.dictionary.models.Lesson
import com.rsl.dictionary.models.RepositoryDataStatus
import com.rsl.dictionary.models.ScreenDataStatus
import com.rsl.dictionary.repositories.protocols.LessonRepository
import com.rsl.dictionary.repositories.protocols.SignRepository
import com.rsl.dictionary.utilities.ErrorMessageMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltViewModel
class LessonsViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val signRepository: SignRepository
) : ViewModel() {

    private val _lessons = MutableStateFlow<List<Lesson>>(emptyList())
    val lessons: StateFlow<List<Lesson>> = _lessons.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _screenStatus = MutableStateFlow<ScreenDataStatus>(ScreenDataStatus.Loaded)
    val screenStatus: StateFlow<ScreenDataStatus> = _screenStatus.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        updateScreenStatus(signRepository.dataStatus.value)
        viewModelScope.launch {
            signRepository.dataStatus.collectLatest { updateScreenStatus(it) }
        }
        loadLessons()
    }

    fun loadLessons() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            runCatching { lessonRepository.getAllLessons() }
                .onSuccess {
                    _lessons.value = it
                    if (_screenStatus.value !is ScreenDataStatus.LoadedWithCachedWarning) {
                        updateScreenStatus(signRepository.dataStatus.value)
                    }
                }
                .onFailure {
                    _error.value = _statusMessage.value ?: ErrorMessageMapper.map(it)
                }
            _isLoading.value = false
        }
    }

    private fun updateScreenStatus(dataStatus: RepositoryDataStatus) {
        val status = when (dataStatus) {
            RepositoryDataStatus.Idle,
            RepositoryDataStatus.Updated,
            RepositoryDataStatus.UpToDate -> ScreenDataStatus.Loaded

            is RepositoryDataStatus.UsingCachedData -> {
                ScreenDataStatus.LoadedWithCachedWarning(dataStatus.reason)
            }

            is RepositoryDataStatus.NoData -> ScreenDataStatus.Error(dataStatus.reason)
        }

        _screenStatus.value = status
        _statusMessage.value = ErrorMessageMapper.map(status)
        if (status !is ScreenDataStatus.Error && _error.value == _statusMessage.value) {
            _error.value = null
        }
    }
}
