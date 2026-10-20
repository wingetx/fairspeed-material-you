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

package com.fairspeed.test.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fairspeed.test.presentation.ui.screen.SpeedTestScreen
import com.fairspeed.test.presentation.viewmodel.SpeedTestViewModel

// Navigation routes
sealed class Screen(val route: String) {
    object SpeedTest : Screen("speed_test")
    object History : Screen("history")
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation(
    viewModel: SpeedTestViewModel,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.SpeedTest.route
    ) {
        composable(Screen.SpeedTest.route) {
            SpeedTestScreen(viewModel = viewModel)
        }

        // TODO: Add HistoryScreen
        // composable(Screen.History.route) {
        //     HistoryScreen()
        // }

        // TODO: Add SettingsScreen
        // composable(Screen.Settings.route) {
        //     SettingsScreen()
        // }
    }
}
