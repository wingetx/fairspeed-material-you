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

package com.fairspeed.test.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.fairspeed.test.R
import com.fairspeed.test.domain.model.SignalInfo
import com.fairspeed.test.domain.model.SignalType
import com.fairspeed.test.domain.model.SpeedTestResult
import com.fairspeed.test.presentation.viewmodel.SpeedTestViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(viewModel: SpeedTestViewModel, modifier: Modifier = Modifier) {
    val fullHistory by viewModel.testHistory.collectAsState()
    var itemToDelete by remember { mutableStateOf<SpeedTestResult?>(null) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    
    // Lazy loading implementation
    val lazyListState = rememberLazyListState()
    val itemsToShow = remember { mutableStateOf(20) } // Initial load: 20 items
    
    // Load more items when scrolling to bottom
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
            
            lastVisibleItem > totalItems - 3 && itemsToShow.value < fullHistory.size
        }
    }
    
    // Auto-load more items when reaching bottom
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && itemsToShow.value < fullHistory.size) {
            itemsToShow.value = minOf(itemsToShow.value + 20, fullHistory.size)
        }
    }
    
    val displayedHistory = fullHistory.take(itemsToShow.value)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {


        if (fullHistory.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No history available",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(displayedHistory) { result ->
                    HistoryScreenItemCard(
                        result = result,
                        modifier = Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = { itemToDelete = result }
                        )
                    )
                }
                
                // Loading indicator when more items are available
                if (itemsToShow.value < fullHistory.size) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (shouldLoadMore.value) {
                                Text("Loading more...")
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Delete item confirmation dialog
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete item") },
            text = { Text("Are you sure you want to delete this item from history?") },
            confirmButton = {
                TextButton(onClick = {
                    itemToDelete?.let { viewModel.deleteHistoryItem(it) }
                    itemToDelete = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Clear history confirmation dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear History") },
            text = { Text("Are you sure you want to clear all history? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    showClearHistoryDialog = false
                    itemsToShow.value = 20 // Reset to initial load
                }) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun HistoryScreenItemCard(result: SpeedTestResult, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        val dateFormat = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT, Locale.getDefault())
        val dateString = dateFormat.format(Date(result.timestamp))
        val isWifi = result.connectionType.lowercase().contains("wifi")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateString,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isWifi) Icons.Default.Home else Icons.Default.Phone,
                    contentDescription = result.connectionType,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = result.connectionType,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (result.isp.isNotBlank() && result.isp != "Unknown" && result.isp != "Unknown ISP") {
                Text(
                    text = result.isp,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }
            result.signalInfo?.let { signal ->
                SignalIndicator(signal = signal)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HistoryScreenMetric(label = stringResource(R.string.download_speed), value = String.format("%.1f", result.downloadSpeed), unit = "Mbps", color = Color(0xFF4CAF50))
            HistoryScreenMetric(label = stringResource(R.string.upload_speed), value = String.format("%.1f", result.uploadSpeed), unit = "Mbps", color = Color(0xFFF57C00))
            HistoryScreenMetric(label = stringResource(R.string.ping), value = String.format("%.0f", result.ping), unit = "ms", color = null)
            HistoryScreenMetric(label = stringResource(R.string.jitter), value = String.format("%.0f", result.jitter), unit = "ms", color = null)
        }
    }
}

@Composable
fun HistoryScreenMetric(label: String, value: String, unit: String, color: Color?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color ?: MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = unit,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}

@Composable
private fun SignalIndicator(signal: SignalInfo) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SignalBars(level = signal.level)
        if (signal.type == SignalType.CELLULAR && signal.dbm != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${signal.dbm} dBm",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SignalBars(level: Int, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    val barHeights = listOf(5.dp, 8.dp, 11.dp, 14.dp)
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        barHeights.forEachIndexed { index, barHeight ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (index < level.coerceIn(0, 4)) color else color.copy(alpha = 0.25f))
            )
        }
    }
}
