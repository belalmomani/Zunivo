package com.zunivo.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zunivo.app.R
import com.zunivo.app.ui.components.EmptyState
import com.zunivo.app.ui.components.ScreenHeader

@Composable
fun FavoritesScreen() {
    Column {
        ScreenHeader(stringResource(R.string.nav_favorites))
        EmptyState(
            icon = Icons.Filled.Favorite,
            title = stringResource(R.string.fav_empty_title),
            message = stringResource(R.string.fav_empty_msg),
        )
    }
}
