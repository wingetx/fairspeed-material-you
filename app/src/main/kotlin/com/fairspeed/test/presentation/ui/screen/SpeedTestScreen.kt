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

// Modified 2026-10-20 for FairSpeed Material You: dynamic theme and UI spacing.

package com.fairspeed.test.presentation.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.fairspeed.test.R
import com.fairspeed.test.domain.model.SpeedTestState
import com.fairspeed.test.domain.model.Server
import com.fairspeed.test.domain.model.flagEmoji
import com.fairspeed.test.presentation.viewmodel.SpeedTestViewModel
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.IconButton
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

// Permissions required for the detailed network info (5G/4G, band, carrier aggregation)
private val NETWORK_DETAILS_PERMISSIONS = arrayOf(
    android.Manifest.permission.READ_PHONE_STATE,
    android.Manifest.permission.ACCESS_FINE_LOCATION,
    android.Manifest.permission.ACCESS_COARSE_LOCATION
)

@Composable
fun SpeedTestScreen(
    viewModel: SpeedTestViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.speedTestState.collectAsState()
    val selectedServer by viewModel.selectedServer.collectAsState()
    val servers by viewModel.servers.collectAsState()
    val userIp by viewModel.userIp.collectAsState()
    val testMode by viewModel.testMode.collectAsState()
    val disableTimeLimit by viewModel.disableTimeLimit.collectAsState()
    val connectionType by viewModel.connectionType.collectAsState()
    val showNetworkDetailsPrompt by viewModel.showNetworkDetailsPrompt.collectAsState()

    val context = LocalContext.current

    // Refresh the connection label when the screen resumes (e.g. after enabling
    // the network details permissions from the system settings)
    DisposableEffect(Unit) {
        val lifecycleOwner = context as? LifecycleOwner
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshConnectionInfo()
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose {
            lifecycleOwner?.lifecycle?.removeObserver(observer)
        }
    }

    // Network details permission request state
    var showNetworkPermissionsRationale by remember { mutableStateOf(false) }
    var showOpenSettingsDialog by remember { mutableStateOf(false) }
    val networkPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { !it }) {
            // If the system no longer shows the rationale, the permissions were
            // permanently denied ("don't ask again"): offer the app settings instead
            val activity = context as? android.app.Activity
            val canAskAgain = activity != null && NETWORK_DETAILS_PERMISSIONS.any { permission ->
                activity.shouldShowRequestPermissionRationale(permission)
            }
            if (!canAskAgain) {
                showOpenSettingsDialog = true
            }
        }
        viewModel.refreshConnectionInfo()
    }

    val activeServer = when (state) {
        is SpeedTestState.PingTesting -> (state as SpeedTestState.PingTesting).server
        is SpeedTestState.DownloadTesting -> (state as SpeedTestState.DownloadTesting).server
        is SpeedTestState.UploadTesting -> (state as SpeedTestState.UploadTesting).server
        is SpeedTestState.Success -> (state as SpeedTestState.Success).result.server
        else -> null
    }

    val isAutoSelected = selectedServer?.id == "auto"
    val headerServerName = if (isAutoSelected && activeServer != null && activeServer.id != "auto") {
        "🌍 Auto - ${activeServer.flagEmoji} ${activeServer.location} - ${activeServer.name}"
    } else if (isAutoSelected) {
        "🌍 Auto"
    } else {
        selectedServer?.let { "${it.flagEmoji} ${it.location} - ${it.name}" } ?: "No Server Selected"
    }

    val headerLocation = "" 

    // Advanced options dialog state
    var showAdvancedOptionsDialog by remember { mutableStateOf(false) }
    
    // Temporary state for dialog options
    var tempTestMode by remember { mutableStateOf(testMode) }
    var tempDisableTimeLimit by remember { mutableStateOf(disableTimeLimit) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Available height for the home screen: the modifier from MainScreen is already
        // reduced by Scaffold padding (top bar, bottom navigation and system nav bar)
        val availableHeight = maxHeight

        // Adaptive vertical spacing: tightens when vertical space is scarce so the
        // home fits the display without scrolling whenever possible
        val verticalSpacing = when {
            availableHeight < 560.dp -> 4.dp
            availableHeight < 720.dp -> 6.dp
            else -> 8.dp
        }

        // Adaptive test area height: fills the space left by the other content
        // (header, setup, badge, button, IP info, padding and spacing ~ 340dp),
        // with a minimum to avoid over-squeezing and a 360dp maximum.
        // Success needs ~300dp for the 4 result cards.
        val testAreaMinHeight = if (state is SpeedTestState.Success) 300.dp else 240.dp
        val testAreaHeight = (availableHeight - 340.dp).coerceIn(testAreaMinHeight, 360.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(verticalSpacing)
        ) {
            val isTestRunning = state is SpeedTestState.Loading ||
                state is SpeedTestState.DownloadTesting ||
                state is SpeedTestState.UploadTesting ||
                state is SpeedTestState.PingTesting

            // Header with both centered title and overflow menu
            HeaderSection(
                serverName = headerServerName,
                location = headerLocation,
                servers = servers,
                isClickable = !isTestRunning,
                onServerSelected = { serverId ->
                    viewModel.selectServer(serverId)
                },
                onAdvancedOptionsClick = { 
                    tempTestMode = testMode
                    tempDisableTimeLimit = disableTimeLimit
                    showAdvancedOptionsDialog = true
                }
            )

            // Main Testing Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(testAreaHeight)
                    .clip(shape = MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    is SpeedTestState.Idle -> IdleContent()
                    is SpeedTestState.Loading -> LoadingContent()
                    is SpeedTestState.Success -> SuccessContent(state as SpeedTestState.Success)
                    is SpeedTestState.Error -> ErrorContent(state as SpeedTestState.Error)
                    is SpeedTestState.DownloadTesting -> DownloadTestingContent(state as SpeedTestState.DownloadTesting)
                    is SpeedTestState.UploadTesting -> UploadTestingContent(state as SpeedTestState.UploadTesting)
                    is SpeedTestState.PingTesting -> PingTestingContent(state as SpeedTestState.PingTesting)
                }
            }

            // Setup Info Display
            if (state is SpeedTestState.Idle) {
                val setupText = buildString {
                    append("Setup: ")

                    // Test mode
                    when (testMode) {
                        "download" -> append("Download only")
                        "upload" -> append("Upload only")
                        else -> append("Default")
                    }

                    // Extended test
                    if (disableTimeLimit) {
                        if (testMode != "both") append(" - ")
                        append("Extended")
                    }
                }

                Text(
                    text = setupText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // Connection type badge (uses the type captured at test start when results are shown)
            val connectionBadge = if (state is SpeedTestState.Success) {
                (state as SpeedTestState.Success).result.connectionType
            } else {
                connectionType
            }
            // When the network details permissions are missing, the badge becomes a
            // button: tapping it asks for the missing permissions (also after past denials)
            val badgeNeedsPermissions = state is SpeedTestState.Idle && showNetworkDetailsPrompt
            if (connectionBadge.isNotBlank() && connectionBadge != "Unknown") {
                Text(
                    text = if (badgeNeedsPermissions) "$connectionBadge · Tap for details" else connectionBadge,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .then(
                            if (badgeNeedsPermissions) {
                                Modifier.clickable { showNetworkPermissionsRationale = true }
                            } else {
                                Modifier
                            }
                        )
                )
            }

            // Control Button
            ControlButton(
                state = state,
                onStartTest = { viewModel.startSpeedTest() },
                onStopTest = { viewModel.stopSpeedTest() }
            )
            
            // Advanced Options Dialog
            if (showAdvancedOptionsDialog) {
                AdvancedOptionsDialog(
                    testMode = tempTestMode,
                    disableTimeLimit = tempDisableTimeLimit,
                    onTestModeChange = { tempTestMode = it },
                    onDisableTimeLimitChange = { tempDisableTimeLimit = it },
                    onConfirm = {
                        viewModel.setTestMode(tempTestMode)
                        viewModel.setDisableTimeLimit(tempDisableTimeLimit)
                        showAdvancedOptionsDialog = false
                    },
                    onDismiss = { showAdvancedOptionsDialog = false }
                )
            }

            // Network details permission rationale dialog
            if (showNetworkPermissionsRationale) {
                AlertDialog(
                    onDismissRequest = { showNetworkPermissionsRationale = false },
                    title = { Text(text = "Network details") },
                    text = {
                        Text(
                            text = "FairSpeed can show your connection type (4G/5G), the cellular band and whether carrier aggregation is active.\n\n" +
                                    "This requires Phone and Location permissions. Location is used only to read the current cell info and is never stored or shared."
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            showNetworkPermissionsRationale = false
                            networkPermissionsLauncher.launch(NETWORK_DETAILS_PERMISSIONS)
                        }) {
                            Text("Continue")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNetworkPermissionsRationale = false }) {
                            Text("Not now")
                        }
                    }
                )
            }

            // Shown when the permissions were permanently denied: open the app settings
            if (showOpenSettingsDialog) {
                AlertDialog(
                    onDismissRequest = { showOpenSettingsDialog = false },
                    title = { Text("Enable permissions") },
                    text = {
                        Text(
                            "Network details need Phone and Location permissions, which were denied. " +
                                    "Please enable them in the app settings to see 5G/4G, band and carrier aggregation."
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            showOpenSettingsDialog = false
                            val settingsIntent = Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(settingsIntent)
                        }) {
                            Text("Open settings")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showOpenSettingsDialog = false }) {
                            Text("Not now")
                        }
                    }
                )
            }

            if (userIp != null && userIp?.isNotBlank() == true) {
                val ips = userIp!!.split("|")
                val ipv4 = ips.getOrNull(0) ?: "Unknown"
                val ipv6 = ips.getOrNull(1) ?: "Not Available"
                val isp = ips.getOrNull(2) ?: "Unknown ISP"
                
                SelectionContainer {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        if (isp != "Unknown ISP" && isp.isNotBlank()) {
                            Text(
                                text = "ISP: $isp",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                        Text(
                            text = "IPv4: $ipv4",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "IPv6: $ipv6",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }


        }
    }
}

@Composable
private fun HeaderSection(
    serverName: String,
    location: String = "",
    servers: List<Server> = emptyList(),
    isClickable: Boolean = true,
    onServerSelected: (String) -> Unit = {},
    onAdvancedOptionsClick: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(enabled = isClickable) { expanded = true }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header with centered title and overflow menu
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Speed Test title - centered
                Text(
                    text = stringResource(R.string.speed_test),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                // Overflow menu - positioned to the right within the same card
                IconButton(
                    onClick = onAdvancedOptionsClick,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Advanced options",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayText = if (location.isNotEmpty()) "$serverName - $location" else serverName
                Text(
                    text = displayText,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select Server",
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                servers.forEach { server ->
                    DropdownMenuItem(
                        text = { 
                            if (server.id == "auto") {
                                Text("🌍 ${server.name}")
                            } else {
                                val pingText = if (server.distance == 0.0) " (Unreachable)" else " (${server.distance.toInt()} ms)"
                                Text("${server.flagEmoji} ${server.location} - ${server.name}$pingText") 
                            }
                        },
                        onClick = {
                            onServerSelected(server.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.fairspeed_icon),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "FairSpeed",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Ready to test your internet speed",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LoadingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.testing),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SuccessContent(state: SpeedTestState.Success) {
    val result = state.result

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(R.string.test_complete),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Download Speed
        SpeedCard(
            label = stringResource(R.string.download_speed),
            value = String.format("%.1f", result.downloadSpeed),
            unit = stringResource(R.string.mbps),
            color = Color(0xFF4CAF50)
        )

        // Upload Speed
        SpeedCard(
            label = stringResource(R.string.upload_speed),
            value = String.format("%.1f", result.uploadSpeed),
            unit = stringResource(R.string.mbps),
            color = Color(0xFFF57C00)
        )

        // Ping
        SpeedCard(
            label = stringResource(R.string.ping),
            value = String.format("%.1f", result.ping),
            unit = stringResource(R.string.ms),
            color = null
        )

        // Jitter
        SpeedCard(
            label = stringResource(R.string.jitter),
            value = String.format("%.1f", result.jitter),
            unit = stringResource(R.string.ms),
            color = null
        )
    }
}

@Composable
private fun ErrorContent(state: SpeedTestState.Error) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⚠️",
            fontSize = 48.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.error),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF5252)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = state.message,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DownloadTestingContent(state: SpeedTestState.DownloadTesting) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(80.dp),
            color = Color(0xFF4CAF50),
            strokeWidth = 6.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.download_speed),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = buildAnnotatedString {
                val speedParts = String.format("%.2f", state.currentSpeed).replace(",", ".").split(".")
                withStyle(SpanStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))) {
                    append(speedParts[0])
                }
                withStyle(SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50).copy(alpha = 0.6f))) {
                    append(",${if(speedParts.size>1) speedParts[1] else "00"}")
                }
                withStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))) {
                    append(" Mbps")
                }
            }
        )
        Text(
            text = "Measuring...",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UploadTestingContent(state: SpeedTestState.UploadTesting) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(80.dp),
            color = Color(0xFFF57C00),
            strokeWidth = 6.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.upload_speed),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = buildAnnotatedString {
                val speedParts = String.format("%.2f", state.currentSpeed).replace(",", ".").split(".")
                withStyle(SpanStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF57C00))) {
                    append(speedParts[0])
                }
                withStyle(SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF57C00).copy(alpha = 0.6f))) {
                    append(",${if(speedParts.size>1) speedParts[1] else "00"}")
                }
                withStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF57C00))) {
                    append(" Mbps")
                }
            }
        )
        Text(
            text = "Measuring...",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PingTestingContent(state: SpeedTestState.PingTesting) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(80.dp),
            color = Color(0xFF9C27B0),
            strokeWidth = 6.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.ping),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = buildAnnotatedString {
                val pingStr = String.format("%.0f", state.currentPing)
                withStyle(SpanStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9C27B0))) {
                    append(pingStr)
                }
                withStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9C27B0))) {
                    append(" ms")
                }
            }
        )
        Text(
            text = "Measuring...",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SpeedCard(
    label: String,
    value: String,
    unit: String,
    color: Color?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val displayColor = color ?: MaterialTheme.colorScheme.onSurface
                Text(
                    text = buildAnnotatedString {
                        val parts = value.replace(",", ".").split(".")
                        withStyle(SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = displayColor)) {
                            append(parts[0])
                        }
                        if (parts.size > 1) {
                            withStyle(SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = displayColor.copy(alpha = 0.6f))) {
                                append(",${parts[1]}")
                            }
                        }
                    }
                )
                Text(
                    text = unit,
                    fontSize = 14.sp,
                    color = displayColor,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ControlButton(
    state: SpeedTestState,
    onStartTest: () -> Unit,
    onStopTest: () -> Unit
) {
    val isLoading = state is SpeedTestState.Loading ||
            state is SpeedTestState.DownloadTesting ||
            state is SpeedTestState.UploadTesting ||
            state is SpeedTestState.PingTesting

    Button(
        onClick = if (isLoading) onStopTest else onStartTest,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isLoading) Color.LightGray else MaterialTheme.colorScheme.primary
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = if (isLoading) stringResource(R.string.stop_test) else stringResource(R.string.start_test),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isLoading) Color.Black else MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun HistoryItemCard(result: com.fairspeed.test.domain.model.SpeedTestResult) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val date = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, java.util.Locale.getDefault()).format(java.util.Date(result.timestamp))
                    val time = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, java.util.Locale.getDefault()).format(java.util.Date(result.timestamp))
                    Text(text = date, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = time, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (result.isp != "Unknown" && result.isp != "Unknown ISP" && result.isp.isNotBlank()) {
                    Text(
                        text = result.isp,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                } else {
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                }
                Text(
                    text = result.connectionType,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HistoryMetric(label = stringResource(R.string.download_speed), value = String.format("%.1f", result.downloadSpeed), unit = "Mbps", color = Color(0xFF4CAF50))
                HistoryMetric(label = stringResource(R.string.upload_speed), value = String.format("%.1f", result.uploadSpeed), unit = "Mbps", color = Color(0xFFF57C00))
                HistoryMetric(label = stringResource(R.string.ping), value = String.format("%.0f", result.ping), unit = "ms", color = Color(0xFF9C27B0))
                HistoryMetric(label = stringResource(R.string.jitter), value = String.format("%.0f", result.jitter), unit = "ms", color = Color(0xFF673AB7))
            }
        }
    }
}

