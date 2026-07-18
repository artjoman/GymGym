package com.gymgym.app.settings

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * In-app language selection. Works on all supported API levels:
 *
 *  - API 33+ (per-app languages): delegates to the framework [LocaleManager], so
 *    the choice is unified with the system "App languages" screen, persists
 *    across restarts, and the framework recreates activities automatically.
 *  - API 26–32: stores a BCP-47 tag in [SharedPreferences] and applies it by
 *    wrapping each [Context] in [attach] (called from `attachBaseContext`); the
 *    caller recreates the activity so the new resources take effect.
 *
 * An empty tag means "follow the system language" (the default).
 */
object AppLocale {

    /** BCP-47 tags shown in the picker, matching the shipped `values-*` resource qualifiers. */
    val SUPPORTED = listOf("en", "ru", "es", "zh-CN", "fr", "lv", "ar")

    /** Autonym (language's own name) for each tag; deliberately not translated. */
    fun displayName(tag: String): String = when (tag) {
        "en" -> "English"
        "ru" -> "Русский"
        "es" -> "Español"
        "zh-CN" -> "中文"
        "fr" -> "Français"
        "lv" -> "Latviešu"
        "ar" -> "العربية"
        else -> tag
    }

    /** The current app language tag, or "" for system default. */
    fun currentTag(context: Context): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)
                ?.applicationLocales
                ?.takeUnless { it.isEmpty }
                ?.get(0)
                ?.toLanguageTag()
                ?: ""
        } else {
            prefs(context).getString(KEY_TAG, "").orEmpty()
        }

    /**
     * Persist [tag] ("" = system default). On API 33+ the framework applies and
     * recreates; on older versions the caller must recreate the activity after
     * this returns (see [needsManualRestart]).
     */
    fun setTag(context: Context, tag: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)?.applicationLocales =
                if (tag.isEmpty()) LocaleList.getEmptyLocaleList()
                else LocaleList.forLanguageTags(tag)
        } else {
            prefs(context).edit().putString(KEY_TAG, tag).apply()
            // Update the process/application resources immediately so strings
            // fetched off the application context (e.g. spoken cues) also switch.
            applyToResources(context.applicationContext, tag)
        }
    }

    /** True when the caller must call `Activity.recreate()` itself after [setTag]. */
    fun needsManualRestart(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU

    /**
     * Wrap [base] with the stored locale. Call from `attachBaseContext`. A no-op
     * on API 33+, where the framework has already localized the base context.
     */
    fun attach(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = prefs(base).getString(KEY_TAG, "").orEmpty()
        if (tag.isEmpty()) return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return base.createConfigurationContext(config)
    }

    private fun applyToResources(context: Context, tag: String) {
        val locale = if (tag.isEmpty()) Locale.getDefault() else Locale.forLanguageTag(tag)
        val res = context.resources
        val config = Configuration(res.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        @Suppress("DEPRECATION")
        res.updateConfiguration(config, res.displayMetrics)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private const val PREFS = "app_locale"
    private const val KEY_TAG = "lang_tag"
}
