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

package com.fairspeed.test.config



/**
 * Speed Test Configuration
 * Server and test parameters
 */
object SpeedTestConfig {
    
    // Server configuration
    // Change based on build variant or runtime
    val BASE_URL = BuildConfigExt.SPEED_TEST_URL
    
    // Test configuration
    const val DOWNLOAD_TEST_DURATION = 8000L   // 8 seconds
    const val UPLOAD_TEST_DURATION = 8000L     // 8 seconds
    const val PING_TEST_COUNT = 8              // 8 ping measurements for accuracy
    
    // File sizes
    const val MIN_DOWNLOAD_SIZE = 1024 * 1024      // 1 MB
    const val MAX_DOWNLOAD_SIZE = 100 * 1024 * 1024 // 100 MB
    const val MIN_UPLOAD_SIZE = 1024 * 1024        // 1 MB
    const val MAX_UPLOAD_SIZE = 100 * 1024 * 1024  // 100 MB
    
    // Network configuration
    const val CONNECT_TIMEOUT_SECONDS = 10L
    const val READ_TIMEOUT_SECONDS = 30L
    const val WRITE_TIMEOUT_SECONDS = 30L
    
    // Logging
    val ENABLE_HTTP_LOGGING = true // Hardcoded for now
    
    // Test configuration for accuracy
    const val TEST_POINT_COUNT = 4  // Multiple data points for averaging
    const val MIN_SERVER_DISTANCE_KM = 50
    const val MAX_SERVER_COUNT = 10
    
    // Measurement accuracy settings
    const val WARMUP_DATA_DISCARD_MS = 1000L  // Discard first 1 second of data
    const val MEASUREMENT_INTERVAL_MS = 100L  // Collect data every 100ms
    const val PING_WARMUP_COUNT = 1            // Skip first ping request
    const val BUFFER_SIZE = 65536              // 64KB read/write buffer
    
    // Result submission
    const val SUBMIT_RESULTS = true  // Send results to server
    const val KEEP_LOCAL_RESULTS = true  // Also keep locally
    
    object Endpoints {
        const val SERVERS = "api/servers"
        const val CONFIG = "api/config"
        const val PING = "speedtest/ping"
        const val DOWNLOAD = "speedtest/download"
        const val UPLOAD = "speedtest/upload"
        const val RESULTS_GET = "results/get"
        const val RESULTS_UPDATE = "results/update"
    }
}

/**
 * Build configuration extension
 * Provides runtime access to BuildConfig fields
 */
object BuildConfigExt {
    val SPEED_TEST_URL: String
        get() = when {
            true -> "https://librespeed.org/"  // Safe default
            else -> "https://librespeed.org/"      // Production Safe Default
        }
    
    val IS_DEBUG: Boolean
        get() = true
}
