package com.keenetic.local.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.Screen
import com.keenetic.local.ui.components.RowDivider
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SectionUi
import com.keenetic.local.ui.theme.KeeneticColors

/** Новая структура разделов (макет: 5 вкладок). Каждый пункт ведёт на существующий экран. */
object HubCatalog {
    val network: List<SectionCategory> = listOf(
        SectionCategory("Wi-Fi", Icons.Default.Wifi, listOf(
            SectionItem("Wi-Fi сети", Screen.WiFi.route, "Точки доступа 2.4 / 5 ГГц"),
            SectionItem("Общие параметры Wi-Fi", Screen.WifiSystem.route, "Каналы, мощность, роуминг, изоляция"),
            SectionItem("Монитор Wi-Fi", Screen.WifiMonitor.route, "Сканирование, загрузка каналов"),
            SectionItem("WPS", Screen.Wps.route, "Быстрое подключение по PIN"),
            SectionItem("Mesh Wi-Fi (MWS)", Screen.Mws.route, "Mesh-сеть из нескольких роутеров"),
            SectionItem("Подключение через соседнюю Wi-Fi (WISP)", Screen.WifiRepeater.route, "Репитер, клиентский режим")
        )),
        SectionCategory("Интернет", Icons.Default.Public, listOf(
            SectionItem("Проводной интернет (ISP)", Screen.Internet.route, "WAN/LAN и интерфейсы"),
            SectionItem("Интернет (подробно)", Screen.InternetDetailed.route, "IP, шлюз, DNS, скорость"),
            SectionItem("Приоритеты подключений", Screen.Priorities.route, "Порядок интернет-каналов"),
            SectionItem("IntelliQoS", Screen.IntelliQos.route, "Приоритизация трафика"),
            SectionItem("DNS", Screen.DnsSettings.route, "Серверы, DoH/DoT, перехват"),
            SectionItem("IPv6", Screen.Ipv6.route, "Адреса, префиксы, маршруты"),
            SectionItem("Мобильный интернет", Screen.Mobile.route, "USB-модемы и 4G/LTE"),
            SectionItem("Квота мобильного трафика", Screen.MobileTraffic.route, "Лимиты, SMS, отключение сети"),
            SectionItem("Другие подключения", Screen.OtherConnections.route, "WireGuard, L2TP, PPTP, ZeroTier, Proxy")
        )),
        SectionCategory("Маршрутизация", Icons.Default.AltRoute, listOf(
            SectionItem("Переадресация портов", Screen.PortForwarding.route, "NAT и виртуальные серверы"),
            SectionItem("Статическая и доменная маршрутизация", Screen.StaticRoutes.route, "Маршруты, DNS-маршруты"),
            SectionItem("LAN сегменты", Screen.LanSegments.route, "Подсети и изоляция клиентов"),
            SectionItem("UPnP / NAT-PMP", Screen.Upnp.route, "Автоматическая переадресация"),
            SectionItem("Динамический DNS", Screen.Ddns.route, "DDNS-провайдеры")
        ))
    )

    val protect: List<SectionCategory> = listOf(
        SectionCategory("Фильтрация", Icons.Default.FilterAlt, listOf(
            SectionItem("Контентная фильтрация", Screen.ContentFilter.route, "NextDNS, SafeDNS, SkyDNS"),
            SectionItem("FQDN группы", Screen.ObjectGroup.route, "Списки доменов для фильтров и маршрутов")
        )),
        SectionCategory("Доступ", Icons.Default.Lock, listOf(
            SectionItem("Межсетевой экран", Screen.Firewall.route, "Правила фильтрации трафика"),
            SectionItem("Контроль доступа Wi-Fi", Screen.WifiAcl.route, "Чёрные и белые списки MAC")
        )),
        SectionCategory("VPN", Icons.Default.VpnKey, listOf(
            SectionItem("VPN-сервер и клиенты", Screen.VpnAdvanced.route, "WireGuard, SSTP, OpenVPN, L2TP"),
            SectionItem("VPN-серверы (детально)", Screen.VpnServers.route, "WireGuard, L2TP, IKEv2, SSTP, IPsec")
        ))
    )

