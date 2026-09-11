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

    // Phase 5: Новые разделы
    data object SystemAdvanced : Screen("system_advanced", "Системные настройки")
    data object Components : Screen("components", "Компоненты")
    data object SshSnmp : Screen("ssh_snmp", "Сетевые сервисы")
    data object Upnp : Screen("upnp", "UPnP / NAT-PMP")
    data object VpnServers : Screen("vpn_servers", "VPN Серверы")
    data object Ipv6 : Screen("ipv6", "IPv6")
    data object Ddns : Screen("ddns", "Динамический DNS")
    data object ContentFilter : Screen("content_filter", "Контентная фильтрация")
    data object TorrentDetail : Screen("torrent_detail", "Торрент-клиент")
    data object Cloud : Screen("cloud", "Облачные сервисы")
    data object NetworkMonitor : Screen("network_monitor", "Мониторинг сети")
    data object DeviceListDetailed : Screen("device_list_detailed", "Список устройств")
    data object Wps : Screen("wps", "WPS")
    data object CableDiagnostics : Screen("cable_diagnostics", "Диагностика кабеля")
    data object InternetDetailed : Screen("internet_detailed", "Интернет (подробно)")
    data object Mws : Screen("mws", "Mesh Wi-Fi")
    data object MobileTraffic : Screen("mobile_traffic", "Квота мобильного трафика")
    data object OtherConnections : Screen("other_connections", "Другие подключения")
    data object Priorities : Screen("priorities", "Приоритеты подключений")
    data object WifiSystem : Screen("wifi_system", "Общие параметры Wi-Fi")
    data object ObjectGroup : Screen("object_group", "FQDN группы")
    data object SystemMonitor : Screen("system_monitor", "Системный монитор")
    data object IntelliQos : Screen("intelliqos", "IntelliQoS")
    data object WifiAcl : Screen("wifi_acl", "Контроль доступа Wi-Fi")
    data object TrafficMonitor : Screen("traffic_monitor", "Монитор трафика")
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
        composable(Screen.SystemAdvanced.route) {
            SystemAdvancedScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.Components.route) {
            ComponentsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.SshSnmp.route) {
            SshSnmpScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.Upnp.route) {
            UpnpScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.VpnServers.route) {
            VpnServersScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.Ipv6.route) {
            Ipv6Screen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.Ddns.route) {
            DdnsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.ContentFilter.route) {
            ContentFilterScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.TorrentDetail.route) {
            TorrentDetailScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.Cloud.route) {
            CloudScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.NetworkMonitor.route) {
            NetworkMonitorScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.DeviceListDetailed.route) {
            DeviceListDetailedScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.Wps.route) {
            WpsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.CableDiagnostics.route) {
            CableDiagnosticsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.InternetDetailed.route) {
            InternetDetailedScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.Mws.route) {
            MwsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.MobileTraffic.route) {
            MobileTrafficScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.OtherConnections.route) {
            OtherConnectionsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.Priorities.route) {
            PrioritiesScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.WifiSystem.route) {
            WifiSystemScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.ObjectGroup.route) {
            ObjectGroupScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.SystemMonitor.route) {
            SystemMonitorScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.IntelliQos.route) {
            IntelliQosScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.WifiAcl.route) {
            WifiAclScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.TrafficMonitor.route) {
            TrafficMonitorScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}
