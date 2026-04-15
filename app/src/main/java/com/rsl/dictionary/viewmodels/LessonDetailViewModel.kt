package com.rsl.dictionary.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsl.dictionary.errors.LessonRepositoryError
import com.rsl.dictionary.models.Lesson
import com.rsl.dictionary.repositories.protocols.LessonRepository
import com.rsl.dictionary.services.analytics.AnalyticsService
import com.rsl.dictionary.utilities.ErrorMessageMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LessonDetailViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val analyticsService: AnalyticsService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _lesson = MutableStateFlow<Lesson?>(null)
    val lesson: StateFlow<Lesson?> = _lesson.asStateFlow()

    private val _allLessons = MutableStateFlow<List<Lesson>>(emptyList())
    val allLessons: StateFlow<List<Lesson>> = _allLessons.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val hasPrevious: Boolean
        get() = currentLessonIndex > 0

    val hasNext: Boolean
        get() = currentLessonIndex >= 0 && currentLessonIndex < allLessons.value.lastIndex

    init {
        savedStateHandle.get<String>("lessonId")?.let { lessonId ->
            loadLesson(lessonId)
        }
    }

    fun loadLesson(lessonId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            runCatching {
                val lessons = lessonRepository.getAllLessons()
                val currentLesson = lessons.firstOrNull { it.id == lessonId }
                lessons to currentLesson
            }.onSuccess { (lessons, currentLesson) ->
                _allLessons.value = lessons
                _lesson.value = currentLesson
                if (currentLesson == null) {
                    _error.value = ErrorMessageMapper.map(LessonRepositoryError.NotFound)
                } else {
                    analyticsService.logLessonViewed(currentLesson.id, currentLesson.title)
                }
            }.onFailure {
                _error.value = ErrorMessageMapper.map(it)
            }
            _isLoading.value = false
        }
    }

    private val currentLessonIndex: Int
        get() = allLessons.value.indexOfFirst { it.id == lesson.value?.id }
}
