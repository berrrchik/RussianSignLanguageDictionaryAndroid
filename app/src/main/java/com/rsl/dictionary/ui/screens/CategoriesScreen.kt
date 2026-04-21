package com.rsl.dictionary.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rsl.dictionary.R
import com.rsl.dictionary.services.analytics.rememberAnalyticsService
import com.rsl.dictionary.ui.components.CategoryCardView
import com.rsl.dictionary.ui.components.EmptyStateView
import com.rsl.dictionary.ui.components.ErrorView
import com.rsl.dictionary.ui.components.LoadingView
import com.rsl.dictionary.ui.components.ScreenTitleWithOfflineStatus
import com.rsl.dictionary.ui.navigation.Screen
import com.rsl.dictionary.viewmodels.CategoriesViewModel
import com.rsl.dictionary.models.ScreenDataStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    navController: NavController,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val analyticsService = rememberAnalyticsService()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val screenStatus by viewModel.screenStatus.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        analyticsService.logScreenView("categories", "CategoriesScreen")
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    ScreenTitleWithOfflineStatus(
                        title = stringResource(R.string.tab_categories)
                    )
                }
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading && categories.isEmpty() -> {
                        LoadingView(stringResource(R.string.loading_categories))
                    }

                    screenStatus is ScreenDataStatus.Error && error != null -> {
                        ErrorView(
                            message = error.orEmpty(),
                            retryAction = { viewModel.retryAfterBlockingError() }
                        )
                    }

                    categories.isEmpty() -> {
                        EmptyStateView(
                            icon = Icons.Default.Info,
                            title = stringResource(R.string.no_data),
                            message = stringResource(R.string.data_not_loaded)
                        )
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 150.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                        ) {
                            items(categories, key = { it.id }) { category ->
                                CategoryCardView(category = category) {
                                    viewModel.onCategoryOpened(category)
                                    navController.navigate(Screen.CategoryDetail.createRoute(category.id))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
