package com.rsl.dictionary.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rsl.dictionary.R
import com.rsl.dictionary.services.analytics.rememberAnalyticsService
import com.rsl.dictionary.ui.components.AlphabeticScrollbarList
import com.rsl.dictionary.ui.components.EmptyStateView
import com.rsl.dictionary.ui.components.ErrorView
import com.rsl.dictionary.ui.components.LoadingView
import com.rsl.dictionary.ui.components.ScreenTitleWithOfflineStatus
import com.rsl.dictionary.ui.navigation.Screen
import com.rsl.dictionary.utilities.data.SortOrder
import com.rsl.dictionary.viewmodels.FavoritesViewModel
import com.rsl.dictionary.viewmodels.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val analyticsService = rememberAnalyticsService()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val groupedResults by viewModel.groupedResults.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val favoritesViewModel: FavoritesViewModel = hiltViewModel()
    val favorites by favoritesViewModel.favorites.collectAsStateWithLifecycle()

    var categoryMenuExpanded by remember { mutableStateOf(false) }
    val selectedCategoryName = categories.firstOrNull { it.id == selectedCategoryId }?.name
        ?: stringResource(R.string.all_categories)

    LaunchedEffect(Unit) {
        analyticsService.logScreenView("search", "SearchScreen")
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                ScreenTitleWithOfflineStatus(
                    title = stringResource(R.string.tab_search)
                )
            }
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            singleLine = true
        )

        if (categories.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    TextButton(
                        onClick = { categoryMenuExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Text(selectedCategoryName)
                    }
                    DropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(stringResource(R.string.all_categories))
                                    if (selectedCategoryId == null) {
                                        Text("✓", fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            onClick = {
                                viewModel.selectedCategoryId.value = null
                                categoryMenuExpanded = false
                            }
                        )
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(category.name)
                                        if (selectedCategoryId == category.id) {
                                            Text("✓", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.selectedCategoryId.value = category.id
                                    categoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        viewModel.sortOrder.value = if (sortOrder == SortOrder.ASCENDING) {
                            SortOrder.DESCENDING
                        } else {
                            SortOrder.ASCENDING
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (sortOrder == SortOrder.ASCENDING) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = null
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    LoadingView(stringResource(R.string.loading_signs))
                }

                error != null -> {
                    ErrorView(
                        message = error.orEmpty(),
                        retryAction = { viewModel.reload() }
                    )
                }

                groupedResults.isEmpty() && searchQuery.isBlank() -> {
                    EmptyStateView(
                        icon = Icons.Default.Search,
                        title = stringResource(R.string.no_data),
                        message = stringResource(R.string.data_not_loaded)
                    )
                }

                groupedResults.isEmpty() && searchQuery.isNotBlank() -> {
                    EmptyStateView(
                        icon = Icons.Default.Search,
                        title = stringResource(R.string.nothing_found),
                        message = stringResource(R.string.try_different_query)
                    )
                }

                else -> {
                    AlphabeticScrollbarList(
                        groupedSigns = groupedResults,
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
