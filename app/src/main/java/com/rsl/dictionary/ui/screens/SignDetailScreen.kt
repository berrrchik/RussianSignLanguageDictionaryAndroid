package com.rsl.dictionary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rsl.dictionary.R
import com.rsl.dictionary.models.FavoriteOfflineStatus
import com.rsl.dictionary.models.SignSynonym
import com.rsl.dictionary.services.analytics.rememberAnalyticsService
import com.rsl.dictionary.ui.components.ErrorView
import com.rsl.dictionary.ui.components.LoadingView
import com.rsl.dictionary.ui.components.VideoNavigationView
import com.rsl.dictionary.ui.components.VideoPlayerView
import com.rsl.dictionary.ui.navigation.Screen
import com.rsl.dictionary.viewmodels.CategoriesViewModel
import com.rsl.dictionary.viewmodels.SignDetailViewModel
import com.rsl.dictionary.viewmodels.VideoPlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignDetailScreen(
    signId: String,
    navController: NavController,
    viewModel: SignDetailViewModel = hiltViewModel(),
    videoViewModel: VideoPlayerViewModel = hiltViewModel()
) {
    val analyticsService = rememberAnalyticsService()
    val sign by viewModel.sign.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val favoriteOfflineStatus by viewModel.favoriteOfflineStatus.collectAsStateWithLifecycle()
    val isFavoriteActionInProgress by viewModel.isFavoriteActionInProgress.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val visitedSignIds by viewModel.visitedSignIds.collectAsStateWithLifecycle()

    val categoriesViewModel: CategoriesViewModel = hiltViewModel()
    val categories by categoriesViewModel.categories.collectAsStateWithLifecycle()

    val currentVideoIndex by videoViewModel.currentVideoIndex.collectAsStateWithLifecycle()
    val videoUri by videoViewModel.videoUri.collectAsStateWithLifecycle()
    val isVideoLoading by videoViewModel.isLoading.collectAsStateWithLifecycle()
    val videoError by videoViewModel.error.collectAsStateWithLifecycle()
    val useFavoritesCache = favoriteOfflineStatus == FavoriteOfflineStatus.READY_OFFLINE

    var isLoadingSynonym by rememberSaveable { mutableStateOf(false) }
    var synonymError by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingSynonymId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        analyticsService.logScreenView("sign_detail", "SignDetailScreen")
    }

    LaunchedEffect(signId) {
        viewModel.loadSign(signId)
    }

    LaunchedEffect(sign?.id) {
        val currentSign = sign ?: return@LaunchedEffect
        if (currentSign.videosArray.isNotEmpty()) {
            videoViewModel.currentVideoIndex.value = 0
            videoViewModel.loadVideo(currentSign.videosArray.first(), useFavoritesCache)
        }
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

    val currentSign = sign
    val videos = currentSign?.videosArray.orEmpty()
    val currentVideo = videos.getOrNull(currentVideoIndex)
    val categoryName = categories.firstOrNull { it.id == currentSign?.categoryId }?.name.orEmpty()
    val visibleSynonyms = currentSign?.synonyms
        .orEmpty()
        .filter { synonym ->
            synonym.id != currentSign?.id && synonym.id !in visitedSignIds
        }

    fun loadSynonym(synonym: SignSynonym) {
        isLoadingSynonym = true
        synonymError = null
        pendingSynonymId = synonym.id
        viewModel.loadSynonymSign(
            signId = synonym.id,
            onSuccess = { synonymSign ->
                isLoadingSynonym = false
                pendingSynonymId = null
                navController.navigate(
                    Screen.SignDetail.createRoute(
                        signId = synonymSign.id,
                        visitedSignIds = visitedSignIds
                    )
                )
            },
            onFailure = { message ->
                isLoadingSynonym = false
                synonymError = message
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(currentSign?.word.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = {
                        closeScreen()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleFavorite() },
                        enabled = !isFavoriteActionInProgress
                    ) {
                        Icon(
                            imageVector = if (isFavorite) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Default.FavoriteBorder
                            },
                            contentDescription = stringResource(
                                if (isFavorite) {
                                    R.string.remove_from_favorites
                                } else {
                                    R.string.add_to_favorites
                                }
                            ),
                            tint = if (isFavorite) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .height(400.dp)
                ) {
                    LoadingView(message = stringResource(R.string.loading_signs))
                }
            }

            error != null -> {
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .height(400.dp)
                ) {
                    ErrorView(
                        message = error.orEmpty(),
                        retryAction = { viewModel.loadSign(signId) }
                    )
                }
            }

            currentSign != null -> {
                LazyColumn(
                    modifier = Modifier.padding(paddingValues),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(400.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                when {
                                    isVideoLoading -> {
                                        LoadingView(message = stringResource(R.string.loading_video))
                                    }

                                    videoError != null -> {
                                        ErrorView(
                                            message = videoError.orEmpty(),
                                            retryAction = {
                                                currentVideo?.let {
                                                    videoViewModel.loadVideo(it, useFavoritesCache)
                                                }
                                            },
                                            skipAction = if (currentVideoIndex < videos.lastIndex) {
                                                {
                                                    val nextIndex = currentVideoIndex + 1
                                                    videoViewModel.currentVideoIndex.value = nextIndex
                                                    videos.getOrNull(nextIndex)?.let {
                                                        videoViewModel.loadVideo(it, useFavoritesCache)
                                                    }
                                                }
                                            } else {
                                                null
                                            }
                                        )
                                    }

                                    videoUri != null -> {
                                        VideoPlayerView(
                                            videoUri = videoUri!!,
                                            modifier = Modifier.height(400.dp)
                                        )
                                    }

                                    else -> {
                                        LoadingView(message = stringResource(R.string.loading_video))
                                    }
                                }
                            }

                            if (videos.size > 1) {
                                VideoNavigationView(
                                    currentIndex = currentVideoIndex,
                                    totalCount = videos.size,
                                    onPrevious = {
                                        val previousIndex = currentVideoIndex - 1
                                        videoViewModel.currentVideoIndex.value = previousIndex
                                        videos.getOrNull(previousIndex)?.let {
                                            videoViewModel.loadVideo(it, useFavoritesCache)
                                        }
                                    },
                                    onNext = {
                                        val nextIndex = currentVideoIndex + 1
                                        videoViewModel.currentVideoIndex.value = nextIndex
                                        videos.getOrNull(nextIndex)?.let {
                                            videoViewModel.loadVideo(it, useFavoritesCache)
                                        }
                                    }
                                )
                            }

                            if (!currentVideo?.contextDescription.isNullOrBlank()) {
                                Text(
                                    text = currentVideo?.contextDescription.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                )
                            }
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            if (currentSign.description.isNotBlank()) {
                                Text(
                                    text = currentSign.description,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = categoryName,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            favoriteOfflineStatus
                                ?.takeIf { it != FavoriteOfflineStatus.READY_OFFLINE }
                                ?.let { status ->
                                Row(
                                    modifier = Modifier
                                        .padding(top = 12.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(favoriteStatusBackground(status))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = favoriteStatusLabel(status),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = favoriteStatusTextColor(status)
                                    )
                                }
                            }

                            if (visibleSynonyms.isNotEmpty()) {
                                Text(
                                    text = stringResource(R.string.synonyms),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                                )
                            }
                        }
                    }

                    if (isLoadingSynonym) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .padding(horizontal = 16.dp)
                            ) {
                                LoadingView(message = stringResource(R.string.loading_synonym))
                            }
                        }
                    }

                    if (synonymError != null) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .padding(horizontal = 16.dp)
                            ) {
                                ErrorView(
                                    message = synonymError.orEmpty(),
                                    retryAction = {
                                        val retryId = pendingSynonymId ?: return@ErrorView
                                        val synonym = visibleSynonyms.firstOrNull { it.id == retryId }
                                            ?: return@ErrorView
                                        loadSynonym(synonym)
                                    }
                                )
                            }
                        }
                    }

                    items(visibleSynonyms, key = { it.id }) { synonym ->
                        OutlinedButton(
                            onClick = { loadSynonym(synonym) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .testTag("synonym_${synonym.id}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = synonym.word)
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun favoriteStatusLabel(status: FavoriteOfflineStatus): String = when (status) {
    FavoriteOfflineStatus.PENDING -> "Подготавливается для офлайн"
        FavoriteOfflineStatus.READY_OFFLINE -> ""
    FavoriteOfflineStatus.FAILED -> "Не удалось подготовить офлайн"
}

private fun favoriteStatusBackground(status: FavoriteOfflineStatus) = when (status) {
    FavoriteOfflineStatus.PENDING -> Color(0xFFFFF3E0)
    FavoriteOfflineStatus.READY_OFFLINE -> Color(0xFFE6F4EA)
    FavoriteOfflineStatus.FAILED -> Color(0xFFFDECEA)
}

private fun favoriteStatusTextColor(status: FavoriteOfflineStatus) = when (status) {
    FavoriteOfflineStatus.PENDING -> Color(0xFFB26A00)
    FavoriteOfflineStatus.READY_OFFLINE -> Color(0xFF1E7D32)
    FavoriteOfflineStatus.FAILED -> Color(0xFFB3261E)
}
