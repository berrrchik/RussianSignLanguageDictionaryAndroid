package com.rsl.dictionary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rsl.dictionary.models.Category
import com.rsl.dictionary.models.Sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlphabeticScrollbarList(
    groupedSigns: Map<String, List<Sign>>,
    categories: List<Category>,
    favorites: List<String>,
    onSignClick: (Sign) -> Unit
) {
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val sectionKeys = remember(groupedSigns) { groupedSigns.keys.filter { it.isNotBlank() } }
    val sectionIndexes = remember(groupedSigns) { buildSectionIndexes(groupedSigns) }
    val categoriesById = remember(categories) { categories.associateBy { it.id } }
    val showsAlphabetIndex = sectionKeys.isNotEmpty() && sectionKeys.size == groupedSigns.size
    val currentSection by remember(lazyListState, sectionKeys, sectionIndexes) {
        derivedStateOf {
            findCurrentSection(
                firstVisibleItemIndex = lazyListState.firstVisibleItemIndex,
                sectionKeys = sectionKeys,
                sectionIndexes = sectionIndexes
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            state = lazyListState,
            contentPadding = PaddingValues(end = if (showsAlphabetIndex) 28.dp else 0.dp)
        ) {
            groupedSigns.forEach { (letter, signs) ->
                if (letter.isNotBlank()) {
                    stickyHeader(key = "header_$letter") {
                        LetterHeader(
                            letter = letter,
                            isActive = letter == currentSection
                        )
                    }
                }
                items(signs, key = { it.id }) { sign ->
                    SignRowView(
                        sign = sign,
                        categoryName = categoriesById[sign.categoryId]?.name.orEmpty(),
                        isFavorite = sign.id in favorites,
                        onClick = { onSignClick(sign) }
                    )
                }
            }
        }

        if (showsAlphabetIndex) {
            BoxWithConstraints(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(22.dp)
            ) {
                val density = LocalDensity.current
                val compactItemHeight = 14.dp
                val availableItemHeight = maxHeight / sectionKeys.size
                val itemHeight = if (availableItemHeight > compactItemHeight) {
                    compactItemHeight
                } else {
                    availableItemHeight
                }
                val totalHeight = itemHeight * sectionKeys.size
                val itemHeightPx = with(density) { itemHeight.toPx() }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .width(22.dp)
                            .height(totalHeight)
                            .pointerInput(sectionKeys, itemHeightPx) {
                                detectTapGestures { offset ->
                                    scrollToSection(
                                        offsetY = offset.y,
                                        itemHeightPx = itemHeightPx,
                                        sectionKeys = sectionKeys,
                                        sectionIndexes = sectionIndexes,
                                        scope = coroutineScope,
                                        onScroll = { index -> lazyListState.scrollToItem(index) }
                                    )
                                }
                            }
                            .pointerInput(sectionKeys, itemHeightPx) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    scrollToSection(
                                        offsetY = change.position.y,
                                    itemHeightPx = itemHeightPx,
                                    sectionKeys = sectionKeys,
                                    sectionIndexes = sectionIndexes,
                                    scope = coroutineScope,
                                    onScroll = { index -> lazyListState.scrollToItem(index) }
                                    )
                                }
                            },
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        sectionKeys.forEach { letter ->
                            val isActive = letter == currentSection
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .height(itemHeight),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = letter,
                                    fontSize = 10.sp,
                                    lineHeight = 10.sp,
                                    color = if (isActive) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                                    fontWeight = if (isActive) {
                                        FontWeight.SemiBold
                                    } else {
                                        FontWeight.Medium
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildSectionIndexes(groupedSigns: Map<String, List<Sign>>): Map<String, Int> {
    val indexes = linkedMapOf<String, Int>()
    var currentIndex = 0

    groupedSigns.forEach { (key, signs) ->
        indexes[key] = currentIndex
        currentIndex += signs.size + if (key.isNotBlank()) 1 else 0
    }

    return indexes
}

@Composable
private fun LetterHeader(
    letter: String,
    isActive: Boolean
) {
    Text(
        text = letter,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isActive) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

private fun findCurrentSection(
    firstVisibleItemIndex: Int,
    sectionKeys: List<String>,
    sectionIndexes: Map<String, Int>
): String? {
    if (sectionKeys.isEmpty()) return null

    var currentSection = sectionKeys.first()
    sectionKeys.forEach { key ->
        val sectionIndex = sectionIndexes[key] ?: return@forEach
        if (sectionIndex <= firstVisibleItemIndex) {
            currentSection = key
        } else {
            return currentSection
        }
    }

    return currentSection
}

private fun scrollToSection(
    offsetY: Float,
    itemHeightPx: Float,
    sectionKeys: List<String>,
    sectionIndexes: Map<String, Int>,
    scope: CoroutineScope,
    onScroll: suspend (Int) -> Unit
) {
    if (sectionKeys.isEmpty() || itemHeightPx <= 0f) return

    val rawIndex = (offsetY / itemHeightPx).toInt().coerceIn(0, sectionKeys.lastIndex)
    val sectionKey = sectionKeys[rawIndex]
    val itemIndex = sectionIndexes[sectionKey] ?: return

    scope.launch {
        onScroll(itemIndex)
    }
}
