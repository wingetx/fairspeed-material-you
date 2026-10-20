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

package com.fairspeed.test.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.fairspeed.test.domain.model.AppTheme
import com.fairspeed.test.presentation.ui.screen.MainScreen
import com.fairspeed.test.presentation.viewmodel.SettingsViewModel
import com.fairspeed.test.presentation.ui.theme.fairSpeedColorScheme
import com.fairspeed.test.presentation.viewmodel.SpeedTestViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val appTheme by settingsViewModel.appTheme.collectAsState()
            val isDarkTheme = when (appTheme) {
                AppTheme.DARK -> true
                AppTheme.LIGHT -> false
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            val accentMode by settingsViewModel.accentMode.collectAsState()
            val customAccentHex by settingsViewModel.customAccentHex.collectAsState()
            val context = LocalContext.current
            val colorScheme = fairSpeedColorScheme(context, isDarkTheme, accentMode, customAccentHex)

            SideEffect {
                // Match system bar icon contrast to the selected app appearance.
                val bars = WindowInsetsControllerCompat(window, window.decorView)
                bars.isAppearanceLightStatusBars = !isDarkTheme
                bars.isAppearanceLightNavigationBars = !isDarkTheme
            }

            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: SpeedTestViewModel = hiltViewModel()
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
