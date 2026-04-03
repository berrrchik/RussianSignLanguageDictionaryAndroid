package com.rsl.dictionary.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rsl.dictionary.R
import com.rsl.dictionary.services.analytics.rememberAnalyticsService
import com.rsl.dictionary.ui.components.AlphabeticScrollbarList
import com.rsl.dictionary.ui.components.EmptyStateView
import com.rsl.dictionary.ui.components.LoadingView
import com.rsl.dictionary.ui.navigation.Screen
import com.rsl.dictionary.viewmodels.CategoriesViewModel
import com.rsl.dictionary.viewmodels.FavoritesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    navController: NavController,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val analyticsService = rememberAnalyticsService()
    val categoriesViewModel: CategoriesViewModel = hiltViewModel()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val groupedFavorites by viewModel.groupedFavorites.collectAsStateWithLifecycle()
    val categories by categoriesViewModel.categories.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        analyticsService.logScreenView("favorites", "FavoritesScreen")
    }

    LaunchedEffect(error) {
        error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_favorites)) },
                actions = {
                    if (favorites.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    containerColor = Color(0xFFFF9800),
                    contentColor = Color.White
                ) {
                    Text(text = data.visuals.message, color = Color.White)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    LoadingView(stringResource(R.string.loading_favorites))
                }

                favorites.isEmpty() -> {
                    EmptyStateView(
                        icon = Icons.Default.FavoriteBorder,
                        title = stringResource(R.string.no_favorites_title),
                        message = stringResource(R.string.no_favorites_message),
                        hint = stringResource(R.string.no_favorites_hint)
                    )
                }

                else -> {
                    AlphabeticScrollbarList(
                        groupedSigns = groupedFavorites,
                        categories = categories,
                        favorites = favorites.map { it.id },
                        onSignClick = { sign ->
                            navController.navigate(Screen.SignDetail.createRoute(sign.id))
                        }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.clear_favorites_title)) },
            text = { Text(stringResource(R.string.clear_favorites_message)) },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearAll()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.clear),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        )
    }
}
