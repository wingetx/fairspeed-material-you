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
package com.fairspeed.test.domain.model

/** Accent selection is independent of the app's light/dark appearance. */
enum class AccentMode(val displayName: String) {
    MATERIAL_YOU("Material You"),
    TEAL("Deep teal"),
    BLUE("Ocean blue"),
    PURPLE("Muted violet"),
    SAGE("Sage green"),
    SLATE("Slate gray"),
    CUSTOM("Custom hex color")
}

/** Returns an uppercase six-character RGB code or null for invalid input. */
fun normalizeAccentHex(input: String): String? {
    val normalized = input.trim().removePrefix("#").uppercase(java.util.Locale.ROOT)
    return normalized.takeIf { it.matches(Regex("[0-9A-F]{6}")) }
}
