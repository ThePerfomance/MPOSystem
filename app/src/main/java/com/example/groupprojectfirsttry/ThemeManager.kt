package com.example.groupprojectfirsttry

import android.content.Context
import android.content.SharedPreferences

object ThemeManager {

    private const val PREFS_NAME = "AppSettings"
    private const val KEY_THEME  = "selected_theme"
    private const val KEY_FONT   = "selected_font_size"
    private const val KEY_TRAINER_ENABLED = "trainer_enabled"
    private const val KEY_ADAPTIVE_TRAINER = "adaptive_trainer_enabled"
    private const val KEY_TRAINER_ONLY_PASSED = "trainer_only_passed"
    private const val KEY_TRAINER_EXCLUDE_CORRECT = "trainer_exclude_correct"

    private val themes = listOf(
        R.style.Theme_Emerald,
        R.style.Theme_Ruby,
        R.style.Theme_Amber,
        R.style.Theme_Sapphire,
        R.style.Theme_Topaz,
        R.style.Theme_Amethyst,
        R.style.Theme_Diamond
    )

    val fontSizes = listOf(20f, 16f, 12f)

    // ─── Проверка разрешения ──────────────────────────────────────────────────

    // Можно ли менять тему — берём из BuildConfig
    val canChangeTheme: Boolean get() = BuildConfig.CAN_CHANGE_THEME

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSavedThemeIndex(context: Context): Int =
        prefs(context).getInt(KEY_THEME, 0)

    fun getSavedFontSizeIndex(context: Context): Int =
        prefs(context).getInt(KEY_FONT, 1)

    fun isTrainerEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TRAINER_ENABLED, false)

    fun isAdaptiveTrainerEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ADAPTIVE_TRAINER, false)

    fun isTrainerOnlyPassed(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TRAINER_ONLY_PASSED, true)

    fun isTrainerExcludeCorrect(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TRAINER_EXCLUDE_CORRECT, true)

    fun saveTheme(context: Context, index: Int) {
        // Сохраняем только если разрешено
        if (!canChangeTheme) return
        prefs(context).edit().putInt(KEY_THEME, index).apply()
    }

    fun saveFontSize(context: Context, index: Int) =
        prefs(context).edit().putInt(KEY_FONT, index).apply()

    fun setTrainerEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_TRAINER_ENABLED, enabled).apply()

    fun setAdaptiveTrainerEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_ADAPTIVE_TRAINER, enabled).apply()

    fun setTrainerOnlyPassed(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_TRAINER_ONLY_PASSED, enabled).apply()

    fun setTrainerExcludeCorrect(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_TRAINER_EXCLUDE_CORRECT, enabled).apply()

    fun getThemeResId(index: Int): Int = themes[index]

    fun getSavedFontSize(context: Context): Float =
        fontSizes[getSavedFontSizeIndex(context)]

    fun applyTheme(context: Context) {
        // Если нельзя менять — всегда применяем тему 0 (дефолтную)
        val themeIndex = if (canChangeTheme) getSavedThemeIndex(context) else 0
        context.setTheme(getThemeResId(themeIndex))
    }
}