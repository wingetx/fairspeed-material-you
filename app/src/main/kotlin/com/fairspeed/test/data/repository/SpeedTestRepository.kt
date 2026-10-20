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

package com.fairspeed.test.data.repository

import com.fairspeed.test.data.remote.api.LibreSpeedApi
import com.fairspeed.test.data.remote.dto.ServerDto
import com.fairspeed.test.data.remote.service.SpeedTestService
import com.fairspeed.test.domain.model.Result
import com.fairspeed.test.domain.model.Server
import com.fairspeed.test.domain.model.SpeedTestResult
import com.fairspeed.test.domain.model.SpeedTestState
import com.fairspeed.test.domain.repository.ISpeedTestRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class SpeedTestRepository @Inject constructor(
    private val api: LibreSpeedApi,
    private val speedTestService: SpeedTestService,
    private val sharedPreferences: android.content.SharedPreferences
) : ISpeedTestRepository {

    override suspend fun startSpeedTest(server: Server, testMode: String, disableTimeLimit: Boolean): Flow<SpeedTestState> = channelFlow {
        var partialResult: SpeedTestResult? = null
        var ispName = "Unknown"
        var targetServer = server
        // Capture the connection type once at test start: if the network drops
        // during the test, results must keep the network the test started on
        // instead of reporting "Unknown"
        val connectionType = try { speedTestService.getConnectionType() } catch (e: Exception) { "Unknown" }
        // Capture the signal strength (WiFi/cellular) once at test start, same as connectionType
        val signalInfo = try { speedTestService.getSignalInfo() } catch (e: Exception) { null }
        
        try {
            send(SpeedTestState.Loading)

            if (targetServer.id == "auto") {
                val realServers = publicServers.filter { it.id != "auto" }
                targetServer = realServers.map { s ->
                    async(kotlinx.coroutines.Dispatchers.IO) {
                        val serverUrl = s.url.let { if (it.endsWith("/")) it else "$it/" }
                        val ping = try {
                            var minP = Double.MAX_VALUE
                            try { api.testPing("${serverUrl}empty.php") } catch(e: Exception) {}
                            
                            for (i in 0..2) {
                                val t0 = System.currentTimeMillis()
                                val response = api.testPing("${serverUrl}empty.php")
                                if (response.isSuccessful) {
                                    val currentPing = (System.currentTimeMillis() - t0).toDouble()
                                    if (currentPing < minP) minP = currentPing
                                }
                            }
                            minP
                        } catch (e: Exception) {
                            Double.MAX_VALUE
                        }
                        s.copy(distance = if (ping == Double.MAX_VALUE) 9999.0 else ping) 
                    }
                }.awaitAll().minByOrNull { it.distance } ?: realServers.first()
            }

            val serverUrl = targetServer.url.let { if (it.endsWith("/")) it else "$it/" }
            
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val client = okhttp3.OkHttpClient.Builder()
                .cache(null)
                .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .build()
                    val request = okhttp3.Request.Builder().url("https://get.geojs.io/v1/ip/geo.json")
                        .header("Cache-Control", "no-cache")
                        .build()
                    val ipsResponse = client.newCall(request).execute().body?.string()?.trim() ?: ""
                    val jsonObj = org.json.JSONObject(ipsResponse)
                    val orgName = jsonObj.optString("organization_name", "")
                    if (orgName.isNotBlank()) {
                        ispName = orgName
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SpeedTestRepository", "Error fetching ISP", e)
                }
                Unit
            }

            // Step 2: Measure Ping
            var finalPing = 0.0
            var finalJitter = 0.0
            try {
                val pingResult = speedTestService.measurePing(serverUrl) { progress: Float, currentPing: Double ->
                    finalPing = currentPing
                    send(SpeedTestState.PingTesting(progress, currentPing, targetServer))
                }
                finalPing = pingResult.ping
                finalJitter = pingResult.jitter
                delay(500)
            } catch (e: Exception) {
                // Save partial result with ping data if available
                val resultId = speedTestService.generateResultId()
                partialResult = SpeedTestResult(
                    id = resultId,
                    downloadSpeed = 0.0,
                    uploadSpeed = 0.0,
                    ping = finalPing,
                    jitter = finalJitter,
                    timestamp = System.currentTimeMillis(),
                    server = targetServer,
                    isp = ispName,
                    connectionType = connectionType,
                    signalInfo = signalInfo
                )
                savePartialResult(partialResult)
                throw e
            }

            // Step 3: Measure Download Speed (if not upload-only mode)
            var finalDownload = 0.0
            if (testMode != "upload") {
                try {
                    finalDownload = speedTestService.measureDownloadSpeed(serverUrl, disableTimeLimit) { progress: Float, currentSpeed: Double ->
                        send(SpeedTestState.DownloadTesting(progress, currentSpeed, targetServer))
                    }
                    delay(500)
                } catch (e: Exception) {
                    // Save partial result with ping and download data
                    val resultId = speedTestService.generateResultId()
                    partialResult = SpeedTestResult(
                        id = resultId,
                        downloadSpeed = finalDownload,
                        uploadSpeed = 0.0,
                        ping = finalPing,
                        jitter = finalJitter,
                        timestamp = System.currentTimeMillis(),
                        server = targetServer,
                        isp = ispName,
                        connectionType = connectionType,
                        signalInfo = signalInfo
                    )
                    savePartialResult(partialResult)
                    throw e
                }
            }

            // Step 4: Measure Upload Speed (if not download-only mode)
            var finalUpload = 0.0
            if (testMode != "download") {
                try {
                    finalUpload = speedTestService.measureUploadSpeed(serverUrl, disableTimeLimit) { progress: Float, currentSpeed: Double ->
                        send(SpeedTestState.UploadTesting(progress, currentSpeed, targetServer))
                    }
                } catch (e: Exception) {
                    // Save partial result with ping, download and partial upload data
                    val resultId = speedTestService.generateResultId()
                    partialResult = SpeedTestResult(
                        id = resultId,
                        downloadSpeed = finalDownload,
                        uploadSpeed = finalUpload,
                        ping = finalPing,
                        jitter = finalJitter,
                        timestamp = System.currentTimeMillis(),
                        server = targetServer,
                        isp = ispName,
                        connectionType = connectionType,
                        signalInfo = signalInfo
                    )
                    savePartialResult(partialResult)
                    throw e
                }
            }

            // Step 5: Create final result
            val resultId = speedTestService.generateResultId()
            val finalResult = SpeedTestResult(
                id = resultId,
                downloadSpeed = finalDownload,
                uploadSpeed = finalUpload,
                ping = finalPing,
                jitter = finalJitter,
                timestamp = System.currentTimeMillis(),
                server = targetServer,
                isp = ispName,
                connectionType = connectionType,
                signalInfo = signalInfo
            )

            // Step 6: Submit result to server (optional)
            try {
                api.submitResult(
                    url = "${serverUrl}results/update",
                    resultId = resultId,
                    ping = finalPing,
                    download = finalDownload,
                    upload = finalUpload
                )
            } catch (e: Exception) {
                // Submission failure is not critical
            }

            send(SpeedTestState.Success(finalResult))
        } catch (e: Exception) {
            // If we have a partial result from a specific failure, use it
            if (partialResult != null) {
                send(SpeedTestState.Error("${e.message ?: "Unknown Error"} - Partial results saved"))
            } else {
                // Create minimal partial result if no specific partial result was saved
                val resultId = speedTestService.generateResultId()
                val minimalResult = SpeedTestResult(
                    id = resultId,
                    downloadSpeed = 0.0,
                    uploadSpeed = 0.0,
                    ping = 0.0,
                    jitter = 0.0,
                    timestamp = System.currentTimeMillis(),
                    server = targetServer,
                    isp = ispName,
                    connectionType = connectionType,
                    signalInfo = signalInfo
                )
                savePartialResult(minimalResult)
                send(SpeedTestState.Error("${e.message ?: "Unknown Error"} - Partial results saved"))
            }
        }
    }

    override suspend fun getTestHistory(): Result<List<SpeedTestResult>> {
        return try {
            val json = sharedPreferences.getString("history", null)
            if (json != null) {
                val listType = object : com.google.gson.reflect.TypeToken<List<com.fairspeed.test.domain.model.SpeedTestResult>>() {}.type
                val history = com.google.gson.Gson().fromJson<List<com.fairspeed.test.domain.model.SpeedTestResult>>(json, listType)
                Result.Success(history ?: emptyList())
            } else {
                Result.Success(emptyList())
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private val publicServers = listOf(
        // Auto selection
        Server(id = "auto", name = "Auto", location = "Closest Server", country = "Auto", distance = 0.0, url = ""),
        
        // EUROPE - Clouvider, Linode, INEA & GARR (Verified Working)
        Server(id = "1", name = "Clouvider", location = "London, UK", country = "UK", distance = 0.0, url = "https://lon.speedtest.clouvider.net/backend/"),
        Server(id = "22", name = "Clouvider", location = "Manchester, UK", country = "UK", distance = 0.0, url = "https://man.speedtest.clouvider.net/backend/"),
        Server(id = "2", name = "Clouvider", location = "Amsterdam, NL", country = "NL", distance = 0.0, url = "https://ams.speedtest.clouvider.net/backend/"),
        Server(id = "3", name = "Clouvider", location = "Frankfurt, DE", country = "DE", distance = 0.0, url = "https://fra.speedtest.clouvider.net/backend/"),
        Server(id = "10", name = "Linode", location = "Paris, FR", country = "FR", distance = 0.0, url = "https://speedtest.paris.linode.com/"),
        Server(id = "18", name = "Linode", location = "Madrid, ES", country = "ES", distance = 0.0, url = "https://speedtest.madrid.linode.com/"),
        Server(id = "19", name = "Linode", location = "Stockholm, SE", country = "SE", distance = 0.0, url = "https://speedtest.stockholm.linode.com/"),
        Server(id = "20", name = "Linode", location = "Milan, IT", country = "IT", distance = 0.0, url = "https://speedtest.milan.linode.com/"),
        Server(id = "25", name = "GARR", location = "Roma, IT", country = "IT", distance = 0.0, url = "https://st-be-rm2.infra.garr.it/"),
        Server(id = "21", name = "CESNET", location = "Prague, CZ", country = "CZ", distance = 0.0, url = "https://speedtest.cesnet.cz/backend/"),
        Server(id = "24", name = "INEA", location = "Poznan, PL", country = "PL", distance = 0.0, url = "https://speedtest.kamilszczepanski.com/"),

        // NORTH AMERICA - Clouvider & Linode (Verified Working)
        Server(id = "4", name = "Clouvider", location = "Atlanta, US", country = "US", distance = 0.0, url = "https://atl.speedtest.clouvider.net/backend/"),
        Server(id = "5", name = "Clouvider", location = "New York, US", country = "US", distance = 0.0, url = "https://nyc.speedtest.clouvider.net/backend/"),
        Server(id = "6", name = "Clouvider", location = "Dallas, US", country = "US", distance = 0.0, url = "https://dal.speedtest.clouvider.net/backend/"),
        Server(id = "7", name = "Clouvider", location = "Los Angeles, US", country = "US", distance = 0.0, url = "https://la.speedtest.clouvider.net/backend/"),
        Server(id = "9", name = "Clouvider", location = "Ashburn, US", country = "US", distance = 0.0, url = "https://ash.speedtest.clouvider.net/backend/"),
        Server(id = "23", name = "Clouvider", location = "Phoenix, US", country = "US", distance = 0.0, url = "https://phx.speedtest.clouvider.net/backend/"),
        Server(id = "11", name = "Linode", location = "Toronto, CA", country = "CA", distance = 0.0, url = "https://speedtest.toronto1.linode.com/"),

        // ASIA & OCEANIA - Linode (Verified Working)
        Server(id = "12", name = "Linode", location = "Singapore", country = "SG", distance = 0.0, url = "https://speedtest.singapore.linode.com/"),
        Server(id = "13", name = "Linode", location = "Tokyo, JP", country = "JP", distance = 0.0, url = "https://speedtest.tokyo2.linode.com/"),
        Server(id = "14", name = "Linode", location = "Osaka, JP", country = "JP", distance = 0.0, url = "https://speedtest.osaka.linode.com/"),
        Server(id = "15", name = "Linode", location = "Mumbai, IN", country = "IN", distance = 0.0, url = "https://speedtest.mumbai1.linode.com/"),
        Server(id = "16", name = "Linode", location = "Sydney, AU", country = "AU", distance = 0.0, url = "https://speedtest.sydney.linode.com/"),

        // SOUTH AMERICA - Linode (Verified Working)
        Server(id = "17", name = "Linode", location = "Sao Paulo, BR", country = "BR", distance = 0.0, url = "https://speedtest.sao-paulo.linode.com/")
    )

    override suspend fun getServers(): Result<List<Server>> = kotlinx.coroutines.coroutineScope {
        try {
            val serversOut = publicServers.map { s ->
                async(kotlinx.coroutines.Dispatchers.IO) {
                    if (s.id == "auto") return@async s
                    
                    val serverUrl = s.url.let { if (it.endsWith("/")) it else "$it/" }
                    val ping = try {
                        // Warmup request to establish DNS, TCP and TLS handshakes
                        try { api.testPing("${serverUrl}empty.php") } catch (e: Exception) {}
                        kotlinx.coroutines.delay(10)
                        
                        var totalPing = 0.0
                        var validCount = 0
                        
                        // 3 actual measurements for an accurate average
                        repeat(3) {
                            val start = System.currentTimeMillis()
                            val response = try {
                                api.testPing("${serverUrl}empty.php")
                            } catch (e: Exception) { null }
                            
                            if (response?.isSuccessful == true) {
                                totalPing += (System.currentTimeMillis() - start).toDouble()
                                validCount++
                            }
                            kotlinx.coroutines.delay(10)
                        }
                        
                        if (validCount > 0) totalPing / validCount else 0.0
                    } catch (e: Exception) {
                        0.0
                    }
                    s.copy(distance = ping) 
                }
            }.awaitAll()
            
            val sortedServers = serversOut.sortedWith(Comparator { s1, s2 ->
                when {
                    s1.id == "auto" -> -1
                    s2.id == "auto" -> 1
                    s1.distance == 0.0 && s2.distance == 0.0 -> s1.name.compareTo(s2.name)
                    s1.distance == 0.0 -> 1
                    s2.distance == 0.0 -> -1
                    else -> s1.distance.compareTo(s2.distance)
                }
            })
            
            Result.Success(sortedServers)
        } catch (e: Exception) {
            val autoFirst = publicServers.partition { it.id == "auto" }.let { it.first + it.second }
            Result.Success(autoFirst)
        }
    }

    override suspend fun getUserIp(): Result<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        return@withContext try {
            val client = okhttp3.OkHttpClient.Builder()
                .cache(null)
                .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            var ipv4 = "Unknown"
            var ipv6 = "Not Available"
            var isp = "Unknown"
            
            try {
                val request = okhttp3.Request.Builder().url("https://get.geojs.io/v1/ip/geo.json")
                        .header("Cache-Control", "no-cache")
                        .build()
                val responseStr = client.newCall(request).execute().body?.string()?.trim() ?: ""
                val jsonObj = org.json.JSONObject(responseStr)
                val rawIp = jsonObj.optString("ip", "")
                val orgName = jsonObj.optString("organization_name", "")
                
                if (orgName.isNotBlank()) isp = orgName
                
                if (rawIp.isNotBlank()) {
                    if (rawIp.contains(":")) {
                        ipv6 = rawIp
                    } else {
                        ipv4 = rawIp
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SpeedTestRepository", "Error fetching getUserIp", e)
            }
            
            if (ipv4 == "Unknown") {
                try {
                    val reqV4 = okhttp3.Request.Builder().url("https://ipv4.icanhazip.com")
                        .header("Cache-Control", "no-cache")
                        .build()
                    val v4Response = client.newCall(reqV4).execute().body?.string()?.trim() ?: ""
                    if (v4Response.isNotBlank()) ipv4 = v4Response
                } catch(e: Exception) {}
            }
            
            if (ipv6 == "Not Available") {
                try {
                    val reqV6 = okhttp3.Request.Builder().url("https://ipv6.icanhazip.com")
                        .header("Cache-Control", "no-cache")
                        .build()
                    val v6Response = client.newCall(reqV6).execute().body?.string()?.trim() ?: ""
                    if (v6Response.isNotBlank()) ipv6 = v6Response
                } catch(e: Exception) {}
            }

            Result.Success("$ipv4|$ipv6|$isp")
        } catch (e: Exception) {
            android.util.Log.e("SpeedTestRepository", "Outer error in getUserIp", e)
            Result.Error(e)
        }
    }

    override suspend fun selectServer(serverId: String): Result<Server> {
        return try {
            val selected = publicServers.find { it.id == serverId }
            if (selected != null) {
                Result.Success(selected)
            } else {
                Result.Error(Exception("Server not found"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    
    
    override suspend fun clearHistory(): Result<Unit> {
        return try {
            sharedPreferences.edit().remove("history").apply()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun saveTestHistory(historyList: List<SpeedTestResult>): Result<Unit> {
        return try {
            val json = com.google.gson.Gson().toJson(historyList)
            sharedPreferences.edit().putString("history", json).apply()
            Result.Success(Unit)
        } catch(e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun savePartialResult(result: SpeedTestResult) {
        try {
            val currentHistory = getTestHistory()
            if (currentHistory is Result.Success) {
                val newHistory = (listOf(result) + currentHistory.data).take(50)
                val json = com.google.gson.Gson().toJson(newHistory)
                sharedPreferences.edit().putString("history", json).apply()
            } else {
                val json = com.google.gson.Gson().toJson(listOf(result))
                sharedPreferences.edit().putString("history", json).apply()
            }
        } catch (e: Exception) {
            android.util.Log.e("SpeedTestRepository", "Error saving partial result", e)
        }
    }
}
