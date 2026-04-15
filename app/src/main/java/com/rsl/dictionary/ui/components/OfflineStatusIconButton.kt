package com.rsl.dictionary.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rsl.dictionary.R
import com.rsl.dictionary.viewmodels.OfflineStatusViewModel
import kotlinx.coroutines.delay

@Composable
fun ScreenTitleWithOfflineStatus(
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        OfflineStatusIconButton()
    }
}

@Composable
fun OfflineStatusIconButton(
    viewModel: OfflineStatusViewModel = hiltViewModel()
) {
    val isNetworkConnected by viewModel.isNetworkConnected.collectAsStateWithLifecycle()
    var isMessageVisible by remember { mutableStateOf(false) }
    var messageVersion by remember { mutableStateOf(0) }

    if (isNetworkConnected) return

    LaunchedEffect(messageVersion) {
        if (messageVersion == 0) return@LaunchedEffect
        delay(2_500)
        isMessageVisible = false
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.animateContentSize()
    ) {
        IconButton(
            onClick = {
                isMessageVisible = true
                messageVersion += 1
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.wifi_off_24),
                contentDescription = stringResource(R.string.offline_status_icon_description),
                tint = MaterialTheme.colorScheme.secondary
            )
        }

        AnimatedVisibility(visible = isMessageVisible) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shadowElevation = 4.dp,
                tonalElevation = 2.dp
            ) {
                Text(
                    text = stringResource(R.string.offline_status_message),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .widthIn(max = 220.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}
