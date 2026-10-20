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

import com.fairspeed.test.domain.model.Result
import com.fairspeed.test.domain.model.Server
import com.fairspeed.test.domain.model.SpeedTestResult
import kotlinx.coroutines.flow.Flow

import com.fairspeed.test.domain.model.SpeedTestState

interface ISpeedTestRepository {
    suspend fun startSpeedTest(server: Server, testMode: String = "both", disableTimeLimit: Boolean = false): Flow<SpeedTestState>
    suspend fun getTestHistory(): Result<List<SpeedTestResult>>
    suspend fun getServers(): Result<List<Server>>
    suspend fun selectServer(serverId: String): Result<Server>
    suspend fun clearHistory(): Result<Unit>
    suspend fun saveTestHistory(historyList: List<SpeedTestResult>): Result<Unit>
    suspend fun getUserIp(): Result<String>
}
