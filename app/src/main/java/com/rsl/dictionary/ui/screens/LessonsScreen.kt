package com.rsl.dictionary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.rsl.dictionary.models.ScreenDataStatus
import com.rsl.dictionary.services.analytics.rememberAnalyticsService
import com.rsl.dictionary.ui.components.ErrorView
import com.rsl.dictionary.ui.components.LoadingView
import com.rsl.dictionary.ui.components.ScreenTitleWithOfflineStatus
import com.rsl.dictionary.ui.navigation.Screen
import com.rsl.dictionary.viewmodels.LessonsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonsScreen(
    navController: NavController,
    viewModel: LessonsViewModel = hiltViewModel()
) {
    val analyticsService = rememberAnalyticsService()
    val lessons by viewModel.lessons.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val screenStatus by viewModel.screenStatus.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        analyticsService.logScreenView("lessons", "LessonsScreen")
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    ScreenTitleWithOfflineStatus(
                        title = stringResource(R.string.tab_lessons)
                    )
                }
            )

            when {
                isLoading && lessons.isEmpty() -> {
                    LoadingView(stringResource(R.string.loading_lessons))
                }

                screenStatus is ScreenDataStatus.Error && error != null -> {
                    ErrorView(
                        message = error.orEmpty(),
                        retryAction = { viewModel.loadLessons() }
                    )
                }

                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                text = stringResource(R.string.lessons_header),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        items(lessons, key = { it.id }) { lesson ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate(
                                            Screen.LessonDetail.createRoute(lesson.id)
                                        )
                                    }
                                    .padding(horizontal = 16.dp, vertical = 16.dp)
                            ) {
                                Text(
                                    text = lesson.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
