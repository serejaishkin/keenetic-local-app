package com.keenetic.local.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.keenetic.local.ui.screens.*

sealed class Screen(val route: String, val title: String) {
    object Login : Screen("login", "Вход")
    object Dashboard : Screen("dashboard", "Статус")
    object Devices : Screen("devices", "Устройства")
    object WiFi : Screen("wifi", "Wi-Fi")
    object Terminal : Screen("terminal", "Терминал")
    object Settings : Screen("settings", "Настройки")
}

@Composable
fun KeeneticNavHost(navController: NavHostController, viewModel: RouterViewModel) {
    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen(viewModel = viewModel, onLoginSuccess = {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(viewModel = viewModel)
        }
        composable(Screen.Devices.route) {
            DevicesScreen(viewModel = viewModel)
        }
        composable(Screen.WiFi.route) {
            WiFiScreen(viewModel = viewModel)
        }
        composable(Screen.Terminal.route) {
            TerminalScreen(viewModel = viewModel)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(viewModel = viewModel, onLoggedOut = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            })
        }
    }
}