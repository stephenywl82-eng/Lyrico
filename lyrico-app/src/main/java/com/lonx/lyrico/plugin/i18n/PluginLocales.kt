package com.lonx.lyrico.plugin.i18n

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.icu.util.ULocale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Application configuration honors both system and per-app language preferences. */
object PluginLocales {
    private val current = MutableStateFlow(listOf(java.util.Locale.getDefault().toLanguageTag()))
    val preferences = current.asStateFlow()
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        PluginStrings.inferScript = { ULocale.addLikelySubtags(ULocale.forLocale(it)).script }
        update(context.resources.configuration)
        context.applicationContext.registerComponentCallbacks(object : ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) = update(newConfig)
            override fun onLowMemory() = Unit
        })
    }

    fun update(configuration: Configuration) {
        current.value = (0 until configuration.locales.size()).map { configuration.locales[it].toLanguageTag() }
    }
}
