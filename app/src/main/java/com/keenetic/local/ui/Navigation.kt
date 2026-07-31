package com.keenetic.local.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.keenetic.local.ui.screens.*

sealed class Screen(val route: String, val title: String) {
    object Login : Screen("login", "Вход")
    object Dashboard : Screen("dashboard", "Статус")
    object Internet : Screen("internet", "Интернет")
    object Devices : Screen("devices", "Устройства")
    object WiFi : Screen("wifi", "Wi-Fi")
    object Terminal : Screen("terminal", "Терминал")
    object Settings : Screen("settings", "Настройки приложения")
    object AllSections : Screen("all_sections", "Разделы")
    object Apps : Screen("apps", "Приложения")
    object SystemSettings : Screen("system_settings", "Настройки системы")
    object InternetSection : Screen("internet_section", "Интернет")
    object WiFiSection : Screen("wifi_section", "Wi-Fi")
    object NetworkSection : Screen("network_section", "Сеть")
    object ManagementSection : Screen("management_section", "Управление")
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
        composable(Screen.Internet.route) {
            InternetScreen(viewModel = viewModel)
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
            SettingsScreen(
                viewModel = viewModel,
                onLoggedOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onOpenApps = { navController.navigate(Screen.Apps.route) },
                onOpenSystemSettings = { navController.navigate(Screen.SystemSettings.route) }
            )
        }
        composable(Screen.AllSections.route) {
            AllSectionsScreen(navController = navController)
        }
        composable(Screen.Apps.route) {
            AppsScreen(viewModel = viewModel)
        }
        composable(Screen.SystemSettings.route) {
            SystemSettingsScreen(
                viewModel = viewModel,
                onOpenAppSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.InternetSection.route) {
            SectionCategoryScreen(
                navController = navController,
                title = "Интернет",
                description = "Подключения, каналы связи и приоритеты",
                items = listOf(
                    SectionItem("Кабель Ethernet", Screen.Internet.route, "WAN/LAN и состояние интерфейсов"),
                    SectionItem("Mobile", soon = true, subtitle = "Модемы и мобильные соединения"),
                    SectionItem("Wireless ISP", Screen.WiFi.route, "Беспроводные провайдеры"),
                    SectionItem("Приоритеты подключений", soon = true, subtitle = "Маршрутизация по каналам")
                )
            )
        }
        composable(Screen.WiFiSection.route) {
            SectionCategoryScreen(
                navController = navController,
                title = "Wi-Fi",
                description = "Точки доступа, клиенты и сегменты",
                items = listOf(
                    SectionItem("Точки доступа", Screen.WiFi.route, "SSID, безопасность, состояние"),
                    SectionItem("Списки клиентов", Screen.Devices.route, "Подключённые устройства"),
                    SectionItem("Сегменты", soon = true, subtitle = "LAN-сегменты и изоляция"),
                    SectionItem("Wi-Fi-система", Screen.WiFi.route, "Режимы и характеристики WLAN")
                )
            )
        }
        composable(Screen.NetworkSection.route) {
            SectionCategoryScreen(
                navController = navController,
                title = "Сеть",
                description = "Правила, маршрутизация и доступ",
                items = listOf(
                    SectionItem("Маршрутизация", Screen.Devices.route, "Политики и статические маршруты"),
                    SectionItem("Переадресация портов", soon = true, subtitle = "Внешние и внутренние порты"),
                    SectionItem("Межсетевой экран", soon = true, subtitle = "Правила безопасности"),
                    SectionItem("DNS-фильтры", Screen.Settings.route, "DNS и блокировки")
                )
            )
        }
        composable(Screen.ManagementSection.route) {
            SectionCategoryScreen(
                navController = navController,
                title = "Управление",
                description = "Система, приложения и диагностика",
                items = listOf(
                    SectionItem("Настройки системы", Screen.SystemSettings.route, "VPN, DoH, расписания"),
                    SectionItem("Приложения", Screen.Apps.route, "OPKG, пакеты и сервисы"),
                    SectionItem("Диагностика", Screen.Terminal.route, "Команды и отладка"),
                    SectionItem("Пользователи и доступ", soon = true, subtitle = "Учетные записи и права")
                )
            )
        }
    }
}