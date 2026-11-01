package com.zunivo.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.zunivo.app.R
import com.zunivo.app.domain.IconItem
import com.zunivo.app.feature.search.Category
import com.zunivo.app.feature.search.ErrorKind
import com.zunivo.app.feature.search.SearchStatus
import com.zunivo.app.feature.search.SearchUiState
import com.zunivo.app.feature.search.SearchViewModel
import com.zunivo.app.feature.search.StyleFilter
import com.zunivo.app.feature.search.applyStyle
import com.zunivo.app.ui.components.EmptyState
import com.zunivo.app.ui.components.ScreenHeader

@Composable
fun SearchScreen(onOpenEditor: (IconItem) -> Unit, vm: SearchViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val selected = state.selected
    if (selected != null) {
        BackHandler { vm.select(null) }
        IconDetailScreen(item = selected, onBack = { vm.select(null) }, onOpenEditor = onOpenEditor)
    } else {
        SearchContent(state, vm)
    }
}

@Composable
private fun SearchContent(state: SearchUiState, vm: SearchViewModel) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScreenHeader(stringResource(R.string.nav_search))
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(Category.entries) { category ->
                FilterChip(
                    selected = state.category == category,
                    onClick = { vm.onCategory(if (state.category == category) null else category) },
                    label = { Text(stringResource(category.labelRes)) },
                )
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(StyleFilter.entries) { mode ->
                FilterChip(
                    selected = state.style == mode,
                    onClick = { vm.onStyle(mode) },
                    label = { Text(stringResource(styleLabel(mode))) },
                )
            }
        }
        val note = when {
            state.category != null -> R.string.search_category_note
            state.style == StyleFilter.OUTLINE || state.style == StyleFilter.FILLED -> R.string.search_style_note
            else -> null
        }
        if (note != null) {
            Text(
                stringResource(note),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val usedOtherQuery = state.searchedAs.isNotEmpty() && state.category == null &&
            state.searchedAs.first() != state.query.trim().lowercase()
        if (usedOtherQuery) {
            Text(
                stringResource(R.string.search_searched_as, state.searchedAs.joinToString(", ")),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth()) { ResultsArea(state, vm) }
    }
}

@Composable
private fun ResultsArea(state: SearchUiState, vm: SearchViewModel) {
    when (state.status) {
        SearchStatus.IDLE -> EmptyState(
            Icons.Filled.Search,
            stringResource(R.string.search_idle_title),
            stringResource(R.string.search_idle_msg),
        )
        SearchStatus.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        SearchStatus.EMPTY -> EmptyState(
            Icons.Filled.Search,
            stringResource(R.string.search_empty_title),
            stringResource(if (state.untranslated) R.string.search_untranslated_msg else R.string.search_empty_msg),
        )
        SearchStatus.ERROR -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            EmptyState(
                Icons.Filled.Warning,
                stringResource(errorTitle(state.error)),
                if (state.error == ErrorKind.SERVER) stringResource(R.string.error_server_msg, state.serverCode)
                else stringResource(errorMessage(state.error)),
            )
            Button(onClick = vm::retry) { Text(stringResource(R.string.action_retry)) }
        }
        SearchStatus.SUCCESS -> {
            val filtered = state.items.applyStyle(state.style)
            if (filtered.isEmpty()) {
                EmptyState(
                    Icons.Filled.Search,
                    stringResource(R.string.search_empty_title),
                    stringResource(R.string.search_filter_empty_msg),
                )
            } else {
                val visible = filtered.take(state.shown)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(88.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(visible, key = { it.ref.id }) { item ->
                        IconTile(item) { vm.select(item) }
                    }
                    if (filtered.size > state.shown || state.hasMore) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            if (state.loadingMore) {
                                Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(Modifier.size(24.dp))
                                }
                            } else {
                                TextButton(onClick = vm::showMore, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.action_load_more))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun IconTile(item: IconItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AsyncImage(
                model = item.ref.svgUrl(height = 48, colorHex = ICON_COLOR),
                contentDescription = item.ref.name,
                modifier = Modifier.size(40.dp),
            )
            Text(
                item.ref.name,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** White so monotone icons are visible on the dark theme; multicolor icons ignore it. */
internal const val ICON_COLOR = "#FFFFFF"

private fun styleLabel(mode: StyleFilter): Int = when (mode) {
    StyleFilter.ALL -> R.string.style_all
    StyleFilter.OUTLINE -> R.string.style_outline
    StyleFilter.FILLED -> R.string.style_filled
    StyleFilter.COLORED -> R.string.style_colored
}

private fun errorTitle(kind: ErrorKind?): Int = when (kind) {
    ErrorKind.OFFLINE -> R.string.error_offline_title
    ErrorKind.TIMEOUT -> R.string.error_timeout_title
    ErrorKind.SERVER -> R.string.error_server_title
    ErrorKind.BAD_DATA, null -> R.string.error_bad_data_title
}

private fun errorMessage(kind: ErrorKind?): Int = when (kind) {
    ErrorKind.OFFLINE -> R.string.error_offline_msg
    ErrorKind.TIMEOUT -> R.string.error_timeout_msg
    ErrorKind.SERVER -> R.string.error_server_msg
    ErrorKind.BAD_DATA, null -> R.string.error_bad_data_msg
}
