package com.example.util

import android.content.Context
import android.content.res.Configuration
import com.example.data.model.AppLanguage
import java.util.Locale

object LocaleHelper {
  fun wrapContext(base: Context, language: AppLanguage): Context {
    val locale = Locale(language.code)
    Locale.setDefault(locale)
    val config = Configuration(base.resources.configuration)
    config.setLocale(locale)
    config.setLayoutDirection(locale)
    return base.createConfigurationContext(config)
  }
}
