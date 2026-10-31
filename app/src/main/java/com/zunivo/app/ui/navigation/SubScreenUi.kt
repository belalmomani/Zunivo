package com.zunivo.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.zunivo.app.R

fun SubScreen.titleRes(): Int = when (this) {
    SubScreen.FOLDERS -> R.string.menu_folders
    SubScreen.EDITOR -> R.string.menu_editor
    SubScreen.STUDIO -> R.string.menu_studio
    SubScreen.PACKS -> R.string.menu_packs
    SubScreen.DESIGNERS -> R.string.menu_designers
    SubScreen.STORE -> R.string.menu_store
    SubScreen.DOWNLOADS -> R.string.menu_downloads
    SubScreen.SETTINGS -> R.string.menu_settings
    SubScreen.HELP -> R.string.menu_help
    SubScreen.PRIVACY -> R.string.menu_privacy
    SubScreen.ABOUT -> R.string.menu_about
}

fun SubScreen.icon(): ImageVector = when (this) {
    SubScreen.FOLDERS -> Icons.Filled.List
    SubScreen.EDITOR -> Icons.Filled.Create
    SubScreen.STUDIO -> Icons.Filled.Star
    SubScreen.PACKS -> Icons.Filled.AddCircle
    SubScreen.DESIGNERS -> Icons.Filled.Face
    SubScreen.STORE -> Icons.Filled.ShoppingCart
    SubScreen.DOWNLOADS -> Icons.Filled.KeyboardArrowDown
    SubScreen.SETTINGS -> Icons.Filled.Settings
    SubScreen.HELP -> Icons.Filled.Email
    SubScreen.PRIVACY -> Icons.Filled.Lock
    SubScreen.ABOUT -> Icons.Filled.Info
}

/** Phase of the execution plan in which the feature becomes real (see docs/PLAN.md). */
fun SubScreen.plannedPhase(): Int = when (this) {
    SubScreen.EDITOR, SubScreen.STUDIO -> 4
    SubScreen.FOLDERS, SubScreen.PACKS, SubScreen.DOWNLOADS -> 5
    SubScreen.DESIGNERS, SubScreen.STORE -> 6
    else -> 2
}
