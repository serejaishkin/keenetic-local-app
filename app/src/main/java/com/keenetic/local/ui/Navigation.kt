package com.keenetic.local.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.keenetic.local.ui.screens.*

sealed class Screen(val route: String, val title: String) {
    data object Dashboard : Screen("dashboard", "Главная")
    data object Devices : Screen("devices", "Устройства")
    data object Internet : Screen("internet", "Интернет")
    data object WiFi : Screen("wifi", "Wi-Fi Сеть")
    data object AllSections : Screen("all_sections", "Все разделы")
    data object PortForwarding : Screen("port_forwarding", "Переадресация портов")
    data object Firewall : Screen("firewall", "Межсетевой экран")
    data object StaticRoutes : Screen("static_routes", "Маршрутизация")
    data object LanSegments : Screen("lan_segments", "LAN-сегменты")
    data object Mobile : Screen("mobile", "Мобильный интернет")
    data object UsbDevices : Screen("usb_devices", "Накопители и USB")
    data object UserAccounts : Screen("user_accounts", "Пользователи")
    data object SystemLogs : Screen("system_logs", "Журнал событий")
    data object Firmware : Screen("firmware", "KeeneticOS")
    data object Diagnostics : Screen("diagnostics", "Диагностика")
    data object DnsFilters : Screen("dns_filters", "DNS-фильтры")
    data object VpnAdvanced : Screen("vpn_advanced", "VPN-сервер")
    data object Configuration : Screen("configuration", "Конфигурация (RCI / CLI)")
}

@Composable
fun KeeneticNavHost(
    navController: NavHostController,
    viewModel: RouterViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                viewModel = viewModel,
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable(Screen.Devices.route) {
            DevicesScreen(viewModel = viewModel)
        }
        composable(Screen.Internet.route) {
            InternetScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.WiFi.route) {
            WiFiScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.AllSections.route) {
            AllSectionsScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable(Screen.PortForwarding.route) {
            PortForwardingScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.Firewall.route) {
            FirewallScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.StaticRoutes.route) {
            StaticRoutesScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.LanSegments.route) {
            LanSegmentsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.Mobile.route) {
            MobileScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.UsbDevices.route) {
            UsbStorageScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.UserAccounts.route) {
            UserAccountsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.SystemLogs.route) {
            SystemLogsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.Firmware.route) {
            FirmwareScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.Diagnostics.route) {
            DiagnosticsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.DnsFilters.route) {
            DnsFiltersScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.VpnAdvanced.route) {
            VpnAdvancedScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.Configuration.route) {
            ConfigurationScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}
