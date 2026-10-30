package com.zunivo.app.i18n

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** Persists the chosen language and applies it by wrapping the Activity base context. */
object LocaleManager {
    private const val PREFS = "zunivo_settings"
    private const val KEY_LANGUAGE = "language"

    fun saved(context: Context): AppLanguage {
        val tag = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LANGUAGE, null)
        return AppLanguage.fromTag(tag)
    }

    fun save(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANGUAGE, language.tag).apply()
    }

    fun wrap(base: Context): Context {
        val tag = saved(base).tag ?: return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
