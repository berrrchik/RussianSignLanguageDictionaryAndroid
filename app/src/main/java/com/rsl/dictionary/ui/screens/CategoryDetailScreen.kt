package com.rsl.dictionary.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rsl.dictionary.R
import com.rsl.dictionary.services.analytics.rememberAnalyticsService
import com.rsl.dictionary.ui.components.AlphabeticScrollbarList
import com.rsl.dictionary.ui.components.EmptyStateView
import com.rsl.dictionary.ui.components.ErrorView
import com.rsl.dictionary.ui.components.LoadingView
import com.rsl.dictionary.ui.navigation.Screen
import com.rsl.dictionary.viewmodels.CategoriesViewModel
import com.rsl.dictionary.viewmodels.CategoryDetailViewModel
import com.rsl.dictionary.viewmodels.FavoritesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    categoryId: String,
    navController: NavController,
    viewModel: CategoryDetailViewModel = hiltViewModel()
) {
    val analyticsService = rememberAnalyticsService()
    val signs by viewModel.signs.collectAsStateWithLifecycle()
    val groupedSigns by viewModel.groupedSigns.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val categoriesViewModel: CategoriesViewModel = hiltViewModel()
    val favoritesViewModel: FavoritesViewModel = hiltViewModel()
    val categories by categoriesViewModel.categories.collectAsStateWithLifecycle()
    val favorites by favoritesViewModel.favorites.collectAsStateWithLifecycle()
    val categoryTitle = categories.firstOrNull { it.id == categoryId }?.name ?: ""

    LaunchedEffect(categoryId) {
        viewModel.loadSigns(categoryId)
    }

    LaunchedEffect(Unit) {
        analyticsService.logScreenView("category_detail", "CategoryDetailScreen")
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(categoryTitle) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    LoadingView(stringResource(R.string.loading_signs))
                }

                error != null -> {
                    ErrorView(message = error.orEmpty(), retryAction = { viewModel.loadSigns(categoryId) })
                }

                signs.isEmpty() -> {
                    EmptyStateView(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.no_signs_found),
                        message = stringResource(R.string.no_signs_in_category)
                    )
                }

                else -> {
                    AlphabeticScrollbarList(
                        groupedSigns = groupedSigns,
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
}
