package com.voria.kenaret.settings

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * In-app language (Persian by default, English as the second language).
 * Stored in a tiny SharedPreferences file because it must be read synchronously
 * before the first frame (attachBaseContext). No health data is stored here.
 */
object LocaleManager {
    const val PERSIAN = "fa"
    const val ENGLISH = "en"

    private const val PREFS = "kenaret_locale"
    private const val KEY_LANGUAGE = "language"

    fun language(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, PERSIAN) ?: PERSIAN

    fun setLanguage(context: Context, language: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANGUAGE, language).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    /** Returns a context whose resources, layout direction and digits follow the chosen language. */
    fun wrap(base: Context): Context {
        val locale = Locale(language(base))
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
