package com.floromilano.app

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object AppLanguage {
    fun preference(context: Context): String {
        if (Build.VERSION.SDK_INT >= 33) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            return if (locales.isEmpty) "system"
            else locales[0].language.takeIf { it == "es" || it == "it" } ?: "system"
        }
        return context.getSharedPreferences("appearance", 0).getString("language", "system")
            ?: "system"
    }

    fun locale(context: Context): Locale {
        val selected = preference(context)
        val code =
            if (selected == "system") Resources.getSystem().configuration.locales[0].language
            else selected
        return Locale.forLanguageTag(if (code == "es") "es" else "it")
    }

    fun wrap(context: Context): Context {
        val locale = locale(context)
        Locale.setDefault(locale)
        return context.createConfigurationContext(
            Configuration(context.resources.configuration).apply { setLocales(LocaleList(locale)) }
        )
    }

    fun select(context: Context, language: String) {
        require(language in listOf("system", "es", "it"))
        context.getSharedPreferences("appearance", 0).edit().putString("language", language).apply()
        if (Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                if (language == "system") LocaleList.getEmptyLocaleList()
                else LocaleList.forLanguageTags(language)
        }
    }

    fun text(context: Context, id: Int, vararg args: Any): String =
        wrap(context).getString(id, *args)
}
