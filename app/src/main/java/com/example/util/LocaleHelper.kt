package com.example.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import android.provider.Settings
import com.example.data.model.AppLanguage
import java.util.Locale

object LocaleHelper {
  fun wrapContext(base: Context, language: AppLanguage): Context {
    val locale = Locale.forLanguageTag(language.code)
    Locale.setDefault(locale)
    val config = Configuration(base.resources.configuration)
    config.setLocale(locale)
    config.setLayoutDirection(locale)
    val localizedContext = base.createConfigurationContext(config)
    return object : ContextWrapper(base) {
      override fun getResources(): Resources = localizedContext.resources

      override fun getAssets(): AssetManager = localizedContext.assets

      override fun createConfigurationContext(overrideConfiguration: Configuration): Context =
        localizedContext.createConfigurationContext(overrideConfiguration)
    }
  }

  tailrec fun Context.findActivity(): Activity? =
    when (this) {
      is Activity -> this
      is ContextWrapper -> baseContext.findActivity()
      else -> null
    }

  fun openBluetoothSettings(context: Context): Boolean {
    val activity = context.findActivity()
    val launchContext = activity ?: context
    val btIntent =
      Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
        if (activity == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    val opened = runCatching { launchContext.startActivity(btIntent) }.isSuccess
    if (opened) return true

    val fallbackIntent =
      Intent(Settings.ACTION_SETTINGS).apply {
        if (activity == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    return runCatching { launchContext.startActivity(fallbackIntent) }.isSuccess
  }

  fun openAppSettings(context: Context): Boolean {
    val activity = context.findActivity()
    val launchContext = activity ?: context
    val appIntent =
      Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
      ).apply {
        if (activity == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    val opened = runCatching { launchContext.startActivity(appIntent) }.isSuccess
    if (opened) return true
    return openBluetoothSettings(context)
  }
}
