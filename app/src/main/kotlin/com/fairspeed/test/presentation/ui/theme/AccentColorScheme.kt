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

// Added for the FairSpeed Material You fork, 2026-10-20.
package com.fairspeed.test.presentation.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.fairspeed.test.domain.model.AccentMode
import com.fairspeed.test.domain.model.normalizeAccentHex

/** App-specific colors never change the system's Wallpaper & style setting. */
fun accentPreview(mode: AccentMode, customHex: String = "367C89"): Color = when (mode) {
    AccentMode.MATERIAL_YOU -> Color(0xFF8BA5B8)
    AccentMode.TEAL -> Color(0xFF367C89)
    AccentMode.BLUE -> Color(0xFF526F9B)
    AccentMode.PURPLE -> Color(0xFF79629C)
    AccentMode.SAGE -> Color(0xFF587968)
    AccentMode.SLATE -> Color(0xFF626F7B)
    AccentMode.CUSTOM -> {
        val hex = normalizeAccentHex(customHex)
        if (hex != null) Color(android.graphics.Color.parseColor("#$hex"))
        else Color(0xFF367C89)
    }
}

/** Guarantee readable text/icons even with very bright or very dark custom colors. */
private fun readableOn(color: Color): Color = if (color.luminance() > 0.18f) Color.Black else Color.White

@Composable
fun fairSpeedColorScheme(
    context: Context,
    dark: Boolean,
    mode: AccentMode,
    customHex: String
): ColorScheme {
    if (mode == AccentMode.MATERIAL_YOU) {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
            dark -> darkColorScheme()
            else -> lightColorScheme()
        }
    }

    val chosen = accentPreview(mode, customHex)
    val primary = if (dark) lerp(chosen, Color.White, 0.38f) else chosen
    val onPrimary = readableOn(primary)
    val container = if (dark) lerp(chosen, Color.Black, 0.65f) else lerp(chosen, Color.White, 0.77f)
    val onContainer = readableOn(container)
    return if (dark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = container,
            onPrimaryContainer = onContainer,
            secondary = primary,
            onSecondary = onPrimary,
            secondaryContainer = container,
            onSecondaryContainer = onContainer,
            tertiary = primary,
            onTertiary = onPrimary,
            background = Color(0xFF121212),
            onBackground = Color(0xFFE8EAEC),
            surface = Color(0xFF1C1D1E),
            onSurface = Color(0xFFE8EAEC),
            surfaceVariant = Color(0xFF2B2D30),
            onSurfaceVariant = Color(0xFFCFD1D4)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = container,
            onPrimaryContainer = onContainer,
            secondary = primary,
            onSecondary = onPrimary,
            secondaryContainer = container,
            onSecondaryContainer = onContainer,
            tertiary = primary,
            onTertiary = onPrimary,
            background = Color(0xFFFAFAFA),
            onBackground = Color(0xFF242628),
            surface = Color.White,
            onSurface = Color(0xFF242628),
            surfaceVariant = Color(0xFFEBEDF0),
            onSurfaceVariant = Color(0xFF4D535B)
        )
    }
}
