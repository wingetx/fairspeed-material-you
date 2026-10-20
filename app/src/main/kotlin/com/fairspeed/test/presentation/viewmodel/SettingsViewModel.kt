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

package com.fairspeed.test.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fairspeed.test.domain.model.AppTheme
import com.fairspeed.test.domain.model.AccentMode
import com.fairspeed.test.domain.model.Server
import com.fairspeed.test.domain.model.Result
import com.fairspeed.test.domain.repository.ISpeedTestRepository
import com.fairspeed.test.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val speedTestRepository: ISpeedTestRepository
) : ViewModel() {

    val appTheme: StateFlow<AppTheme> = settingsRepository.appTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.SYSTEM
        )

    val accentMode: StateFlow<AccentMode> = settingsRepository.accentMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AccentMode.MATERIAL_YOU
        )

    val customAccentHex: StateFlow<String> = settingsRepository.customAccentHex
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "367C89"
        )

    val defaultServerId: StateFlow<String?> = settingsRepository.defaultServerId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _servers = MutableStateFlow<List<Server>>(emptyList())
    val servers: StateFlow<List<Server>> = _servers

    init {
        loadServers()
    }

    private fun loadServers() {
        viewModelScope.launch {
            when (val result = speedTestRepository.getServers()) {
                is Result.Success -> {
                    // Sort alphabetically by country
                    val sortedList = result.data.sortedBy { it.country }
                    _servers.value = sortedList
                }
                is Result.Error -> {
                    // Handle error if needed
                }
                is Result.Loading -> {
                    // Do nothing
                }
            }
        }
    }

    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsRepository.setAppTheme(theme)
        }
    }
    
    fun setAccent(mode: AccentMode, customHex: String) {
        viewModelScope.launch {
            settingsRepository.setAccent(mode, customHex)
        }
    }

    fun setDefaultServerId(serverId: String?) {
        viewModelScope.launch {
            settingsRepository.setDefaultServerId(serverId)
        }
    }
}