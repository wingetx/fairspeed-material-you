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

package com.fairspeed.test.domain.model

data class SpeedTestResult(
    val id: String = "",
    val downloadSpeed: Double = 0.0,
    val uploadSpeed: Double = 0.0,
    val ping: Double = 0.0,
    val jitter: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val server: Server = Server(),
    val isp: String = "Unknown",
    val connectionType: String = "Unknown",
    val signalInfo: SignalInfo? = null
)

enum class SignalType {
    WIFI,
    CELLULAR
}

/**
 * Signal strength captured at test time: bar level (0-4) and raw dBm when
 * available (WiFi RSSI; cellular RSRP/RSSI depending on the RAT).
 */
data class SignalInfo(
    val type: SignalType,
    val level: Int,
    val dbm: Int? = null
)

data class Server(
    val id: String = "",
    val name: String = "Default Server",
    val location: String = "Unknown",
    val country: String = "Unknown",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val distance: Double = 0.0,
    val url: String = ""
)

sealed class SpeedTestState {
    object Idle : SpeedTestState()
    object Loading : SpeedTestState()
    data class DownloadTesting(val progress: Float, val currentSpeed: Double, val server: Server) : SpeedTestState()
    data class UploadTesting(val progress: Float, val currentSpeed: Double, val server: Server) : SpeedTestState()
    data class PingTesting(val progress: Float, val currentPing: Double, val server: Server) : SpeedTestState()
    data class Success(val result: SpeedTestResult) : SpeedTestState()
    data class Error(val message: String) : SpeedTestState()
}

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

fun String.toFlagEmoji(): String {
    if (this.equals("auto", ignoreCase = true)) return "🌍"
    val code = if (this.equals("UK", ignoreCase = true)) "GB" else this.uppercase()
    if (code.length != 2) return ""
    return try {
        val firstLetter = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6
        val secondLetter = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6
        String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
    } catch (e: Exception) { "" }
}

val Server.flagEmoji: String
    get() = country.toFlagEmoji()
