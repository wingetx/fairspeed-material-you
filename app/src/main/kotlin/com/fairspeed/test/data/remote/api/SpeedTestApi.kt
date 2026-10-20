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

package com.fairspeed.test.data.remote.api

import com.fairspeed.test.data.remote.dto.ServerDto
import com.fairspeed.test.data.remote.dto.SpeedTestResultDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SpeedTestApi {
    @GET("/api/servers")
    suspend fun getServers(): List<ServerDto>

    @GET("/api/servers/nearest")
    suspend fun getNearestServer(): ServerDto

    @POST("/api/speedtest/download")
    suspend fun testDownloadSpeed(
        @Query("duration") duration: Long = 10000
    ): SpeedTestResultDto

    @POST("/api/speedtest/upload")
    suspend fun testUploadSpeed(
        @Query("duration") duration: Long = 10000
    ): SpeedTestResultDto

    @POST("/api/speedtest/ping")
    suspend fun testPing(): SpeedTestResultDto

    @GET("/api/config")
    suspend fun getConfig(): ConfigDto

    data class ConfigDto(
        val version: String = "1.0.0",
        val downloadDuration: Long = 10000,
        val uploadDuration: Long = 10000,
        val testPointCount: Int = 4
    )
}
