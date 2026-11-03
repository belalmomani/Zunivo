package com.zunivo.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.startapp.sdk.ads.banner.Banner
import com.zunivo.app.ads.AdsConfig

/** Standard Start.io banner. Shown above the navigation bar, never over content, and never required for any feature. */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    if (!AdsConfig.enabled) return
    AndroidView(modifier = modifier.fillMaxWidth(), factory = { context -> Banner(context) })
}
