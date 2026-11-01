package com.zunivo.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zunivo.app.R
import com.zunivo.app.domain.IconItem
import com.zunivo.app.feature.library.LibraryViewModel
import com.zunivo.app.ui.components.EmptyState
import com.zunivo.app.ui.components.ScreenHeader

@Composable
fun FavoritesScreen(onOpenIcon: (IconItem) -> Unit, libVm: LibraryViewModel = viewModel()) {
    val state by libVm.state.collectAsStateWithLifecycle()
    Column {
        ScreenHeader(stringResource(R.string.nav_favorites))
        if (state.library.favorites.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Favorite,
                title = stringResource(R.string.fav_empty_title),
                message = stringResource(R.string.fav_empty_msg),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(88.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.library.favorites, key = { it.icon.id }) { snap ->
                    val item = snap.toItem()
                    IconTile(item) { onOpenIcon(item) }
                }
            }
        }
    }
}
