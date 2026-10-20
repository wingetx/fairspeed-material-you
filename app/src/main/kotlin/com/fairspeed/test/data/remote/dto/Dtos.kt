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

package com.fairspeed.test.data.remote.dto

import com.fairspeed.test.domain.model.Server
import com.fairspeed.test.domain.model.SpeedTestResult
import com.google.gson.annotations.SerializedName

data class SpeedTestResultDto(
    @SerializedName("download_speed")
    val downloadSpeed: Double = 0.0,

    @SerializedName("upload_speed")
    val uploadSpeed: Double = 0.0,

    val ping: Double = 0.0,
    val jitter: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val server: ServerDto = ServerDto(),
    val isp: String = "Unknown",
    val id: String = ""
) {
    fun toDomain(): SpeedTestResult = SpeedTestResult(
        id = id,
        downloadSpeed = downloadSpeed,
        uploadSpeed = uploadSpeed,
        ping = ping,
        jitter = jitter,
        timestamp = timestamp,
        server = server.toDomain(),
        isp = isp
    )
}

data class ServerDto(
    val id: String = "",
    val name: String = "Default Server",
    val location: String = "Unknown",
    val country: String = "Unknown",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val distance: Double = 0.0,
    val url: String = ""
) {
    fun toDomain(): Server = Server(
        id = id,
        name = name,
        location = location,
        country = country,
        latitude = latitude,
        longitude = longitude,
        distance = distance,
        url = url
    )
}
