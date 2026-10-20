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

package com.fairspeed.test.domain.repository

import com.fairspeed.test.domain.model.AppTheme
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val appTheme: Flow<AppTheme>
    suspend fun setAppTheme(theme: AppTheme)
    
    val defaultServerId: Flow<String?>
    suspend fun setDefaultServerId(serverId: String?)
    
    // Advanced speed test options
    val testMode: Flow<String>
    suspend fun setTestMode(mode: String)
    
    val disableTimeLimit: Flow<Boolean>
    suspend fun setDisableTimeLimit(enabled: Boolean)
}