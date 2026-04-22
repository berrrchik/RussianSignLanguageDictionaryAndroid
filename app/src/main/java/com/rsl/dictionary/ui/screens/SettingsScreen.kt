package com.rsl.dictionary.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rsl.dictionary.BuildConfig
import com.rsl.dictionary.R
import com.rsl.dictionary.services.analytics.rememberAnalyticsService
import com.rsl.dictionary.ui.components.AppInfoRow
import com.rsl.dictionary.ui.components.AuthorInfoRow
import com.rsl.dictionary.ui.components.VOGInfoRow
import com.rsl.dictionary.ui.components.VOGLinksRow
import com.rsl.dictionary.viewmodels.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val analyticsService = rememberAnalyticsService()
    val context = LocalContext.current
    val isCacheClearing by viewModel.isCacheClearing.collectAsStateWithLifecycle()
    val shortTermCacheSize by viewModel.shortTermCacheSize.collectAsStateWithLifecycle()
    val favoritesOfflineSize by viewModel.favoritesOfflineSize.collectAsStateWithLifecycle()
    val showCacheClearedDialog by viewModel.showCacheCleared.collectAsStateWithLifecycle()

    var showClearCacheDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        analyticsService.logScreenView("settings", "SettingsScreen")
    }

    LazyColumn {
        item {
            Text(
                text = stringResource(R.string.about_app),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
        item {
            AuthorInfoRow(
                label = "Версия",
                value = context.getString(
                    R.string.version_format,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE.toString()
                )
            )
        }
        item {
            AuthorInfoRow(
                label = stringResource(R.string.app_description_label),
                value = "Словарь русского жестового языка — приложение для изучения жестового языка с видео-демонстрациями жестов."
            )
        }
        item {
            AuthorInfoRow(
                label = stringResource(R.string.developer),
                value = "Анастасия Берчик"
            )
        }
        item {
            AppInfoRow(
                icon = Icons.Default.Email,
                text = "berrrchik@mail.ru",
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:berrrchik@mail.ru"))
                    )
                }
            )
        }
        item {
            AppInfoRow(
                icon = Icons.Default.Info,
                text = "GitHub",
                trailingIcon = Icons.Default.Info,
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/berrrchik"))
                    )
                }
            )
        }

        item { HorizontalDivider(modifier = Modifier.padding(top = 8.dp)) }

        item {
            Text(
                text = stringResource(R.string.partners),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
        item {
            VOGInfoRow(
                name = "Всероссийское общество глухих (ВОГ)",
                description = "Всероссийское общество глухих — общероссийская общественная организация инвалидов по слуху, созданная для защиты прав и интересов глухих граждан России."
            )
        }
        item {
            VOGLinksRow(context = context)
        }

        item { HorizontalDivider(modifier = Modifier.padding(top = 8.dp)) }

        item {
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isCacheClearing) { showClearCacheDialog = true }
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.clear_cache),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = stringResource(R.string.short_term_cache_size_format, shortTermCacheSize),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    text = stringResource(R.string.clear_cache_scope_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        item {
            AuthorInfoRow(
                label = stringResource(R.string.favorites_offline_size_label),
                value = favoritesOfflineSize
            )
        }
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text(stringResource(R.string.clear_cache_title)) },
            text = { Text(stringResource(R.string.clear_cache_message)) },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCacheDialog = false
                        viewModel.clearCache()
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

    if (showCacheClearedDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCacheClearedDialog() },
            title = { Text(stringResource(R.string.cache_cleared_title)) },
            text = { Text(stringResource(R.string.cache_cleared_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissCacheClearedDialog() }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}
