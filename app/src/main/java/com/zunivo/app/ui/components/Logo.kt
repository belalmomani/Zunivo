package com.zunivo.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zunivo.app.ui.theme.Cyan
import com.zunivo.app.ui.theme.Indigo
import com.zunivo.app.ui.theme.Violet

/** Zunivo "Z" mark drawn as a vector (provisional; final artwork can replace this file only). */
@Composable
fun ZunivoLogo(modifier: Modifier = Modifier, logoSize: Dp = 64.dp) {
    val brush = Brush.linearGradient(listOf(Cyan, Indigo, Violet))
    Canvas(modifier = modifier.size(logoSize)) {
        val u = this.size.minDimension / 100f
        val path = Path().apply {
            moveTo(20f * u, 24f * u)
            lineTo(80f * u, 24f * u)
            lineTo(80f * u, 34f * u)
            lineTo(40f * u, 66f * u)
            lineTo(80f * u, 66f * u)
            lineTo(80f * u, 76f * u)
            lineTo(20f * u, 76f * u)
            lineTo(20f * u, 66f * u)
            lineTo(60f * u, 34f * u)
            lineTo(20f * u, 34f * u)
            close()
        }
        drawPath(path = path, brush = brush)
    }
}
