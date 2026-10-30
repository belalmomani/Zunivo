package com.zunivo.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zunivo.app.R
import com.zunivo.app.ui.components.ActionCard
import com.zunivo.app.ui.components.ScreenHeader
import com.zunivo.app.ui.navigation.SubScreen
import com.zunivo.app.ui.navigation.icon
import com.zunivo.app.ui.navigation.titleRes

private val menuOrder = listOf(
    SubScreen.FOLDERS, SubScreen.EDITOR, SubScreen.PACKS, SubScreen.DESIGNERS, SubScreen.STORE,
    SubScreen.DOWNLOADS, SubScreen.SETTINGS, SubScreen.HELP, SubScreen.PRIVACY, SubScreen.ABOUT,
)

@Composable
fun MoreScreen(onOpen: (SubScreen) -> Unit) {
    Column {
        ScreenHeader(stringResource(R.string.more_title))
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(menuOrder) { screen ->
                ActionCard(
                    icon = screen.icon(),
                    title = stringResource(screen.titleRes()),
                    onClick = { onOpen(screen) },
                    trailing = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
        }
    }
}
