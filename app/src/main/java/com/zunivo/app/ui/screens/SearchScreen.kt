package com.zunivo.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zunivo.app.R
import com.zunivo.app.feature.search.Category
import com.zunivo.app.ui.components.EmptyState
import com.zunivo.app.ui.components.ScreenHeader

/** Phase 2: UI shell only. Real Iconify search is wired in phase 3 and is labelled as such. */
@Composable
fun SearchScreen() {
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(-1) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader(stringResource(R.string.nav_search))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        )
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(Category.entries) { category ->
                FilterChip(
                    selected = selected == category.ordinal,
                    onClick = { selected = if (selected == category.ordinal) -1 else category.ordinal },
                    label = { Text(stringResource(category.labelRes)) },
                )
            }
        }
        EmptyState(
            icon = Icons.Filled.Search,
            title = stringResource(R.string.search_dev_title),
            message = stringResource(R.string.search_dev_msg),
        )
    }
}
