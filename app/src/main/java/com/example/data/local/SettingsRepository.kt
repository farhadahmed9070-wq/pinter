package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppLanguage
import com.example.data.model.PaperWidth
import com.example.data.model.PrintDensity
import com.example.data.model.PrinterEncoding
import com.example.data.model.PrinterSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.printerSettingsDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "printer_settings")

class SettingsRepository(private val context: Context) {
  private object Keys {
    val PAPER_WIDTH_MM = intPreferencesKey("paper_width_mm")
    val PRINT_DENSITY = intPreferencesKey("print_density")
    val AUTO_CUT = booleanPreferencesKey("auto_cut")
    val CASH_DRAWER = booleanPreferencesKey("cash_drawer")
    val FEED_LINES = intPreferencesKey("feed_lines")
    val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
    val ENCODING = stringPreferencesKey("encoding")
    val LANGUAGE = stringPreferencesKey("language")
    val LAST_PRINTER_NAME = stringPreferencesKey("last_printer_name")
    val LAST_PRINTER_ADDRESS = stringPreferencesKey("last_printer_address")
  }

  val settingsFlow: Flow<PrinterSettings> =
    context.printerSettingsDataStore.data.map { prefs ->
      PrinterSettings(
        paperWidth = PaperWidth.fromMm(prefs[Keys.PAPER_WIDTH_MM] ?: PaperWidth.MM_58.mm),
        printDensity = PrintDensity.fromLevel(prefs[Keys.PRINT_DENSITY] ?: PrintDensity.MEDIUM.level),
        autoCut = prefs[Keys.AUTO_CUT] ?: true,
        cashDrawer = prefs[Keys.CASH_DRAWER] ?: false,
        feedLines = (prefs[Keys.FEED_LINES] ?: 3).coerceIn(0, 8),
        autoReconnect = prefs[Keys.AUTO_RECONNECT] ?: false,
        encoding = PrinterEncoding.fromName(prefs[Keys.ENCODING] ?: PrinterEncoding.UTF_8.name),
        language = AppLanguage.fromCode(prefs[Keys.LANGUAGE] ?: AppLanguage.ENGLISH.code),
        lastPrinterName = prefs[Keys.LAST_PRINTER_NAME] ?: "",
        lastPrinterAddress = prefs[Keys.LAST_PRINTER_ADDRESS] ?: "",
      )
    }

  suspend fun updatePaperWidth(paperWidth: PaperWidth) {
    context.printerSettingsDataStore.edit { it[Keys.PAPER_WIDTH_MM] = paperWidth.mm }
  }

  suspend fun updatePrintDensity(density: PrintDensity) {
    context.printerSettingsDataStore.edit { it[Keys.PRINT_DENSITY] = density.level }
  }

  suspend fun updateAutoCut(enabled: Boolean) {
    context.printerSettingsDataStore.edit { it[Keys.AUTO_CUT] = enabled }
  }

  suspend fun updateCashDrawer(enabled: Boolean) {
    context.printerSettingsDataStore.edit { it[Keys.CASH_DRAWER] = enabled }
  }

  suspend fun updateFeedLines(lines: Int) {
    context.printerSettingsDataStore.edit { it[Keys.FEED_LINES] = lines.coerceIn(0, 8) }
  }

  suspend fun updateAutoReconnect(enabled: Boolean) {
    context.printerSettingsDataStore.edit { it[Keys.AUTO_RECONNECT] = enabled }
  }

  suspend fun updateEncoding(encoding: PrinterEncoding) {
    context.printerSettingsDataStore.edit { it[Keys.ENCODING] = encoding.name }
  }

  suspend fun updateLanguage(language: AppLanguage) {
    context.printerSettingsDataStore.edit { it[Keys.LANGUAGE] = language.code }
  }

  suspend fun saveLastConnectedPrinter(name: String, address: String) {
    context.printerSettingsDataStore.edit {
      it[Keys.LAST_PRINTER_NAME] = name
      it[Keys.LAST_PRINTER_ADDRESS] = address
    }
  }
}
