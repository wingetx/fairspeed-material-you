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

package com.fairspeed.test.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fairspeed.test.domain.model.Result
import com.fairspeed.test.domain.model.Server
import com.fairspeed.test.domain.model.SpeedTestResult
import com.fairspeed.test.domain.model.SpeedTestState
import com.fairspeed.test.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SpeedTestViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val startSpeedTestUseCase: StartSpeedTestUseCase,
    private val getTestHistoryUseCase: GetTestHistoryUseCase,
    private val getServersUseCase: GetServersUseCase,
    private val selectServerUseCase: SelectServerUseCase,
    private val clearHistoryUseCase: ClearHistoryUseCase,
    private val saveTestHistoryUseCase: SaveTestHistoryUseCase,
    private val getUserIpUseCase: GetUserIpUseCase,
    private val settingsRepository: com.fairspeed.test.domain.repository.SettingsRepository,
    private val connectionInfoProvider: com.fairspeed.test.data.system.ConnectionInfoProvider
) : ViewModel() {

    private val _speedTestState = MutableStateFlow<SpeedTestState>(SpeedTestState.Idle)
    val speedTestState: StateFlow<SpeedTestState> = _speedTestState.asStateFlow()

    private val _userIp = MutableStateFlow<String?>("Unknown / Offline")
    val userIp: StateFlow<String?> = _userIp.asStateFlow()

    private val _testHistory = MutableStateFlow<List<SpeedTestResult>>(emptyList())
    val testHistory: StateFlow<List<SpeedTestResult>> = _testHistory.asStateFlow()

    private val _servers = MutableStateFlow<List<Server>>(emptyList())
    val servers: StateFlow<List<Server>> = _servers.asStateFlow()

    private val _selectedServer = MutableStateFlow<Server?>(null)
    val selectedServer: StateFlow<Server?> = _selectedServer.asStateFlow()

    private val _testMode = MutableStateFlow("both")
    val testMode: StateFlow<String> = _testMode.asStateFlow()

    private val _disableTimeLimit = MutableStateFlow(false)
    val disableTimeLimit: StateFlow<Boolean> = _disableTimeLimit.asStateFlow()

    private val _connectionType = MutableStateFlow("")
    val connectionType: StateFlow<String> = _connectionType.asStateFlow()

    private val _showNetworkDetailsPrompt = MutableStateFlow(false)
    val showNetworkDetailsPrompt: StateFlow<Boolean> = _showNetworkDetailsPrompt.asStateFlow()

    private var _testJob: kotlinx.coroutines.Job? = null

    private var networkCallback: android.net.ConnectivityManager.NetworkCallback? = null

    init {
        loadServers()
        loadHistory()
        loadUserIp()
        loadSettings()
        refreshConnectionInfo()

        try {
            val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            networkCallback = object : android.net.ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    viewModelScope.launch {
                        refreshConnectionInfo()
                        kotlinx.coroutines.delay(2500) // Give OS time to settle routes
                        loadUserIp()
                    }
                }
                override fun onLost(network: android.net.Network) {
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(1000)
                        loadUserIp()
                        refreshConnectionInfo()
                    }
                }
            }
            networkCallback?.let { cm?.registerDefaultNetworkCallback(it) }
        } catch (e: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        try {
            val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            networkCallback?.let { cm?.unregisterNetworkCallback(it) }
        } catch (e: Exception) {}
    }
    
    fun loadHistory() {
        viewModelScope.launch {
            when (val result = getTestHistoryUseCase()) {
                is Result.Success -> {
                    _testHistory.value = result.data
                }
                else -> {}
            }
        }
    }

    fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.testMode.collect { mode ->
                _testMode.value = mode
            }
            settingsRepository.disableTimeLimit.collect { enabled ->
                _disableTimeLimit.value = enabled
            }
        }
    }

    fun loadUserIp() {
        viewModelScope.launch {
            when (val result = getUserIpUseCase()) {
                is Result.Success -> {
                    _userIp.value = result.data
                }
                is Result.Error -> {
                    _userIp.value = "Unknown / Offline"
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Refresh the detailed connection type (e.g. 5G, 4G+ (B3)) and the state of
     * the permission prompt. Called on start, on network changes and after a
     * permission request result.
     */
    fun refreshConnectionInfo() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            try { connectionInfoProvider.refresh() } catch (e: Exception) {}
            val type = try { connectionInfoProvider.getConnectionType() } catch (e: Exception) { "" }
            _connectionType.value = type
            _showNetworkDetailsPrompt.value = try {
                !connectionInfoProvider.hasDetailPermissions() && connectionInfoProvider.isCellular()
            } catch (e: Exception) { false }
        }
    }

    fun startSpeedTest() {
        if (_speedTestState.value is SpeedTestState.Loading) return
        val currentServer = _selectedServer.value ?: return

        loadUserIp()

        _testJob?.cancel()
        _testJob = viewModelScope.launch {
            try {
                _speedTestState.value = SpeedTestState.Loading
                val currentTestMode = _testMode.value
                val currentDisableTimeLimit = _disableTimeLimit.value
                startSpeedTestUseCase(currentServer, currentTestMode, currentDisableTimeLimit).collect { state ->
                    _speedTestState.value = state
                    if (state is SpeedTestState.Success) {
                        addToHistory(state.result)
                    }
                    if (state is SpeedTestState.Error) {
                        // The repository saved a partial result before emitting the error
                        loadHistory()
                    }
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    _speedTestState.value = SpeedTestState.Error(e.message ?: "An error occurred")
                    loadHistory()
                }
            }
        }
    }

    fun stopSpeedTest() {
        val job = _testJob
        _testJob = null
        _speedTestState.value = SpeedTestState.Idle
        if (job != null) {
            viewModelScope.launch {
                // Wait for the test job to finish saving its partial result
                try { job.cancelAndJoin() } catch (e: Exception) {}
                loadHistory()
            }
        }
    }

    private fun addToHistory(result: SpeedTestResult) {
        val newHistory = (listOf(result) + _testHistory.value).take(50) // Keep last 50 results
        _testHistory.value = newHistory
        viewModelScope.launch {
            saveTestHistoryUseCase(newHistory)
        }
    }

    fun loadServers() {
        viewModelScope.launch {
            when (val result = getServersUseCase()) {
                is Result.Success -> {
                    _servers.value = result.data
                    if (_selectedServer.value == null && result.data.isNotEmpty()) {
                        _selectedServer.value = result.data[0]
                    }
                }
                is Result.Error -> {
                    _speedTestState.value = SpeedTestState.Error(result.exception.message ?: "Failed to load servers")
                }
                is Result.Loading -> {}
            }
        }
    }

    fun selectServer(serverId: String) {
        viewModelScope.launch {
            when (val result = selectServerUseCase(serverId)) {
                is Result.Success -> {
                    _selectedServer.value = result.data
                }
                is Result.Error -> {
                    _speedTestState.value = SpeedTestState.Error(result.exception.message ?: "Failed to select server")
                }
                is Result.Loading -> {}
            }
        }
    }

    fun setTestMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.setTestMode(mode)
            _testMode.value = mode
        }
    }

    fun setDisableTimeLimit(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDisableTimeLimit(enabled)
            _disableTimeLimit.value = enabled
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            clearHistoryUseCase()
            _testHistory.value = emptyList()
        }
    }

    fun deleteHistoryItem(item: SpeedTestResult) {
        val newHistory = _testHistory.value.filter { it != item }
        _testHistory.value = newHistory
        viewModelScope.launch {
            saveTestHistoryUseCase(newHistory)
        }
    }

    fun exportHistory(uri: android.net.Uri, context: android.content.Context, onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val list = _testHistory.value
                val json = com.google.gson.Gson().toJson(list)
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(json.toByteArray())
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onSuccess(list.size)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onError(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun importHistory(uri: android.net.Uri, context: android.content.Context, onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val json = inputStream.bufferedReader().use { it.readText() }
                    val type = object : com.google.gson.reflect.TypeToken<List<com.fairspeed.test.domain.model.SpeedTestResult>>() {}.type
                    val importedList: List<com.fairspeed.test.domain.model.SpeedTestResult> = com.google.gson.Gson().fromJson(json, type)
                    
                    val existingTimestamps = _testHistory.value.map { it.timestamp }.toSet()
                    val newItems = importedList.filter { it.timestamp !in existingTimestamps }.distinctBy { it.timestamp }
                    
                    val combined = (newItems + _testHistory.value)
                        .sortedByDescending { it.timestamp }
                        .take(50)
                    
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        _testHistory.value = combined
                        onSuccess(newItems.size)
                    }
                    saveTestHistoryUseCase(combined)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onError(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun getLastResult(): SpeedTestResult? {
        return if (_speedTestState.value is SpeedTestState.Success) {
            (_speedTestState.value as SpeedTestState.Success).result
        } else {
            _testHistory.value.firstOrNull()
        }
    }
}
