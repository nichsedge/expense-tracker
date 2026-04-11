package com.sans.expensetracker.data.util

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocaleManager @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun setLocale(language: String) {
        prefs.edit().putString("language", language).apply()
        updateResources(language)
    }

    fun getLocale(): String {
        return prefs.getString("language", "en") ?: "en"
    }

    fun updateResources(language: String) {
        val locale = Locale.of(language)
        Locale.setDefault(locale)
        val localeManager = context.getSystemService(android.app.LocaleManager::class.java)
        localeManager.applicationLocales = android.os.LocaleList(locale)
    }
}
