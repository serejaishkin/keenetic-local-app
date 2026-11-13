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

/**
 * Структура разделов, выровненная по группам веб-интерфейса KeeneticOS 5.x
 * (Статус / Интернет / Локальная сеть / Сетевые правила / Управление),
 * но разложенная по мобильным вкладкам: Главная, Устройства, Интернет, Сеть, Управление.
 */
object HubCatalog {
    /** Вкладка «Интернет» — как группа «Интернет» на сайте + связанные параметры подключения. */
    val internet: List<SectionCategory> = listOf(
        SectionCategory("Подключения", Icons.Default.Public, listOf(
            SectionItem("Кабель Ethernet", Screen.Internet.route, "Проводной интернет (ISP), WAN/LAN"),
            SectionItem("Интернет (подробно)", Screen.InternetDetailed.route, "IP, шлюз, DNS, скорость"),
            SectionItem("Мобильный интернет", Screen.Mobile.route, "USB-модемы и 4G/LTE"),
            SectionItem("Квота мобильного трафика", Screen.MobileTraffic.route, "Лимиты, SMS, отключение сети"),
            SectionItem("Wireless ISP (WISP)", Screen.WifiRepeater.route, "Подключение через соседнюю Wi-Fi сеть"),
            SectionItem("Другие подключения", Screen.OtherConnections.route, "WireGuard, L2TP, PPTP, ZeroTier, Proxy")
        )),
        SectionCategory("Приоритеты и качество", Icons.Default.SwapVert, listOf(
            SectionItem("Приоритеты подключений", Screen.Priorities.route, "Порядок и резервирование каналов"),
            SectionItem("IntelliQoS", Screen.IntelliQos.route, "Классификация и приоритизация трафика")
        )),
        SectionCategory("Параметры сети", Icons.Default.Dns, listOf(
            SectionItem("DNS", Screen.DnsSettings.route, "Серверы, DoH/DoT, перехват"),
            SectionItem("IPv6", Screen.Ipv6.route, "Адреса, префиксы, маршруты"),
            SectionItem("Динамический DNS", Screen.Ddns.route, "DDNS-провайдеры и обновления"),
            SectionItem("Доменное имя", Screen.Ddns.route, "Удалённый доступ и KeenDNS/DDNS")
        ))
    )

    /** Вкладка «Сеть» — «Локальная сеть» + «Сетевые правила» сайта. */
    val network: List<SectionCategory> = listOf(
        SectionCategory("Локальная сеть", Icons.Default.Home, listOf(
            SectionItem("Клиенты", Screen.Devices.route, "Подключённые устройства и политики"),
            SectionItem("Wi-Fi сети", Screen.WiFi.route, "Точки доступа 2.4 / 5 ГГц"),
            SectionItem("Общие параметры Wi-Fi", Screen.WifiSystem.route, "Каналы, мощность, роуминг"),
            SectionItem("Сегменты", Screen.LanSegments.route, "Подсети и изоляция клиентов"),
            SectionItem("Контроллер (Wi-Fi-система)", Screen.Mws.route, "Mesh/MWS из нескольких роутеров")
        )),
        SectionCategory("Wi-Fi: мониторинг и доступ", Icons.Default.Wifi, listOf(
            SectionItem("Монитор Wi-Fi", Screen.WifiMonitor.route, "Сканирование, загрузка каналов"),
            SectionItem("WPS", Screen.Wps.route, "Быстрое подключение по PIN"),
            SectionItem("Контроль доступа Wi-Fi", Screen.WifiAcl.route, "Чёрные и белые списки MAC")
        )),
        SectionCategory("Сетевые правила", Icons.Default.Rule, listOf(
            SectionItem("Интернет-фильтры", Screen.DnsSettings.route, "DNS-фильтрация и защита"),
            SectionItem("Контентная фильтрация", Screen.ContentFilter.route, "NextDNS, SafeDNS, SkyDNS"),
            SectionItem("Межсетевой экран", Screen.Firewall.route, "Правила фильтрации трафика"),
            SectionItem("Переадресация портов", Screen.PortForwarding.route, "NAT и виртуальные серверы"),
            SectionItem("Маршрутизация", Screen.StaticRoutes.route, "Статическая и доменная маршрутизация"),
            SectionItem("FQDN группы", Screen.ObjectGroup.route, "Списки доменов для фильтров/маршрутов"),
            SectionItem("UPnP / NAT-PMP", Screen.Upnp.route, "Автоматическая переадресация")
        )),
        SectionCategory("VPN", Icons.Default.VpnKey, listOf(
            SectionItem("VPN-сервер и клиенты", Screen.VpnAdvanced.route, "WireGuard, SSTP, OpenVPN, L2TP"),
            SectionItem("VPN-серверы (детально)", Screen.VpnServers.route, "WireGuard, L2TP, IKEv2, SSTP, IPsec")
        ))
    )

