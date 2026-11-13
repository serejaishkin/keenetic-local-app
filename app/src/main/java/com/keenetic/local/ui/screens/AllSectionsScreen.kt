package com.keenetic.local.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.Screen
import com.keenetic.local.ui.components.RowDivider
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SectionUi
import com.keenetic.local.ui.components.SubGroupHeader
import com.keenetic.local.ui.theme.KeeneticColors

data class SectionItem(
    val title: String,
    val route: String,
    val subtitle: String
)

data class SectionCategory(
    val title: String,
    val icon: ImageVector,
    val items: List<SectionItem>
)

@Composable
fun AllSectionsScreen(
    onNavigate: (String) -> Unit
) {
    val categories = listOf(
        SectionCategory(
            title = "Статус (Главная)",
            icon = Icons.Default.Dashboard,
            items = listOf(
                SectionItem("Системный монитор", Screen.SystemMonitor.route, "CPU, RAM, радио, интерфейсы"),
                SectionItem("Монитор трафика", Screen.TrafficMonitor.route, "Скорость на интерфейсах"),
                SectionItem("Монитор Wi-Fi", Screen.WifiMonitor.route, "Клиенты и загрузка каналов")
            )
        ),
        SectionCategory(
            title = "Интернет",
            icon = Icons.Default.Public,
            items = listOf(
                SectionItem("Кабель Ethernet", Screen.Internet.route, "Проводной интернет (ISP), WAN/LAN"),
                SectionItem("Интернет (подробно)", Screen.InternetDetailed.route, "IP, шлюз, DNS, скорость"),
                SectionItem("Мобильный интернет", Screen.Mobile.route, "USB-модемы и 4G/LTE"),
                SectionItem("Wireless ISP (WISP)", Screen.WifiRepeater.route, "Подключение через соседнюю Wi-Fi сеть"),
                SectionItem("Другие подключения", Screen.OtherConnections.route, "WireGuard, L2TP, PPTP, ZeroTier, Proxy"),
                SectionItem("Приоритеты подключений", Screen.Priorities.route, "Порядок и резервирование каналов"),
                SectionItem("IntelliQoS", Screen.IntelliQos.route, "Приоритизация трафика"),
                SectionItem("DNS", Screen.DnsSettings.route, "Серверы, DoH/DoT, перехват"),
                SectionItem("IPv6", Screen.Ipv6.route, "Адреса, префиксы, маршруты"),
                SectionItem("Квота мобильного трафика", Screen.MobileTraffic.route, "Лимиты, SMS, отключение")
            )
        ),
        SectionCategory(
            title = "Локальная сеть",
            icon = Icons.Default.Home,
            items = listOf(
                SectionItem("Клиенты", Screen.Devices.route, "Подключённые устройства и политики"),
                SectionItem("Wi-Fi сети", Screen.WiFi.route, "Точки доступа 2.4 / 5 ГГц"),
                SectionItem("Общие параметры Wi-Fi", Screen.WifiSystem.route, "Каналы, мощность, роуминг"),
                SectionItem("Сегменты", Screen.LanSegments.route, "Подсети и изоляция клиентов"),
                SectionItem("Контроллер (Wi-Fi-система)", Screen.Mws.route, "Mesh/MWS из нескольких роутеров"),
                SectionItem("WPS", Screen.Wps.route, "Быстрое подключение по PIN")
            )
        ),
        SectionCategory(
            title = "Сетевые правила",
            icon = Icons.Default.Rule,
            items = listOf(
                SectionItem("Интернет-фильтры", Screen.DnsSettings.route, "DNS-фильтрация и защита"),
                SectionItem("Контентная фильтрация", Screen.ContentFilter.route, "NextDNS, SafeDNS, SkyDNS"),
                SectionItem("Межсетевой экран", Screen.Firewall.route, "Правила фильтрации трафика"),
                SectionItem("Переадресация портов", Screen.PortForwarding.route, "NAT и виртуальные серверы"),
                SectionItem("Маршрутизация", Screen.StaticRoutes.route, "Статическая и доменная маршрутизация"),
                SectionItem("Доменное имя", Screen.Ddns.route, "Удалённый доступ и KeenDNS/DDNS"),
                SectionItem("Контроль доступа Wi-Fi", Screen.WifiAcl.route, "Чёрные и белые списки MAC"),
                SectionItem("FQDN группы", Screen.ObjectGroup.route, "Списки доменов"),
                SectionItem("UPnP / NAT-PMP", Screen.Upnp.route, "Автоматическая переадресация"),
                SectionItem("VPN-сервер и клиенты", Screen.VpnAdvanced.route, "WireGuard, SSTP, OpenVPN, L2TP")
            )
        ),
        SectionCategory(
            title = "Управление",
            icon = Icons.Default.Settings,
            items = listOf(
                SectionItem("Настройки системы", Screen.SystemAdvanced.route, "Имя хоста, время, LED, NTP, бэкап"),
                SectionItem("Накопители и устройства", Screen.UsbDevices.route, "Диски, USB, печать, файлы"),
                SectionItem("Приложения / OPKG", Screen.Opkg.route, "Пакеты и установленные приложения"),
                SectionItem("Пользователи и доступ", Screen.UserAccounts.route, "Учётные записи и права"),
                SectionItem("KeeneticOS и обновление", Screen.Firmware.route, "Версия прошивки и обновления"),
                SectionItem("Диагностика", Screen.Diagnostics.route, "Ping, Traceroute, DNS"),
                SectionItem("Сетевые сервисы", Screen.SshSnmp.route, "SSH, SNMP, FTP, Telnet, Proxy"),
                SectionItem("Журнал событий", Screen.SystemLogs.route, "Системные события ndm"),
                SectionItem("Конфигурация (RCI / CLI)", Screen.Configuration.route, "Running-config и команды NDM")
            )
        )
    )

    SectionScaffold(
        title = "Все разделы",
        subtitle = "Каталог настроек роутера",
        onBack = null
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categories) { category ->
                SectionCard(
                    title = category.title,
                    icon = category.icon,
                    subtitle = "${category.items.size} пунктов"
                ) {
                    SubGroupHeader(title = "Пункты раздела", count = category.items.size)
                    RowDivider()
                    category.items.forEachIndexed { index, item ->
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
                                Text(
                                    item.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = KeeneticColors.TextPrimary
                                )
                                Text(
                                    item.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KeeneticColors.TextSecondary
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = KeeneticColors.TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (index < category.items.lastIndex) RowDivider()
                    }
                }
            }
        }
    }
}
