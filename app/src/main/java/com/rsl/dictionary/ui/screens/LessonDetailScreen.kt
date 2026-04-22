package com.rsl.dictionary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rsl.dictionary.R
import com.rsl.dictionary.services.analytics.rememberAnalyticsService
import com.rsl.dictionary.ui.components.ErrorView
import com.rsl.dictionary.ui.components.LoadingView
import com.rsl.dictionary.ui.components.VideoPlayerView
import com.rsl.dictionary.ui.navigation.Screen
import com.rsl.dictionary.viewmodels.LessonDetailViewModel
import com.rsl.dictionary.viewmodels.LessonVideoPlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetailScreen(
    lessonId: String,
    navController: NavController,
    viewModel: LessonDetailViewModel = hiltViewModel(),
    videoViewModel: LessonVideoPlayerViewModel = hiltViewModel()
) {
    val analyticsService = rememberAnalyticsService()
    val lesson by viewModel.lesson.collectAsStateWithLifecycle()
    val allLessons by viewModel.allLessons.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val videoUri by videoViewModel.videoUri.collectAsStateWithLifecycle()
    val isVideoLoading by videoViewModel.isLoading.collectAsStateWithLifecycle()
    val videoError by videoViewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        analyticsService.logScreenView("lesson_detail", "LessonDetailScreen")
    }

    LaunchedEffect(lessonId) {
        viewModel.loadLesson(lessonId)
    }

    LaunchedEffect(lesson?.id) {
        val currentLesson = lesson ?: return@LaunchedEffect
        videoViewModel.loadVideo(currentLesson.videoUrl)
    }

    DisposableEffect(Unit) {
        onDispose {
            videoViewModel.clearVideo()
        }
    }

    fun closeScreen() {
        navController.popBackStack()
    }

    BackHandler {
        closeScreen()
    }

    val currentLesson = lesson
    val currentIndex = allLessons.indexOfFirst { it.id == currentLesson?.id }
    val previousLesson = if (currentIndex > 0) allLessons[currentIndex - 1] else null
    val nextLesson = if (currentIndex >= 0 && currentIndex < allLessons.lastIndex) {
        allLessons[currentIndex + 1]
    } else {
        null
    }
    val previousLessonInBackStack = navController
        .previousBackStackEntry
        ?.arguments
        ?.getString("lessonId")

    fun openLesson(targetLessonId: String) {
        navController.navigate(Screen.LessonDetail.createRoute(targetLessonId))
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(currentLesson?.title.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = {
                        closeScreen()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading && currentLesson == null -> {
                LoadingView(
                    message = stringResource(R.string.loading_lessons)
                )
            }

            error != null && currentLesson == null -> {
                ErrorView(
                    message = error.orEmpty(),
                    retryAction = { viewModel.loadLesson(lessonId) }
                )
            }

            currentLesson != null -> {
                LazyColumn(
                    modifier = Modifier.padding(paddingValues)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isVideoLoading -> {
                                    LoadingView(message = stringResource(R.string.loading_lesson_video))
                                }

                                videoError != null -> {
                                    ErrorView(
                                        message = videoError.orEmpty(),
                                        retryAction = {
                                            videoViewModel.loadVideo(currentLesson.videoUrl)
                                        }
                                    )
                                }

                                videoUri != null -> {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        VideoPlayerView(
                                            videoUri = videoUri!!,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(16f / 9f)
                                        )
                                        Text(
                                            text = stringResource(R.string.lesson_video_online_only),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.description_label),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = currentLesson.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            previousLesson?.let { lessonItem ->
                                OutlinedButton(
                                    onClick = {
                                        if (previousLessonInBackStack == lessonItem.id) {
                                            navController.popBackStack()
                                        } else {
                                            openLesson(lessonItem.id)
                                        }
                                    }
                                ) {
                                    Text("\u2190 ${stringResource(R.string.previous_lesson)}")
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            nextLesson?.let { lessonItem ->
                                OutlinedButton(
                                    onClick = {
                                        openLesson(lessonItem.id)
                                    }
                                ) {
                                    Text("${stringResource(R.string.next_lesson)} \u2192")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