@Composable
private fun HistoryMetric(label: String, value: String, unit: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = unit, fontSize = 10.sp, color = color, modifier = Modifier.padding(bottom = 1.dp))
        }
    }
}

@Composable
private fun AdvancedOptionsDialog(
    testMode: String,
    disableTimeLimit: Boolean,
    onTestModeChange: (String) -> Unit,
    onDisableTimeLimitChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Advanced options") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = "Test Mode:", fontWeight = FontWeight.Bold)
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = testMode == "both",
                        onClick = { onTestModeChange("both") }
                    )
                    Text(text = "Download + Upload (Default)", modifier = Modifier.clickable { onTestModeChange("both") })
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = testMode == "download",
                        onClick = { onTestModeChange("download") }
                    )
                    Text(text = "Download Only", modifier = Modifier.clickable { onTestModeChange("download") })
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = testMode == "upload",
                        onClick = { onTestModeChange("upload") }
                    )
                    Text(text = "Upload Only", modifier = Modifier.clickable { onTestModeChange("upload") })
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable { onDisableTimeLimitChange(!disableTimeLimit) }
                ) {
                    Checkbox(
                        checked = disableTimeLimit,
                        onCheckedChange = { onDisableTimeLimitChange(it) }
                    )
                    Text(text = "Extended test (30 seconds)")
                }
                
                Text(
                    text = "Note: Extended tests run for 30 seconds per phase instead of 8 seconds",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Apply")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Gray)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary)
            )
        }
    }
}