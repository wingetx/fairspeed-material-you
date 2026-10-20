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
import retrofit2.http.Streaming
import okhttp3.ResponseBody

import retrofit2.http.Url

/**
 * LibreSpeed API Interface
 * Compatible with open-source LibreSpeed server
 * 
 * API Documentation: https://github.com/librespeed/speedtest
 */
interface LibreSpeedApi {
    
    /**
     * Get list of available test servers
     */
    @GET
    suspend fun getServers(@Url url: String): List<ServerDto>

    /**
     * Get configuration from server
     * Returns test parameters and server info
     */
    @GET
    suspend fun getConfig(@Url url: String): ConfigDto

    /**
     * Get nearest server based on geolocation
     */
    @GET
    suspend fun getNearestServer(
        @Url url: String,
        @Query("nearest") nearest: Boolean = true
    ): List<ServerDto>

    /**
     * Download test - Stream data for specified duration
     * Returns raw response body for speed measurement
     */
    @Streaming
    @GET
    suspend fun testDownloadSpeed(
        @Url url: String,
        @Query("ckSize") sizeMb: Int = 100
    ): ResponseBody

    /**
     * Upload test - POST data for speed measurement
     * Returns response confirming upload
     */
    @POST
    suspend fun testUploadSpeed(
        @Url url: String,
        @retrofit2.http.Body body: okhttp3.RequestBody
    ): retrofit2.Response<Void>

    /**
     * Ping test - Measure latency
     * Returns timing information
     */
    @GET
    suspend fun testPing(@Url url: String): retrofit2.Response<Void>

    /**
     * Get results from server
     */
    @GET
    suspend fun getResults(
        @Url url: String,
        @Query("resultid") resultId: String
    ): ResultDto

    /**
     * Get IP address and ISP info from server
     */
    @GET
    suspend fun getIpInfo(@Url url: String): IpInfoDto

    /**
     * Submit test result to server
     */
    @POST
    suspend fun submitResult(
        @Url url: String,
        @Query("resultid") resultId: String,
        @Query("ping") ping: Double,
        @Query("download") download: Double,
        @Query("upload") upload: Double,
        @Query("ip") ip: String = "0.0.0.0"
    ): SubmitResultDto

    data class ConfigDto(
        val version: String = "5.2",
        val serverCount: Int = 1,
        val telemetryLevel: Int = 0,
        val enableTelemetry: Boolean = false,
        val enableFeedback: Boolean = false
    )

    data class PingResponseDto(
        val ping: Double = 0.0
    )

    data class ResultDto(
        val resultid: String = "",
        val testDate: String = "",
        val ping: Double = 0.0,
        val download: Double = 0.0,
        val upload: Double = 0.0,
        val packetLoss: Int = 0,
        val ispdl: String = "",
        val ispul: String = "",
        val rating: Int = 0,
        val ratingComment: String = ""
    )

    data class SubmitResultDto(
        val resultid: String = "",
        val url: String = ""
    )

    data class IpInfoDto(
        val processedString: String = "",
        val rawIspInfo: String = ""
    )
}
