package com.zunivo.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zunivo.app.R
import com.zunivo.app.core.AppInfo
import com.zunivo.app.feature.search.Category
import com.zunivo.app.ui.components.EmptyState
import com.zunivo.app.ui.components.SectionTitle
import com.zunivo.app.ui.components.TileCard
import com.zunivo.app.ui.components.ZunivoLogo
import com.zunivo.app.ui.navigation.SubScreen
import com.zunivo.app.ui.navigation.icon

@Composable
fun HomeScreen(onOpenSearch: () -> Unit, onOpen: (SubScreen) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZunivoLogo(logoSize = 36.dp)
                Text(AppInfo.NAME_EN, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        item { SearchPill(onClick = onOpenSearch) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TileCard(SubScreen.EDITOR.icon(), stringResource(R.string.home_quick_edit),
                    { onOpen(SubScreen.EDITOR) }, Modifier.weight(1f))
                TileCard(SubScreen.PACKS.icon(), stringResource(R.string.home_quick_pack),
                    { onOpen(SubScreen.PACKS) }, Modifier.weight(1f))
                TileCard(SubScreen.DESIGNERS.icon(), stringResource(R.string.home_quick_designers),
                    { onOpen(SubScreen.DESIGNERS) }, Modifier.weight(1f))
            }
        }
        item {
            Column {
                SectionTitle(stringResource(R.string.home_recent))
                EmptyState(
                    icon = Icons.Filled.Star,
                    title = stringResource(R.string.home_recent_empty_title),
                    message = stringResource(R.string.home_recent_empty_msg),
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(stringResource(R.string.home_categories))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Category.entries) { category ->
                        AssistChip(onClick = onOpenSearch, label = { Text(stringResource(category.labelRes)) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchPill(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                stringResource(R.string.home_search_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
