package com.zunivo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Round colour chip. `hex` must be a valid #RRGGBB (callers pass validated values only). */
@Composable
fun ColorSwatch(hex: String, selected: Boolean, onClick: () -> Unit, diameter: Dp = 32.dp) {
    val color = remember(hex) { Color(android.graphics.Color.parseColor(hex)) }
    val border = if (selected) MaterialTheme.colorScheme.onBackground
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    Box(
        Modifier
            .size(diameter)
            .clip(CircleShape)
            .background(color)
            .border(if (selected) 3.dp else 1.dp, border, CircleShape)
            .clickable(onClick = onClick),
    )
}

fun parseHexColor(hex: String?): Color? =
    if (hex != null && com.zunivo.app.domain.HexColor.isValid(hex)) Color(android.graphics.Color.parseColor(hex)) else null
