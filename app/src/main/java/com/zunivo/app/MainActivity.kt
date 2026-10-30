package com.zunivo.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zunivo.app.i18n.LocaleManager
import com.zunivo.app.ui.ZunivoRoot
import com.zunivo.app.ui.theme.ZunivoTheme

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZunivoTheme {
                ZunivoRoot(
                    currentLanguage = LocaleManager.saved(this@MainActivity),
                    onLanguageChange = { language ->
                        LocaleManager.save(this@MainActivity, language)
                        recreate()
                    },
                )
            }
        }
    }
}
