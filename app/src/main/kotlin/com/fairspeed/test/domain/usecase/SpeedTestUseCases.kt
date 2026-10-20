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

package com.fairspeed.test.domain.usecase

import com.fairspeed.test.domain.model.Result
import com.fairspeed.test.domain.model.SpeedTestResult
import com.fairspeed.test.domain.model.Server
import com.fairspeed.test.domain.repository.ISpeedTestRepository
import kotlinx.coroutines.flow.Flow

import com.fairspeed.test.domain.model.SpeedTestState

class StartSpeedTestUseCase(
    private val repository: ISpeedTestRepository
) {
    suspend operator fun invoke(server: Server, testMode: String = "both", disableTimeLimit: Boolean = false): Flow<SpeedTestState> {
        return repository.startSpeedTest(server, testMode, disableTimeLimit)
    }
}

class GetTestHistoryUseCase(
    private val repository: ISpeedTestRepository
) {
    suspend operator fun invoke(): Result<List<SpeedTestResult>> {
        return repository.getTestHistory()
    }
}

class GetServersUseCase(
    private val repository: ISpeedTestRepository
) {
    suspend operator fun invoke(): Result<List<Server>> {
        return repository.getServers()
    }
}

class SelectServerUseCase(
    private val repository: ISpeedTestRepository
) {
    suspend operator fun invoke(serverId: String): Result<Server> {
        return repository.selectServer(serverId)
    }
}

class ClearHistoryUseCase(
    private val repository: ISpeedTestRepository
) {
    suspend operator fun invoke() {
        repository.clearHistory()
    }
}
