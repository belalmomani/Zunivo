package com.zunivo.app

import android.app.Application
import com.zunivo.app.ads.AdsConfig

class ZunivoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AdsConfig.init(this)
    }
}
