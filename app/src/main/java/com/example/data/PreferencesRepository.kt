package com.example.data

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.i18n.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.demetraDataStore: DataStore<Preferences> by preferencesDataStore(name = "demetra_prefs")

@Immutable
data class UserSettings(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val reducedMotion: Boolean = false,
    val lowDataMode: Boolean = false
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val LANGUAGE = stringPreferencesKey("app_language")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val LOW_DATA_MODE = booleanPreferencesKey("low_data_mode")
    }

    val settingsFlow: Flow<UserSettings> = context.demetraDataStore.data.map { prefs ->
        val langCode = prefs[Keys.LANGUAGE] ?: AppLanguage.ENGLISH.code
        val language = AppLanguage.entries.find { it.code == langCode } ?: AppLanguage.ENGLISH
        UserSettings(
            language = language,
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
            lowDataMode = prefs[Keys.LOW_DATA_MODE] ?: false
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.demetraDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.demetraDataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
    }

    suspend fun setLowDataMode(enabled: Boolean) {
        context.demetraDataStore.edit { it[Keys.LOW_DATA_MODE] = enabled }
    }
}
