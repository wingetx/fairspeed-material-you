/*
 * This file is part of FairSpeed.
 *
 * FairSpeed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FairSpeed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with FairSpeed.  If not, see <https://www.gnu.org/licenses/>.
 */

// Modified 2026-10-20: persistent accent selection for the FairSpeed Material You fork.

package com.fairspeed.test.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fairspeed.test.domain.model.AppTheme
import com.fairspeed.test.domain.model.AccentMode
import com.fairspeed.test.domain.model.normalizeAccentHex
import com.fairspeed.test.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object PreferencesKeys {
        val APP_THEME = stringPreferencesKey("app_theme")
        val ACCENT_MODE = stringPreferencesKey("accent_mode")
        val CUSTOM_ACCENT_HEX = stringPreferencesKey("custom_accent_hex")
        val DEFAULT_SERVER_ID = stringPreferencesKey("default_server_id")
        val TEST_MODE = stringPreferencesKey("test_mode")
        val DISABLE_TIME_LIMIT = stringPreferencesKey("disable_time_limit")
    }

    override val appTheme: Flow<AppTheme> = context.dataStore.data.map { preferences ->
        val themeString = preferences[PreferencesKeys.APP_THEME] ?: AppTheme.SYSTEM.name
        try {
            AppTheme.valueOf(themeString)
        } catch (e: IllegalArgumentException) {
            AppTheme.SYSTEM
        }
    }

    override suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_THEME] = theme.name
        }
    }

    override val accentMode: Flow<AccentMode> = context.dataStore.data.map { preferences ->
        val stored = preferences[PreferencesKeys.ACCENT_MODE] ?: AccentMode.MATERIAL_YOU.name
        AccentMode.values().firstOrNull { it.name == stored } ?: AccentMode.MATERIAL_YOU
    }

    override val customAccentHex: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CUSTOM_ACCENT_HEX] ?: "367C89"
    }

    override suspend fun setAccent(mode: AccentMode, customHex: String) {
        val normalized = normalizeAccentHex(customHex)
            ?: throw IllegalArgumentException("Invalid RGB hex color")
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACCENT_MODE] = mode.name
            preferences[PreferencesKeys.CUSTOM_ACCENT_HEX] = normalized
        }
    }

    override val defaultServerId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DEFAULT_SERVER_ID]
    }

    override suspend fun setDefaultServerId(serverId: String?) {
        context.dataStore.edit { preferences ->
            if (serverId == null) {
                preferences.remove(PreferencesKeys.DEFAULT_SERVER_ID)
            } else {
                preferences[PreferencesKeys.DEFAULT_SERVER_ID] = serverId
            }
        }
    }

    // Advanced speed test options
    override val testMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TEST_MODE] ?: "both" // Default: both download and upload
    }

    override suspend fun setTestMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TEST_MODE] = mode
        }
    }

    override val disableTimeLimit: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DISABLE_TIME_LIMIT]?.toBoolean() ?: false // Default: false (time limit enabled)
    }

    override suspend fun setDisableTimeLimit(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DISABLE_TIME_LIMIT] = enabled.toString()
        }
    }
}