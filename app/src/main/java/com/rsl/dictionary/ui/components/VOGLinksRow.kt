package com.rsl.dictionary.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable

@Composable
fun VOGLinksRow(context: Context) {
    fun openUrl(url: String) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        )
    }

    Column {
        AppInfoRow(
            icon = Icons.Default.Info,
            text = "Официальный сайт",
            trailingIcon = Icons.Default.Info,
            onClick = { openUrl("https://voginfo.ru") }
        )
        AppInfoRow(
            icon = Icons.Default.Info,
            text = "Контакты",
            trailingIcon = Icons.Default.Info,
            onClick = { openUrl("https://voginfo.ru/about/contacts/") }
        )
        AppInfoRow(
            icon = Icons.Default.Phone,
            text = "+7 (499) 255 6704",
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:+74992556704"))
                )
            }
        )
        AppInfoRow(
            icon = Icons.Default.Info,
            text = "ВКонтакте",
            trailingIcon = Icons.Default.Info,
            onClick = { openUrl("https://vk.com/voginfo") }
        )
        AppInfoRow(
            icon = Icons.Default.Send,
            text = "Telegram",
            trailingIcon = Icons.Default.Info,
            onClick = { openUrl("https://t.me/voginfo") }
        )
    }
}
