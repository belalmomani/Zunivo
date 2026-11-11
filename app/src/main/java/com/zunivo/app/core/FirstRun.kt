package com.zunivo.app.core

import android.content.Context

/** Remembers that the welcome tour was shown. Lives in the same private settings file as the language choice. */
object FirstRun {
    private const val PREFS = "zunivo_settings"
    private const val KEY = "onboarding_done"

    fun isDone(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)
    fun markDone(context: Context) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, true).apply() }
    fun reset(context: Context) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply() }
}
