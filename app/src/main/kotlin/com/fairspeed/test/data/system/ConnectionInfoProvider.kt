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

package com.fairspeed.test.data.system

import android.Manifest
import android.content.Context
import android.os.Build
import android.telephony.CellIdentityNr
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthCdma
import android.telephony.CellSignalStrengthGsm
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.CellSignalStrengthTdscdma
import android.telephony.CellSignalStrengthWcdma
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import com.fairspeed.test.domain.model.SignalInfo
import com.fairspeed.test.domain.model.SignalType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides detailed connection info labels (e.g. "5G (n78)", "4G+ (B3+B7)",
 * "WiFi 6 (5 GHz)") instead of generic "Cellular"/"WiFi".
 *
 * Cellular details (network type, band, carrier aggregation) require the
 * READ_PHONE_STATE and ACCESS_FINE_LOCATION runtime permissions. Everything
 * degrades gracefully to generic labels when permissions are not granted.
 */
@Singleton
class ConnectionInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @Volatile
    private var lastDisplayInfo: TelephonyDisplayInfo? = null

    private var registered = false
    private var telephonyCallback: TelephonyCallback? = null
    private var phoneStateListener: PhoneStateListener? = null

    /**
     * Register/unregister telephony listeners according to currently granted
     * permissions. Safe to call repeatedly.
     */
    @Synchronized
    fun refresh() {
        try {
            val tm = context.getSystemService(TelephonyManager::class.java) ?: return
            if (hasPhoneStatePermission()) {
                if (!registered) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val callback = object : TelephonyCallback(),
                            TelephonyCallback.DisplayInfoListener {
                            override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
                                lastDisplayInfo = telephonyDisplayInfo
                            }
                        }
                        telephonyCallback = callback
                        tm.registerTelephonyCallback(context.mainExecutor, callback)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val listener = object : PhoneStateListener() {
                            override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
                                lastDisplayInfo = telephonyDisplayInfo
                            }
                        }
                        phoneStateListener = listener
                        tm.listen(listener, PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED)
                    }
                    registered = true
                }
            } else if (registered) {
                unregisterListeners()
            }
        } catch (e: Exception) {
            android.util.Log.e("ConnectionInfo", "Error registering telephony listeners", e)
        }
    }

    @Synchronized
    private fun unregisterListeners() {
        try {
            val tm = context.getSystemService(TelephonyManager::class.java)
            telephonyCallback?.let { tm?.unregisterTelephonyCallback(it) }
            phoneStateListener?.let { tm?.listen(it, PhoneStateListener.LISTEN_NONE) }
        } catch (e: Exception) {
            android.util.Log.e("ConnectionInfo", "Error unregistering telephony listeners", e)
        }
        telephonyCallback = null
        phoneStateListener = null
        registered = false
    }

    fun hasPhoneStatePermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED

    fun hasLocationPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED

    fun hasDetailPermissions(): Boolean = hasPhoneStatePermission() && hasLocationPermission()

    fun isCellular(): Boolean {
        return try {
            val cm = context.getSystemService(android.net.ConnectivityManager::class.java) ?: return false
            val network = cm.activeNetwork ?: return false
            cm.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Build the human-readable connection label for the current network.
     * Never throws; returns generic labels when details are unavailable.
     */
    fun getConnectionType(): String {
        return try {
            val cm = context.getSystemService(android.net.ConnectivityManager::class.java)
                ?: return "Unknown"
            val network = cm.activeNetwork ?: return "Unknown"
            val capabilities = cm.getNetworkCapabilities(network) ?: return "Unknown"

            val base = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> wifiLabel(capabilities)
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> cellularLabel()
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                else -> "Unknown"
            }

            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) && base != "VPN") {
                "VPN ($base)"
            } else {
                base
            }
        } catch (e: Exception) {
            android.util.Log.e("ConnectionInfo", "Error reading connection type", e)
            "Unknown"
        }
    }

    private fun wifiLabel(capabilities: NetworkCapabilities): String {
        var label = "WiFi"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val wifiInfo = capabilities.getTransportInfo() as? WifiInfo
                if (wifiInfo != null) {
                    // Frequency may be redacted without location permission
                    val freq = wifiInfo.frequency
                    val sixGhz = freq in 5925..7125
                    label = when (wifiInfo.wifiStandard) {
                        ScanResult.WIFI_STANDARD_11BE -> "WiFi 7"
                        ScanResult.WIFI_STANDARD_11AX -> if (sixGhz) "WiFi 6E" else "WiFi 6"
                        ScanResult.WIFI_STANDARD_11AC -> "WiFi 5"
                        ScanResult.WIFI_STANDARD_11N -> "WiFi 4"
                        ScanResult.WIFI_STANDARD_11AD -> "WiGig"
                        else -> label
                    }
                    label += when {
                        sixGhz && !label.startsWith("WiFi 6") -> " (6 GHz)"
                        freq in 2400..2483 -> " (2.4 GHz)"
                        freq in 4910..5875 -> " (5 GHz)"
                        else -> ""
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ConnectionInfo", "Error reading WiFi details", e)
            }
        }
        return label
    }

    private fun cellularLabel(): String {
        var label: String? = null
        var carrierAggregation = false

        // Prefer the cached TelephonyDisplayInfo (listener requires READ_PHONE_STATE)
        val displayInfo = lastDisplayInfo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && displayInfo != null) {
            label = ratLabel(displayInfo.networkType)
            when (displayInfo.overrideNetworkType) {
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA,
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE,
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED -> label = "5G"
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA -> {
                    label = "4G"
                    carrierAggregation = true
                }
            }
        }

        if (label == null) {
            // Works without permission on Android <= 10; needs READ_PHONE_STATE on Android 11+
            label = try {
                val tm = context.getSystemService(TelephonyManager::class.java)
                tm?.let { ratLabel(it.dataNetworkType) }
            } catch (e: Exception) {
                null
            }
        }

        if (label == null) {
            // Last resort: derive the RAT from the readable cell signal strengths
            // (same permission as signal reading) to avoid the generic "Cellular"
            label = ratFromSignalStrength()
        }

        if (label == null) return "Cellular"

        val bands = cellularBands()
        val suffix = if (carrierAggregation && !label.startsWith("5G")) "+" else ""

        return if (bands != null) "$label$suffix ($bands)" else "$label$suffix"
    }

    private fun ratLabel(networkType: Int): String? = when (networkType) {
        TelephonyManager.NETWORK_TYPE_NR -> "5G"
        TelephonyManager.NETWORK_TYPE_LTE,
        TelephonyManager.NETWORK_TYPE_HSPAP -> "4G"
        TelephonyManager.NETWORK_TYPE_HSDPA,
        TelephonyManager.NETWORK_TYPE_HSUPA,
        TelephonyManager.NETWORK_TYPE_HSPA,
        TelephonyManager.NETWORK_TYPE_EVDO_0,
        TelephonyManager.NETWORK_TYPE_EVDO_A,
        TelephonyManager.NETWORK_TYPE_EVDO_B,
        TelephonyManager.NETWORK_TYPE_EHRPD,
        TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
        TelephonyManager.NETWORK_TYPE_CDMA,
        TelephonyManager.NETWORK_TYPE_1xRTT,
        TelephonyManager.NETWORK_TYPE_GPRS,
        TelephonyManager.NETWORK_TYPE_EDGE,
        TelephonyManager.NETWORK_TYPE_GSM -> "2G"
        else -> null
    }

    /**
     * Signal strength of the active connection: WiFi RSSI or cellular level/dBm.
     * Returns null when the active transport is not WiFi/Cellular or the values
     * cannot be read (e.g. missing READ_PHONE_STATE / location permission).
     */
    fun getSignalInfo(): SignalInfo? {
        return try {
            val cm = context.getSystemService(android.net.ConnectivityManager::class.java)
                ?: return null
            val network = cm.activeNetwork ?: return null
            val capabilities = cm.getNetworkCapabilities(network) ?: return null
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> wifiSignal()
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> cellularSignal()
                else -> null
            }
        } catch (e: Exception) {
            android.util.Log.e("ConnectionInfo", "Error reading signal info", e)
            null
        }
    }

    private fun wifiSignal(): SignalInfo? {
        return try {
            val wm = context.getSystemService(WifiManager::class.java) ?: return null
            val wifiInfo = wm.connectionInfo ?: return null
            val rssi = wifiInfo.rssi
            if (rssi <= -100) return null // also covers the old INVALID_RSSI (-127) sentinel
            SignalInfo(type = SignalType.WIFI, level = wifiLevel(rssi), dbm = rssi)
        } catch (e: Exception) {
            // SecurityException on builds that gate WifiInfo behind location permission
            null
        }
    }

    // Same thresholds as WifiManager.calculateSignalLevel(rssi, 5)
    private fun wifiLevel(rssi: Int): Int = when {
        rssi <= -100 -> 0
        rssi >= -50 -> 4
        else -> ((rssi + 100) * 4 / 50).coerceIn(0, 4)
    }

    private fun cellularSignal(): SignalInfo? {
        return try {
            val tm = context.getSystemService(TelephonyManager::class.java) ?: return null
            val signalStrength = tm.signalStrength ?: return null
            val level = signalStrength.level.coerceIn(0, 4)
            val dbm = cellularDbm(signalStrength)
            if (level <= 0 && dbm == null) return null
            SignalInfo(type = SignalType.CELLULAR, level = level, dbm = dbm)
        } catch (e: Exception) {
            // SecurityException without READ_PHONE_STATE / READ_BASIC_PHONE_STATE
            null
        }
    }

    private fun cellularDbm(signalStrength: SignalStrength): Int? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return signalStrength.cellSignalStrengths
                ?.filter { it.dbm != Int.MAX_VALUE }
                ?.maxByOrNull { it.dbm }
                ?.dbm
        }
        @Suppress("DEPRECATION")
        val cdmaDbm = signalStrength.cdmaDbm
        if (cdmaDbm in -120..-2) return cdmaDbm
        @Suppress("DEPRECATION")
        val evdoDbm = signalStrength.evdoDbm
        if (evdoDbm in -120..-2) return evdoDbm
        @Suppress("DEPRECATION")
        val gsmAsu = signalStrength.gsmSignalStrength
        if (gsmAsu in 1..31) return -113 + 2 * gsmAsu
        return null
    }

    /**
     * Best-effort RAT label derived from readable cell signal strengths, used
     * when TelephonyDisplayInfo and dataNetworkType are unavailable.
     */
    private fun ratFromSignalStrength(): String? {
        return try {
            val tm = context.getSystemService(TelephonyManager::class.java) ?: return null
            val signalStrength = tm.signalStrength ?: return null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val cells = signalStrength.cellSignalStrengths?.ifEmpty { null } ?: return null
                return when {
                    cells.any { it is CellSignalStrengthNr } -> "5G"
                    cells.any { it is CellSignalStrengthLte } -> "4G"
                    cells.any { it is CellSignalStrengthWcdma || it is CellSignalStrengthTdscdma } -> "3G"
                    cells.any { it is CellSignalStrengthCdma } -> "3G"
                    cells.any { it is CellSignalStrengthGsm } -> "2G"
                    else -> null
                }
            }
            @Suppress("DEPRECATION")
            val cdma = signalStrength.cdmaDbm in -120..-2
            @Suppress("DEPRECATION")
            val evdo = signalStrength.evdoDbm in -120..-2
            @Suppress("DEPRECATION")
            val gsm = signalStrength.gsmSignalStrength in 1..31
            when {
                evdo || cdma -> return "3G"
                gsm -> return "2G"
                else -> return null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Returns the band string (e.g. "B3", "n78", "B1+B3") for registered cells.
     * Requires ACCESS_FINE_LOCATION (getAllCellInfo); returns null when unavailable.
     */
    private fun cellularBands(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || !hasLocationPermission()) return null
        return try {
            val tm = context.getSystemService(TelephonyManager::class.java) ?: return null
            val bands = mutableListOf<String>()
            for (cellInfo in tm.allCellInfo) {
                if (!cellInfo.isRegistered) continue
                when (cellInfo) {
                    is CellInfoLte -> {
                        val cellBands = cellInfo.cellIdentity.bands
                        if (cellBands != null && cellBands.isNotEmpty()) {
                            bands.addAll(cellBands.distinct().map { "B$it" })
                        }
                    }
                    is CellInfoNr -> {
                        // getCellIdentity() returns the base CellIdentity in the public SDK
                        val cellBands = (cellInfo.cellIdentity as? CellIdentityNr)?.bands
                        if (cellBands != null && cellBands.isNotEmpty()) {
                            bands.addAll(cellBands.distinct().map { "n$it" })
                        }
                    }
                }
            }
            if (bands.isEmpty()) null else bands.distinct().joinToString("+")
        } catch (e: Exception) {
            null
        }
    }
}
