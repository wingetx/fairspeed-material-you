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

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fairspeed.test.presentation.viewmodel.SpeedTestViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

@Composable
fun ExportImportHistoryActions(viewModel: SpeedTestViewModel) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { 
            viewModel.exportHistory(it, context,
                onSuccess = { count -> 
                    android.widget.Toast.makeText(context, "Successfully exported $count items", android.widget.Toast.LENGTH_SHORT).show() 
                },
                onError = { err -> 
                    android.widget.Toast.makeText(context, "Export failed: $err", android.widget.Toast.LENGTH_SHORT).show() 
                }
            )
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { 
            viewModel.importHistory(it, context,
                onSuccess = { count -> 
                    android.widget.Toast.makeText(context, "Successfully imported $count items", android.widget.Toast.LENGTH_SHORT).show() 
                },
                onError = { err -> 
                    android.widget.Toast.makeText(context, "Import failed: $err", android.widget.Toast.LENGTH_SHORT).show() 
                }
            )
        }
    }

    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = "History Actions")
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        DropdownMenuItem(
            text = { Text("Export History") },
            onClick = {
                expanded = false
                val formatter = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", java.util.Locale.getDefault())
                val fileName = "fairspeed_history_${formatter.format(java.util.Date())}.json"
                exportLauncher.launch(fileName)
            }
        )
        DropdownMenuItem(
            text = { Text("Import History") },
            onClick = {
                expanded = false
                importLauncher.launch(arrayOf("application/json", "*/*"))
            }
        )
        DropdownMenuItem(
            text = { Text("Clear History") },
            onClick = {
                expanded = false
                showClearHistoryDialog = true
            }
        )
    }
    
    // Clear History confirmation dialog
    if (showClearHistoryDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear History") },
            text = { Text("Are you sure you want to clear all history? This cannot be undone.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.clearHistory()
                    showClearHistoryDialog = false
                }) {
                    Text("Clear")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: SpeedTestViewModel) {
    var selectedItem by remember { mutableIntStateOf(0) }
    var currentModal by remember { mutableStateOf<String?>(null) }

    // If a modal is open, show it instead of the main navigation
    if (currentModal != null) {
        when (currentModal) {
            "donate" -> DonateScreen(
                onBackPressed = { currentModal = null }
            )
            "contact" -> ContactUsScreen(
                onBackPressed = { currentModal = null }
            )
            "about" -> AboutScreen(
                onBackPressed = { currentModal = null }
            )
        }
        return
    }

    Scaffold(
        topBar = {
            if (selectedItem == 0) {
                TopAppBar(
                    title = { 
                        Text(
                            text = "FairSpeed",
                            fontWeight = FontWeight.Bold
                        ) 
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                currentModal = "donate"
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Donate",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            } else if (selectedItem == 1) {
                TopAppBar(
                    title = { Text("History") },
                    actions = {
                        ExportImportHistoryActions(viewModel = viewModel)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.PlayArrow, contentDescription = "Test", modifier = Modifier.size(28.dp)) },
                    selected = selectedItem == 0,
                    onClick = { selectedItem = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "History", modifier = Modifier.size(28.dp)) },
                    selected = selectedItem == 1,
                    onClick = { selectedItem = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings", modifier = Modifier.size(28.dp)) },
                    selected = selectedItem == 2,
                    onClick = { selectedItem = 2 }
                )
            }
        }
    ) { paddingValues ->
        val modifier = Modifier.padding(paddingValues)
        when (selectedItem) {
            0 -> SpeedTestScreen(viewModel, modifier)
            1 -> HistoryScreen(viewModel, modifier)
            2 -> SettingsScreen(
                modifier,
                onDonateClick = { currentModal = "donate" },
                onContactClick = { currentModal = "contact" },
                onAboutClick = { currentModal = "about" }
            )
        }
    }
}
