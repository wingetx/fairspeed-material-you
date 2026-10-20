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

// Modified 2026-10-20: persistent accent selection for the FairSpeed Material You fork.

package com.fairspeed.test.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fairspeed.test.domain.model.AppTheme
import com.fairspeed.test.domain.model.AccentMode
import com.fairspeed.test.domain.model.normalizeAccentHex
import com.fairspeed.test.presentation.ui.theme.accentPreview
import com.fairspeed.test.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    onDonateClick: () -> Unit = {},
    onContactClick: () -> Unit = {},
    onAboutClick: () -> Unit = {}
) {
    val currentTheme by viewModel.appTheme.collectAsState()
    val accentMode by viewModel.accentMode.collectAsState()
    val customAccentHex by viewModel.customAccentHex.collectAsState()
    val defaultServerId by viewModel.defaultServerId.collectAsState()
    val servers by viewModel.servers.collectAsState()
    var themeMenuExpanded by remember { mutableStateOf(false) }
    var showAccentDialog by remember { mutableStateOf(false) }
    var pendingAccent by remember { mutableStateOf(AccentMode.MATERIAL_YOU) }
    var pendingCustomHex by remember { mutableStateOf("367C89") }
    var serverMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Settings") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
        
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            // Default Server Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { serverMenuExpanded = true }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "Default Server",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Default Server",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (defaultServerId == null) "Auto" else servers.find { it.id == defaultServerId }?.name ?: "Unknown",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = serverMenuExpanded,
                    onDismissRequest = { serverMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("🌍 Auto (Recommended)") },
                        onClick = {
                            viewModel.setDefaultServerId(null)
                            serverMenuExpanded = false
                        }
                    )
                    servers.filter { !it.id.equals("auto", ignoreCase = true) }.forEach { server ->
                        DropdownMenuItem(
                            text = { 
                                val emoji = if (server.country.equals("auto", ignoreCase = true)) "🌍" else server.country.let {
                                    val code = if (it.equals("UK", ignoreCase = true)) "GB" else it.uppercase()
                                    if (code.length == 2) {
                                        try {
                                            val firstLetter = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6
                                            val secondLetter = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6
                                            String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
                                        } catch (e: Exception) { "" }
                                    } else ""
                                }
                                Text("$emoji ${server.country.uppercase()} ${server.location} - ${server.name}")
                            },
                            onClick = {
                                viewModel.setDefaultServerId(server.id)
                                serverMenuExpanded = false
                            }
                        )
                    }
                }
            }
            
            HorizontalDivider()

            // Theme Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { themeMenuExpanded = true }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Theme",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "App Theme",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = when (currentTheme) {
                            AppTheme.SYSTEM -> "System Default"
                            AppTheme.LIGHT -> "Light"
                            AppTheme.DARK -> "Dark"
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                DropdownMenu(
                    expanded = themeMenuExpanded,
                    onDismissRequest = { themeMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("System Default") },
                        onClick = {
                            viewModel.setAppTheme(AppTheme.SYSTEM)
                            themeMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Light") },
                        onClick = {
                            viewModel.setAppTheme(AppTheme.LIGHT)
                            themeMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Dark") },
                        onClick = {
                            viewModel.setAppTheme(AppTheme.DARK)
                            themeMenuExpanded = false
                        }
                    )
                }
            }
            
            HorizontalDivider()

            // Accent palette, independent of Android's system-wide wallpaper colors.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        pendingAccent = accentMode
                        pendingCustomHex = customAccentHex
                        showAccentDialog = true
                    }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Accent color",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Accent Color", fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        if (accentMode == AccentMode.MATERIAL_YOU) "Material You · follows system colors"
                        else accentMode.displayName,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            if (accentMode == AccentMode.MATERIAL_YOU) MaterialTheme.colorScheme.primary
                            else accentPreview(accentMode, customAccentHex),
                            CircleShape
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            }

            HorizontalDivider()

            // Donate Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDonateClick() }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Donate",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Donate",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Support the project",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            HorizontalDivider()

            // Contact Us Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onContactClick() }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Contact",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Contact Us",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "lab@naxod.com",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()
            
            // About Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAboutClick() }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "About",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "About",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }

    if (showAccentDialog) {
        val normalized = normalizeAccentHex(pendingCustomHex)
        val validHex = normalized != null
        AlertDialog(
            onDismissRequest = { showAccentDialog = false },
            title = { Text("Choose accent color") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Only changes FairSpeed. Your phone's system colors stay as they are.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 370.dp)) {
                        items(AccentMode.values().toList()) { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { pendingAccent = option }
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RadioButton(
                                    selected = pendingAccent == option,
                                    onClick = { pendingAccent = option }
                                )
                                Box(
                                    modifier = Modifier
                                        .size(23.dp)
                                        .background(
                                            if (option == AccentMode.MATERIAL_YOU) MaterialTheme.colorScheme.primary
                                            else accentPreview(option, pendingCustomHex),
                                            CircleShape
                                        )
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                )
                                Text(option.displayName)
                            }
                        }
                    }
                    if (pendingAccent == AccentMode.CUSTOM) {
                        OutlinedTextField(
                            value = pendingCustomHex,
                            onValueChange = { input ->
                                if (input.length <= 7 && input.all { it == '#' || it.isLetterOrDigit() }) {
                                    pendingCustomHex = input
                                }
                            },
                            label = { Text("RGB hex (e.g. 367C89)") },
                            singleLine = true,
                            isError = !validHex,
                            supportingText = {
                                if (!validHex) Text("Enter exactly six hexadecimal digits")
                            }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = pendingAccent != AccentMode.CUSTOM || validHex,
                    onClick = {
                        viewModel.setAccent(pendingAccent, normalized ?: customAccentHex)
                        showAccentDialog = false
                    }
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showAccentDialog = false }) { Text("Cancel") }
            }
        )
    }
}