    /** Вкладка «Управление» — как группа «Управление» на сайте. */
    val management: List<SectionCategory> = listOf(
        SectionCategory("Система", Icons.Default.Settings, listOf(
            SectionItem("Настройки системы", Screen.SystemAdvanced.route, "Имя хоста, время, LED, NTP, бэкап"),
            SectionItem("KeeneticOS и обновление", Screen.Firmware.route, "Версия прошивки и обновления"),
            SectionItem("Пользователи и доступ", Screen.UserAccounts.route, "Учётные записи и права"),
            SectionItem("Безопасность системы", Screen.SystemAdvanced.route, "Пароли, политики доступа, блокировки"),
            SectionItem("Компоненты", Screen.Components.route, "Установка и управление компонентами")
        )),
        SectionCategory("Хранилище и приложения", Icons.Default.Apps, listOf(
            SectionItem("Накопители и устройства", Screen.UsbDevices.route, "Диски, USB, печать, файлы"),
            SectionItem("Приложения", Screen.Opkg.route, "OPKG и установленные приложения"),
            SectionItem("Менеджер пакетов OPKG", Screen.Opkg.route, "Диск и конфигурация OPKG"),
            SectionItem("Торрент-клиент", Screen.TorrentDetail.route, "Статус и настройки"),
            SectionItem("Сеть Windows (SMB)", Screen.Smb.route, "Служба SMB / CIFS"),
            SectionItem("Медиасервер (DLNA)", Screen.MediaServer.route, "Доступ по DLNA / UPnP"),
            SectionItem("Облачные сервисы", Screen.Cloud.route, "Keenetic Cloud и NDMP")
        )),
        SectionCategory("Диагностика и сервисы", Icons.Default.Build, listOf(
            SectionItem("Диагностика", Screen.Diagnostics.route, "Ping, Traceroute, DNS"),
            SectionItem("Диагностика кабеля", Screen.CableDiagnostics.route, "Длина и состояние кабелей"),
            SectionItem("Сетевые сервисы", Screen.SshSnmp.route, "SSH, SNMP, FTP, Telnet, Proxy"),
            SectionItem("Журнал событий", Screen.SystemLogs.route, "Системные события ndm"),
            SectionItem("Конфигурация (RCI / CLI)", Screen.Configuration.route, "Running-config и команды NDM")
        )),
        SectionCategory("Мониторинг (статус)", Icons.Default.Monitor, listOf(
            SectionItem("Системный монитор", Screen.SystemMonitor.route, "CPU, RAM, радио, интерфейсы"),
            SectionItem("Монитор трафика", Screen.TrafficMonitor.route, "Скорость на интерфейсах"),
            SectionItem("Монитор Wi-Fi", Screen.WifiMonitor.route, "Клиенты и загрузка каналов"),
            SectionItem("Мониторинг сети", Screen.NetworkMonitor.route, "Conntrack, ARP, IP-правила"),
            SectionItem("Список устройств", Screen.DeviceListDetailed.route, "Все подключённые устройства")
        )),
        SectionCategory("Инструменты для опытных", Icons.Default.Terminal, listOf(
            SectionItem("SSH-терминал", Screen.SshTerminal.route, "Команды CLI по SSH"),
            SectionItem("Снимок ответов RCI", Screen.RciCapture.route, "Пакетное чтение, без записи")
        ))
    )

    val all: List<SectionItem> get() = (internet + network + management).flatMap { it.items }.distinctBy { it.route to it.title }
}

@Composable
fun NetworkHubScreen(onNavigate: (String) -> Unit) =
    HubScreen("Интернет", "Подключения, приоритеты и параметры (как в веб-UI)", HubCatalog.internet, onNavigate, searchable = true)

@Composable
fun ProtectHubScreen(onNavigate: (String) -> Unit) =
    HubScreen("Сеть", "Локальная сеть и сетевые правила", HubCatalog.network, onNavigate, searchable = true)

@Composable
fun MoreHubScreen(onNavigate: (String) -> Unit) =
    HubScreen("Управление", "Система, сервисы, диагностика и мониторинг", HubCatalog.management, onNavigate, searchable = true)

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
