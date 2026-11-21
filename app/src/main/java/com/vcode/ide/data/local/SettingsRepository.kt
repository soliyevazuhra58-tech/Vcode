package com.vcode.ide.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vcode.ide.domain.model.AppSettings
import com.vcode.ide.domain.model.EditorFontFamily
import com.vcode.ide.domain.model.EditorThemeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vcode_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val THEME_ID = stringPreferencesKey("theme_id")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val FONT_SIZE = intPreferencesKey("font_size")
        val TAB_SIZE = intPreferencesKey("tab_size")
        val WORD_WRAP = booleanPreferencesKey("word_wrap")
        val SHOW_LINE_NUMBERS = booleanPreferencesKey("show_line_numbers")
        val SHOW_MINIMAP = booleanPreferencesKey("show_minimap")
        val CODE_FOLDING = booleanPreferencesKey("code_folding")
        val AUTO_SAVE = booleanPreferencesKey("auto_save")
        val AUTO_INDENT = booleanPreferencesKey("auto_indent")
        val BRACKET_MATCHING = booleanPreferencesKey("bracket_matching")
        val EXTRA_KEY_BAR = booleanPreferencesKey("extra_key_bar")
        val ACCENT_COLOR = longPreferencesKey("accent_color")
        val ENABLED_EXTENSIONS = stringSetPreferencesKey("enabled_extensions")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val default = AppSettings()
        AppSettings(
            themeId = prefs[Keys.THEME_ID]?.let { EditorThemeId.fromId(it) } ?: default.themeId,
            fontFamily = prefs[Keys.FONT_FAMILY]?.let { EditorFontFamily.fromId(it) } ?: default.fontFamily,
            fontSizeSp = prefs[Keys.FONT_SIZE] ?: default.fontSizeSp,
            tabSize = prefs[Keys.TAB_SIZE] ?: default.tabSize,
            wordWrap = prefs[Keys.WORD_WRAP] ?: default.wordWrap,
            showLineNumbers = prefs[Keys.SHOW_LINE_NUMBERS] ?: default.showLineNumbers,
            showMinimap = prefs[Keys.SHOW_MINIMAP] ?: default.showMinimap,
            codeFolding = prefs[Keys.CODE_FOLDING] ?: default.codeFolding,
            autoSave = prefs[Keys.AUTO_SAVE] ?: default.autoSave,
            autoIndent = prefs[Keys.AUTO_INDENT] ?: default.autoIndent,
            bracketMatching = prefs[Keys.BRACKET_MATCHING] ?: default.bracketMatching,
            showExtraKeyBar = prefs[Keys.EXTRA_KEY_BAR] ?: default.showExtraKeyBar,
            accentColorHex = prefs[Keys.ACCENT_COLOR] ?: default.accentColorHex,
            enabledExtensions = prefs[Keys.ENABLED_EXTENSIONS] ?: default.enabledExtensions
        )
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val current = AppSettings(
                themeId = prefs[Keys.THEME_ID]?.let { EditorThemeId.fromId(it) } ?: EditorThemeId.DARK_PLUS,
                fontFamily = prefs[Keys.FONT_FAMILY]?.let { EditorFontFamily.fromId(it) } ?: EditorFontFamily.MONOSPACE,
                fontSizeSp = prefs[Keys.FONT_SIZE] ?: 14,
                tabSize = prefs[Keys.TAB_SIZE] ?: 2,
                wordWrap = prefs[Keys.WORD_WRAP] ?: false,
                showLineNumbers = prefs[Keys.SHOW_LINE_NUMBERS] ?: true,
                showMinimap = prefs[Keys.SHOW_MINIMAP] ?: true,
                codeFolding = prefs[Keys.CODE_FOLDING] ?: true,
                autoSave = prefs[Keys.AUTO_SAVE] ?: true,
                autoIndent = prefs[Keys.AUTO_INDENT] ?: true,
                bracketMatching = prefs[Keys.BRACKET_MATCHING] ?: true,
                showExtraKeyBar = prefs[Keys.EXTRA_KEY_BAR] ?: true,
                accentColorHex = prefs[Keys.ACCENT_COLOR] ?: 0xFF007ACC,
                enabledExtensions = prefs[Keys.ENABLED_EXTENSIONS] ?: AppSettings().enabledExtensions
            )
            val updated = transform(current)
            prefs[Keys.THEME_ID] = updated.themeId.id
            prefs[Keys.FONT_FAMILY] = updated.fontFamily.id
            prefs[Keys.FONT_SIZE] = updated.fontSizeSp
            prefs[Keys.TAB_SIZE] = updated.tabSize
            prefs[Keys.WORD_WRAP] = updated.wordWrap
            prefs[Keys.SHOW_LINE_NUMBERS] = updated.showLineNumbers
            prefs[Keys.SHOW_MINIMAP] = updated.showMinimap
            prefs[Keys.CODE_FOLDING] = updated.codeFolding
            prefs[Keys.AUTO_SAVE] = updated.autoSave
            prefs[Keys.AUTO_INDENT] = updated.autoIndent
            prefs[Keys.BRACKET_MATCHING] = updated.bracketMatching
            prefs[Keys.EXTRA_KEY_BAR] = updated.showExtraKeyBar
            prefs[Keys.ACCENT_COLOR] = updated.accentColorHex
            prefs[Keys.ENABLED_EXTENSIONS] = updated.enabledExtensions
        }
    }
}
