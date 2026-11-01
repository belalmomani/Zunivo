package com.zunivo.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zunivo.app.R
import com.zunivo.app.ui.components.EmptyState
import com.zunivo.app.ui.components.ScreenHeader
import com.zunivo.app.ui.navigation.SubScreen
import com.zunivo.app.ui.navigation.icon

/** Designer pages need a backend. Until one is configured nothing fake is shown. */
@Composable
fun DesignersScreen(onBack: () -> Unit) {
    Column {
        ScreenHeader(stringResource(R.string.menu_designers), onBack)
        EmptyState(
            icon = SubScreen.DESIGNERS.icon(),
            title = stringResource(R.string.designers_not_connected_title),
            message = stringResource(R.string.designers_not_connected_msg),
        )
    }
}
