package com.rsl.dictionary.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rsl.dictionary.models.Category

fun sfSymbolToMaterialIcon(sfSymbol: String?): ImageVector = when (sfSymbol) {
    "magnifyingglass" -> Icons.Default.Search
    "heart.fill" -> Icons.Default.Favorite
    "heart" -> Icons.Default.Favorite
    "square.grid.2x2" -> Icons.Default.Home
    "square.grid.2x2.fill" -> Icons.Default.Home
    "book.fill" -> Icons.Default.Info
    "gear" -> Icons.Default.Settings
    "gearshape" -> Icons.Default.Settings
    "gearshape.fill" -> Icons.Default.Settings
    "hand.raised.fill" -> Icons.Default.Info
    "folder.fill" -> Icons.Default.Home
    "folder" -> Icons.Default.Home
    "chevron.right" -> Icons.Default.Info
    "arrow.clockwise" -> Icons.Default.Refresh
    "exclamationmark.triangle.fill" -> Icons.Default.Warning
    "trash.fill" -> Icons.Default.Delete
    "envelope" -> Icons.Default.Email
    "link" -> Icons.Default.Info
    "globe" -> Icons.Default.Info
    "phone" -> Icons.Default.Phone
    "person.2" -> Icons.Default.Info
    "pawprint.fill" -> Icons.Default.Favorite
    "pawprint" -> Icons.Default.Favorite
    "paperplane" -> Icons.Default.Send
    "arrow.up.right.square" -> Icons.Default.Info
    "tray.fill" -> Icons.Default.Info
    "textformat.abc" -> Icons.Default.Info
    "numbers" -> Icons.Default.Info
    "number" -> Icons.Default.Info
    "clock" -> Icons.Default.Info
    "clock.fill" -> Icons.Default.Info
    else -> Icons.Default.Home
}

@Composable
fun CategoryCardView(category: Category, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .semantics { contentDescription = "Категория ${category.name}" },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = sfSymbolToMaterialIcon(category.icon),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            AutoResizeCategoryTitle(
                text = category.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )
            Text(
                text = "${category.signCount} жестов",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun AutoResizeCategoryTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    val baseStyle = MaterialTheme.typography.titleMedium
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val candidateSizes = listOf(16.sp, 15.sp, 14.sp, 13.sp, 12.sp, 11.sp)

    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = with(density) { maxWidth.roundToPx() }
        val resolvedFontSize = remember(text, maxWidthPx, baseStyle) {
            candidateSizes.firstOrNull { candidateSize ->
                val result = textMeasurer.measure(
                    text = AnnotatedString(text),
                    style = baseStyle.copy(fontSize = candidateSize),
                    maxLines = 1,
                    softWrap = false,
                    constraints = Constraints(maxWidth = maxWidthPx)
                )
                !result.hasVisualOverflow
            } ?: candidateSizes.last()
        }

        Text(
            text = text,
            style = baseStyle.copy(fontSize = resolvedFontSize),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
