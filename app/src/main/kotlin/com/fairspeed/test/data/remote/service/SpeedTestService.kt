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

package com.fairspeed.test.data.remote.service

import com.fairspeed.test.data.remote.api.LibreSpeedApi
import com.fairspeed.test.data.system.ConnectionInfoProvider
import com.fairspeed.test.domain.model.SignalInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Service for executing network speed tests
 * Measures actual network performance by transferring data
 */
@Singleton
class SpeedTestService @Inject constructor(
    private val api: LibreSpeedApi,
    private val connectionInfoProvider: ConnectionInfoProvider
) {
    
    private val testDurationMs = 8000L // Default: 8 seconds per phase
    private val extendedTestDurationMs = 30000L // Extended: 30 seconds per phase

    /**
     * Data class to hold Ping and Jitter results
     */
    data class PingResult(val ping: Double, val jitter: Double)

    /**
     * Execute ping test multiple times with warmup skip and median calculation
     * Returns PingResult (median ping and jitter) for more accurate data (excludes outliers)
     */
    suspend fun measurePing(serverUrl: String, onProgress: suspend (Float, Double) -> Unit = { _, _ -> }): PingResult = withContext(Dispatchers.IO) {
        try {
            val pings = mutableListOf<Double>()
            val pingCount = 10
            
            // Warmup ping - discard it
            try {
                api.testPing("${serverUrl}empty.php")
                delay(20)
            } catch (e: Exception) {
                android.util.Log.e("SpeedTest", "Warmup Ping Error", e)
            }
            
            // Actual measurements
            repeat(pingCount) { testIndex ->
                try {
                    val startTime = System.currentTimeMillis()
                    api.testPing("${serverUrl}empty.php")
                    val endTime = System.currentTimeMillis()
                    val currentPing = (endTime - startTime).toDouble()
                    pings.add(currentPing)
                    
                    val progress = ((testIndex + 1) / pingCount.toFloat())
                    val displayValue = calculateMedian(pings)
                    onProgress(progress, displayValue)
                    delay(20) // Small delay to avoid flooding
                } catch (e: Exception) {
                    android.util.Log.e("SpeedTest", "Ping Error", e)
                }
            }
            
            if (pings.isEmpty()) throw Exception("All ping tests failed")
            
            val finalPing = calculateMedian(pings)
            
            // Calculate Jitter (average of absolute differences between consecutive pings)
            var totalDifference = 0.0
            var diffCount = 0
            for (i in 1 until pings.size) {
                totalDifference += kotlin.math.abs(pings[i] - pings[i-1])
                diffCount++
            }
            val finalJitter = if (diffCount > 0) totalDifference / diffCount else 0.0

            PingResult(ping = finalPing, jitter = finalJitter)
        } catch (e: Exception) {
            android.util.Log.e("SpeedTest", "Ping Error", e)
            throw testError("Ping", e)
        }
    }

    /**
     * Execute download speed test with enhanced accuracy
     * Uses nanosecond-precision timing for gigabit-speed measurements
     */
    suspend fun measureDownloadSpeed(
        serverUrl: String,
        disableTimeLimit: Boolean = false,
        onProgress: suspend (Float, Double) -> Unit = { _, _ -> }
    ): Double = withContext(Dispatchers.IO) {
        try {
            val startTimeNanos = System.nanoTime()
            var bytesDownloadedGlobal = 0L
            val speeds = mutableListOf<Double>()
            var lastEmitTimeNanos = System.nanoTime()
            
            try {
                // Request extremely large file (2GB) so it doesn't end prematurely on fast 1Gbps+ connections
                val response = api.testDownloadSpeed("${serverUrl}garbage.php", 2000)
                
                val inputStream = response.byteStream()
                val buffer = ByteArray(524288) // 512KB buffer for optimal gigabit throughput
                var read: Int = 0
                
                var lastBytesDownloadedGlobal = 0L
                var warmupPhaseEndNanos = startTimeNanos + 250_000_000L // Discard first 250ms for accurate stable-state measurement

                val effectiveDurationMs = if (disableTimeLimit) extendedTestDurationMs else testDurationMs
            while (System.nanoTime() - startTimeNanos < effectiveDurationMs * 1_000_000L && inputStream.read(buffer).also { read = it } != -1 && isActive) {
                    bytesDownloadedGlobal += read
                    
                    val nowNanos = System.nanoTime()
                    val deltaDurationNanos = nowNanos - lastEmitTimeNanos
                    val deltaDurationSeconds = deltaDurationNanos / 1_000_000_000.0
                    
                    // Emit progress with higher frequency: every 25ms for better sampling at gigabit speeds
                    if (deltaDurationNanos >= 25_000_000L) { 
                        val deltaBytes = bytesDownloadedGlobal - lastBytesDownloadedGlobal
                        val currentMbps = (deltaBytes * 8.0) / (1_000_000.0 * deltaDurationSeconds)
                        
                        // Only add to speeds list after warmup phase
                        if (nowNanos >= warmupPhaseEndNanos) {
                            speeds.add(currentMbps)
                        }
                        
                        val elapsedMs = (System.nanoTime() - startTimeNanos) / 1_000_000.0
                        val progress = (elapsedMs / effectiveDurationMs).toFloat()
                        onProgress(progress.coerceAtMost(1f), if (speeds.isNotEmpty()) calculateTopPercentile(speeds, 0.95) else currentMbps)
                        
                        lastEmitTimeNanos = nowNanos
                        lastBytesDownloadedGlobal = bytesDownloadedGlobal
                    }
                }
                inputStream.close()
            } catch (e: Exception) { 
                android.util.Log.e("SpeedTest", "Download Stream Error", e)
            }
            
            if (speeds.isEmpty()) throw Exception("Download test failed/timed out")
            // Use 95th percentile to get actual line capacity (standard for gigabit testing)
            calculateTopPercentile(speeds, 0.95)
            
        } catch (e: Exception) {
            android.util.Log.e("SpeedTest", "Download Error", e)
            throw testError("Download", e)
        }
    }

    /**
     * Execute upload speed test with enhanced accuracy
     * Uses nanosecond-precision timing for gigabit-speed measurements
     */
    suspend fun measureUploadSpeed(
        serverUrl: String,
        disableTimeLimit: Boolean = false,
        onProgress: suspend (Float, Double) -> Unit = { _, _ -> }
    ): Double = withContext(Dispatchers.IO) {
        val checkActive = { this.isActive }
        val uploadScope = this
        try {
            val startTimeNanos = System.nanoTime()
            var bytesUploadedGlobal = 0L
            val speeds = mutableListOf<Double>()
            var lastEmitTimeNanos = System.nanoTime()
            var warmupPhaseEndNanos = startTimeNanos + 200_000_000L // 200ms warmup for stable-state measurement

            // Safe stream upload without buffering the entire payload in memory
            val requestBody = object : RequestBody() {
                override fun contentType() = "application/octet-stream".toMediaTypeOrNull()
                // Return a large specific size instead of -1L to prevent 'Transfer-Encoding: chunked' overhead
                // which can severely limit throughput on gigabit connections.
                override fun contentLength(): Long = 1000L * 1024 * 1024 * 1024 // 1000 GB

                override fun writeTo(sink: BufferedSink) {
                    // Use a large random buffer to prevent network compression (which artificially inflates upload speed)
                    // by writing varying offsets of the buffer on each iteration.
                    // This avoids the CPU bottleneck of generating random bytes in the upload loop.
                    val randomData = ByteArray(4 * 1024 * 1024) // 4MB buffer
                    java.util.Random().nextBytes(randomData)
                    val chunkSize = 262144 // 256KB chunk for optimal throughput
                    var lastBytesUploadedGlobal = 0L

                    val effectiveDurationMs = if (disableTimeLimit) extendedTestDurationMs else testDurationMs
            while (System.nanoTime() - startTimeNanos < effectiveDurationMs * 1_000_000L && checkActive()) {
                        try {
                            val randomOffset = (System.nanoTime() % (randomData.size - chunkSize)).toInt()
                            sink.write(randomData, randomOffset, chunkSize)
                            // NO manual sink.flush() - let Okio handle optimal buffering to the socket.
                            // Manual flushes disrupt TCP window scaling and reduce throughput significantly.
                            
                            bytesUploadedGlobal += chunkSize

                            val nowNanos = System.nanoTime()
                            val deltaDurationNanos = nowNanos - lastEmitTimeNanos
                            val deltaDurationSeconds = deltaDurationNanos / 1_000_000_000.0

                            // Emit progress with higher frequency: every 25ms for better sampling at gigabit speeds
                            if (deltaDurationNanos >= 25_000_000L) {
                                val deltaBytes = bytesUploadedGlobal - lastBytesUploadedGlobal
                                val currentMbps = (deltaBytes * 8.0) / (1_000_000.0 * deltaDurationSeconds)
                                
                                // Only add to speeds list after warmup phase
                                if (nowNanos >= warmupPhaseEndNanos) {
                                    speeds.add(currentMbps)
                                }
                                
                                val elapsedMs = (System.nanoTime() - startTimeNanos) / 1_000_000.0
                                val progress = (elapsedMs / effectiveDurationMs).toFloat()
                                val finalSpeed = if (speeds.isNotEmpty()) calculateTopPercentile(speeds, 0.95) else currentMbps
                                
                                // Emit progress asynchronously without blocking the write thread
                                uploadScope.launch {
                                    try {
                                        onProgress(progress.coerceAtMost(1f), finalSpeed)
                                    } catch (e: Exception) {
                                        // Callback error should not interrupt upload
                                        android.util.Log.e("SpeedTest", "Progress callback error", e)
                                    }
                                }
                                
                                lastEmitTimeNanos = nowNanos
                                lastBytesUploadedGlobal = bytesUploadedGlobal
                            }
                        } catch (e: Exception) {
                            break // OkHttp might close stream
                        }
                    }
                    
                    if (!checkActive()) {
                        throw java.io.IOException("Test stopped by user")
                    }
                }
            }
            
            try {
                api.testUploadSpeed("${serverUrl}empty.php", requestBody)
            } catch (e: Exception) {
                // Expected to break/timeout after testDurationMs if strictly chunked
            }
            
            if (speeds.isEmpty()) throw Exception("Upload test failed")
            // Use 95th percentile to get actual line capacity (standard for gigabit testing)
            calculateTopPercentile(speeds, 0.95)
            
        } catch (e: Exception) {
            android.util.Log.e("SpeedTest", "Upload Error", e)
            throw testError("Upload", e)
        }
    }

    /**
     * Generate unique test result ID
     */
    fun generateResultId(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * Build a test error message avoiding redundant "X test failed: ... test failed"
     * chains when the inner exception already reports a failure.
     */
    private fun testError(phase: String, e: Exception): Exception {
        val msg = e.message?.trim().orEmpty()
        return when {
            msg.isEmpty() -> Exception("$phase test failed")
            msg.contains("failed", ignoreCase = true) -> Exception(msg)
            else -> Exception("$phase test failed: $msg")
        }
    }

    /**
     * Calculate median value for more accurate speed measurement
     * Filters out anomalies better than average
     */
    private fun calculateMedian(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        if (values.size == 1) return values[0]
        
        val sorted = values.sorted()
        return if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        } else {
            sorted[sorted.size / 2]
        }
    }

    /**
     * Get the top percentile value (e.g. 0.90 for 90th percentile)
     * Commercial speed testers use 90th or 95th percentile or max sliding windows
     * instead of median/average to report the physical line's capability 
     * without counting the ramp-up and TCP slow-start phases.
     */
    private fun calculateTopPercentile(values: List<Double>, percentile: Double): Double {
        if (values.isEmpty()) return 0.0
        if (values.size == 1) return values[0]
        
        val sorted = values.sorted()
        val index = (sorted.size * percentile).toInt().coerceIn(0, sorted.size - 1)
        return sorted[index]
    }

    /**
     * Filter outliers using IQR (Interquartile Range) method
     * Removes extreme values that don't represent typical performance
     */
    private fun filterOutliers(values: List<Double>): List<Double> {
        if (values.size < 4) return values
        
        val sorted = values.sorted()
        val q1Index = sorted.size / 4
        val q3Index = (sorted.size * 3) / 4
        val q1 = sorted[q1Index]
        val q3 = sorted[q3Index]
        val iqr = q3 - q1
        val lowerBound = q1 - (1.5 * iqr)
        val upperBound = q3 + (1.5 * iqr)
        
        return values.filter { it in lowerBound..upperBound }
    }

    /**
     * Get current connection type (detailed: 5G/4G, band, carrier aggregation when available)
     */
    fun getConnectionType(): String {
        return connectionInfoProvider.getConnectionType()
    }

    /**
     * Get current signal strength (WiFi RSSI or cellular level/dBm), null when unavailable
     */
    fun getSignalInfo(): SignalInfo? {
        return connectionInfoProvider.getSignalInfo()
    }
}
