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

package com.fairspeed.test.domain.repository

import com.fairspeed.test.domain.model.AppTheme
import com.fairspeed.test.domain.model.AccentMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val appTheme: Flow<AppTheme>
    suspend fun setAppTheme(theme: AppTheme)
    
    val accentMode: Flow<AccentMode>
    val customAccentHex: Flow<String>
    suspend fun setAccent(mode: AccentMode, customHex: String)

    val defaultServerId: Flow<String?>
    suspend fun setDefaultServerId(serverId: String?)
    
    // Advanced speed test options
    val testMode: Flow<String>
    suspend fun setTestMode(mode: String)
    
    val disableTimeLimit: Flow<Boolean>
    suspend fun setDisableTimeLimit(enabled: Boolean)
}