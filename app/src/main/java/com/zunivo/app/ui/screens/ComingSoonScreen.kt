package com.zunivo.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zunivo.app.R
import com.zunivo.app.ui.components.EmptyState
import com.zunivo.app.ui.components.ScreenHeader
import com.zunivo.app.ui.navigation.SubScreen
import com.zunivo.app.ui.navigation.icon
import com.zunivo.app.ui.navigation.plannedPhase
import com.zunivo.app.ui.navigation.titleRes

/** Honest placeholder for features scheduled in later phases. */
@Composable
fun ComingSoonScreen(screen: SubScreen, onBack: () -> Unit) {
    Column {
        ScreenHeader(stringResource(screen.titleRes()), onBack)
        EmptyState(
            icon = screen.icon(),
            title = stringResource(R.string.coming_soon_title),
            message = stringResource(R.string.coming_soon_msg, screen.plannedPhase()),
        )
    }
}
