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

// Added for FairSpeed Material You, 2026-10-20.
package com.fairspeed.test.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccentHexTest {
    @Test fun acceptsHashAndNormalizesCase() {
        assertEquals("367C89", normalizeAccentHex("#367c89"))
        assertEquals("ABCDEF", normalizeAccentHex("  abcDEF  "))
    }

    @Test fun rejectsInvalidColors() {
        for (bad in listOf("", "#12", "00FFFF00", "GGGGGG", "#12345G", "#12345#")) {
            assertNull("Should reject $bad", normalizeAccentHex(bad))
        }
    }
}
