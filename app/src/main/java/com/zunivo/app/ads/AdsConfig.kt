package com.zunivo.app.ads

import com.startapp.sdk.adsbase.StartAppSDK
import com.zunivo.app.BuildConfig

/**
 * Single switch for ads. Ads are active only when a Start.io App ID was compiled in:
 * debug builds use Start.io's demo ID (test ads), release builds need -PSTARTIO_APP_ID.
 * When disabled, no Start.io class is touched at runtime.
 */
object AdsConfig {
    val enabled: Boolean get() = BuildConfig.ADS_ENABLED
    val testMode: Boolean get() = BuildConfig.ADS_TEST

    fun prepare() {
        if (enabled && testMode) StartAppSDK.setTestAdsEnabled(true)
    }
}
