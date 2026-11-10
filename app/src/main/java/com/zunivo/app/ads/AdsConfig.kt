package com.zunivo.app.ads

import android.app.Application
import com.startapp.sdk.adsbase.StartAppSDK
import com.zunivo.app.BuildConfig

/**
 * Single switch for ads. Ads are active only when a Start.io App ID was compiled in:
 * debug builds use Start.io's public demo ID (test ads), release builds need -PSTARTIO_APP_ID.
 * When disabled, the SDK is never initialised and no ad code runs.
 */
object AdsConfig {
    /** True only inside an instrumentation test run (the test library is on the class path); ads stay off there. */
    private val underInstrumentation: Boolean by lazy {
        try { Class.forName("androidx.test.platform.app.InstrumentationRegistry"); true } catch (e: ClassNotFoundException) { false }
    }

    val enabled: Boolean get() = BuildConfig.ADS_ENABLED && BuildConfig.STARTIO_APP_ID.isNotEmpty() && !underInstrumentation
    val testMode: Boolean get() = BuildConfig.ADS_TEST

    /** Initialisation as documented in Start.io's Android demo project. "Return ads" (ads when coming back to the app) stay off. */
    fun init(app: Application) {
        if (!enabled) return
        StartAppSDK.initParams(app, BuildConfig.STARTIO_APP_ID)
            .setReturnAdsEnabled(false)
            .init()
        if (testMode) StartAppSDK.setTestAdsEnabled(true)
    }
}
