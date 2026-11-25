package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "tejascode_prefs")

data class AppPreferences(
    val darkTheme: Boolean = true,
    val languageCode: String = "en",
    val rememberMe: Boolean = true
)

class UserPreferencesManager(private val context: Context) {

    private val keyDarkTheme = booleanPreferencesKey("dark_theme")
    private val keyLanguageCode = stringPreferencesKey("app_language_code")
    private val keyRememberMe = booleanPreferencesKey("remember_me")

    val preferencesFlow: Flow<AppPreferences> = context.userPreferencesDataStore.data.map { prefs ->
        AppPreferences(
            darkTheme = prefs[keyDarkTheme] ?: true,
            languageCode = prefs[keyLanguageCode] ?: "en",
            rememberMe = prefs[keyRememberMe] ?: true
        )
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[keyDarkTheme] = enabled
        }
    }

    suspend fun setLanguageCode(code: String) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[keyLanguageCode] = code
        }
    }

    suspend fun setRememberMe(enabled: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[keyRememberMe] = enabled
        }
    }

    companion object {
        val supportedLanguages: List<Pair<String, String>> = listOf(
            "en" to "English",
            "hi" to "हिन्दी (Hindi)",
            "gu" to "ગુજરાતી (Gujarati)",
            "es" to "Español",
            "fr" to "Français",
            "de" to "Deutsch",
            "pt" to "Português",
            "ar" to "العربية",
            "ja" to "日本語",
            "zh" to "中文"
        )
    }
}
