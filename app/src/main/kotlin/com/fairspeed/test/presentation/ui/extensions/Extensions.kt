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

package com.fairspeed.test.presentation.ui.extensions

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Color utilities
fun Color.lighten(factor: Float = 0.2f): Color {
    return this.copy(
        red = (red + (1 - red) * factor).coerceIn(0f, 1f),
        green = (green + (1 - green) * factor).coerceIn(0f, 1f),
        blue = (blue + (1 - blue) * factor).coerceIn(0f, 1f)
    )
}

fun Color.darken(factor: Float = 0.2f): Color {
    return this.copy(
        red = (red * (1 - factor)).coerceIn(0f, 1f),
        green = (green * (1 - factor)).coerceIn(0f, 1f),
        blue = (blue * (1 - factor)).coerceIn(0f, 1f)
    )
}

// Format utilities
fun Double.formatSpeed(decimals: Int = 1): String {
    return String.format("%.${decimals}f", this)
}

fun Long.formatTimestamp(): String {
    return java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        .format(java.util.Date(this))
}

// Speed rating
fun getSpeedRating(speedMbps: Double): String {
    return when {
        speedMbps >= 100 -> "Excellent"
        speedMbps >= 50 -> "Very Good"
        speedMbps >= 25 -> "Good"
        speedMbps >= 10 -> "Medium"
        speedMbps >= 5 -> "Fair"
        else -> "Poor"
    }
}

fun getSpeedColor(speedMbps: Double): Color {
    return when {
        speedMbps >= 100 -> Color(0xFF4CAF50) // Green
        speedMbps >= 50 -> Color(0xFF8BC34A)  // Light Green
        speedMbps >= 25 -> Color(0xFFFFC107) // Amber
        speedMbps >= 10 -> Color(0xFFFF9800) // Orange
        speedMbps >= 5 -> Color(0xFFFF7043)  // Deep Orange
        else -> Color(0xFFE53935)             // Red
    }
}