    val more: List<SectionCategory> = listOf(
        SectionCategory("Система", Icons.Default.Settings, listOf(
            SectionItem("Системные настройки", Screen.SystemAdvanced.route, "Имя хоста, время, LED, NTP, бэкап"),
            SectionItem("KeeneticOS и обновление", Screen.Firmware.route, "Версия прошивки"),
            SectionItem("Пользователи и доступ", Screen.UserAccounts.route, "Учётные записи и права"),
            SectionItem("Конфигурация (RCI / CLI)", Screen.Configuration.route, "Running-config и команды NDM"),
            SectionItem("Компоненты", Screen.Components.route, "Установка и управление"),
            SectionItem("Менеджер пакетов OPKG", Screen.Opkg.route, "Диск и конфигурация OPKG")
        )),
        SectionCategory("Сервисы", Icons.Default.Apps, listOf(
            SectionItem("USB и накопители", Screen.UsbDevices.route, "Диски, SMB, FTP, DLNA"),
            SectionItem("Торрент-клиент", Screen.TorrentDetail.route, "Статус и настройки"),
            SectionItem("Сеть Windows (SMB)", Screen.Smb.route, "Служба SMB / CIFS"),
            SectionItem("Медиасервер (DLNA)", Screen.MediaServer.route, "Доступ по DLNA / UPnP"),
            SectionItem("Сетевые сервисы", Screen.SshSnmp.route, "SSH, SNMP, FTP, Telnet, Proxy"),
            SectionItem("Облачные сервисы", Screen.Cloud.route, "Keenetic Cloud и NDMP")
        )),
        SectionCategory("Мониторинг", Icons.Default.Monitor, listOf(
            SectionItem("Системный монитор", Screen.SystemMonitor.route, "CPU, RAM, радио, интерфейсы"),
            SectionItem("Монитор трафика", Screen.TrafficMonitor.route, "Скорость на интерфейсах"),
            SectionItem("Мониторинг сети", Screen.NetworkMonitor.route, "Conntrack, ARP, IP-правила"),
            SectionItem("Список устройств", Screen.DeviceListDetailed.route, "Все подключённые устройства"),
            SectionItem("Журнал событий", Screen.SystemLogs.route, "Системные события ndm"),
            SectionItem("Диагностика сети", Screen.Diagnostics.route, "Ping, Traceroute, DNS"),
            SectionItem("Диагностика кабеля", Screen.CableDiagnostics.route, "Длина и состояние кабелей")
        )),
        SectionCategory("Инструменты для опытных", Icons.Default.Terminal, listOf(
            SectionItem("SSH-терминал", Screen.SshTerminal.route, "Команды CLI по SSH"),
            SectionItem("Снимок ответов RCI", Screen.RciCapture.route, "Пакетное чтение, без записи")
        ))
    )

    val all: List<SectionItem> get() = (network + protect + more).flatMap { it.items }
}

@Composable
fun NetworkHubScreen(onNavigate: (String) -> Unit) =
    HubScreen("Сеть", "Wi-Fi, подключение и маршрутизация", HubCatalog.network, onNavigate)

@Composable
fun ProtectHubScreen(onNavigate: (String) -> Unit) =
    HubScreen("Защита", "Фильтры, доступ и VPN", HubCatalog.protect, onNavigate)

@Composable
fun MoreHubScreen(onNavigate: (String) -> Unit) =
    HubScreen("Ещё", "Система, сервисы и инструменты", HubCatalog.more, onNavigate, searchable = true)

@Composable
private fun HubScreen(
    title: String,
    subtitle: String,
    categories: List<SectionCategory>,
    onNavigate: (String) -> Unit,
    searchable: Boolean = false
) {
    var query by remember { mutableStateOf("") }
    val q = query.trim()

    SectionScaffold(title = title, subtitle = subtitle, onBack = null) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (searchable) {
                item(key = "search") {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Поиск по всем разделам") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                }
            }
            if (searchable && q.isNotEmpty()) {
                val found = HubCatalog.all.filter {
                    it.title.contains(q, ignoreCase = true) || it.subtitle.contains(q, ignoreCase = true)
                }
                item(key = "found") {
                    SectionCard(title = "Найдено", icon = Icons.Default.Search, subtitle = "${found.size} пунктов") {
                        if (found.isEmpty()) Text("Ничего не найдено", color = KeeneticColors.TextSecondary)
                        found.forEachIndexed { i, it ->
                            HubRow(it, onNavigate)
                            if (i < found.lastIndex) RowDivider()
                        }
                    }
                }
            } else {
                items(categories) { category ->
                    SectionCard(title = category.title, icon = category.icon, subtitle = "${category.items.size} пунктов") {
                        category.items.forEachIndexed { i, item ->
                            HubRow(item, onNavigate)
                            if (i < category.items.lastIndex) RowDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HubRow(item: SectionItem, onNavigate: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SectionUi.MinTapTarget.dp)
            .clickable { onNavigate(item.route) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.bodyLarge, color = KeeneticColors.TextPrimary)
            Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = KeeneticColors.TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}
