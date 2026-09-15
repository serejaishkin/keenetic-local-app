package com.keenetic.local.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
            title = "Интернет и подключение",
            icon = Icons.Default.Public,
            items = listOf(
                SectionItem("Проводной интернет (ISP)", Screen.Internet.route, "WAN/LAN и сетевые интерфейсы"),
                SectionItem("Интернет (подробно)", Screen.InternetDetailed.route, "IP, шлюз, DNS, скорость"),
                SectionItem("DNS / Secure DNS", Screen.DnsSettings.route, "Серверы, DoH/DoT, перехват DNS"),
                SectionItem("Wi-Fi сети", Screen.WiFi.route, "Беспроводные точки доступа 2.4/5 GHz"),
                SectionItem("Общие параметры Wi-Fi", Screen.WifiSystem.route, "Каналы, мощность, роуминг, изоляция"),
                SectionItem("Монитор Wi-Fi", Screen.WifiMonitor.route, "Сканирование, загрузка каналов, клиенты"),
                SectionItem("WPS", Screen.Wps.route, "Быстрое подключение по PIN"),
                SectionItem("Mesh Wi-Fi (MWS)", Screen.Mws.route, "Mesh-сеть из нескольких роутеров"),
                SectionItem("Мобильный интернет", Screen.Mobile.route, "USB-модемы и 4G/LTE"),
                SectionItem("Квота мобильного трафика", Screen.MobileTraffic.route, "Лимиты, SMS, отключение сети"),
                SectionItem("Другие подключения", Screen.OtherConnections.route, "WireGuard, L2TP, PPTP, ZeroTier, Proxy"),
                SectionItem("Приоритеты подключений", Screen.Priorities.route, "Порядок интернет-каналов"),
                SectionItem("IntelliQoS", Screen.IntelliQos.route, "Классификация и приоритизация трафика"),
                SectionItem("VPN-сервер и клиенты", Screen.VpnAdvanced.route, "WireGuard, SSTP, OpenVPN, L2TP"),
                SectionItem("VPN Серверы (детально)", Screen.VpnServers.route, "WireGuard, L2TP, IKEv2, SSTP, IPsec")
            )
        ),
        SectionCategory(
            title = "Сетевые правила и безопасность",
            icon = Icons.Default.Security,
            items = listOf(
                SectionItem("DNS-фильтры", Screen.DnsFilters.route, "Контентная фильтрация и безопасность"),
                SectionItem("Контентная фильтрация", Screen.ContentFilter.route, "NextDNS, SafeDNS, SkyDNS"),
                SectionItem("Переадресация портов", Screen.PortForwarding.route, "NAT и виртуальные серверы"),
                SectionItem("Межсетевой экран", Screen.Firewall.route, "Правила фильтрации трафика"),
                SectionItem("FQDN группы", Screen.ObjectGroup.route, "Доменные имена в группах"),
                SectionItem("Статическая маршрутизация", Screen.StaticRoutes.route, "Таблица маршрутов и шлюзы"),
                SectionItem("LAN сегменты", Screen.LanSegments.route, "Подсети и изоляция клиентов"),
                SectionItem("Контроль доступа Wi-Fi", Screen.WifiAcl.route, "Черные и белые списки MAC"),
                SectionItem("IPv6", Screen.Ipv6.route, "Адреса, префиксы, маршруты IPv6")
            )
        ),
        SectionCategory(
            title = "Сервисы и приложения",
            icon = Icons.Default.Apps,
            items = listOf(
                SectionItem("UPnP / NAT-PMP", Screen.Upnp.route, "Автоматическая переадресация"),
                SectionItem("Динамический DNS", Screen.Ddns.route, "DDNS-провайдеры и обновления"),
                SectionItem("Торрент-клиент", Screen.TorrentDetail.route, "Статус и настройки торрента"),
                SectionItem("Облачные сервисы", Screen.Cloud.route, "Keenetic Cloud и NDMP"),
                SectionItem("USB и накопители", Screen.UsbDevices.route, "Диски, SMB, FTP, DLNA"),
                SectionItem("Сетевые сервисы", Screen.SshSnmp.route, "SSH, SNMP, FTP, Telnet, Proxy"),
                SectionItem("Сеть Windows (SMB)", Screen.Smb.route, "Служба SMB / CIFS"),
                SectionItem("Медиасервер (DLNA)", Screen.MediaServer.route, "Доступ к мультимедиа по DLNA / UPnP")
            )
        ),
        SectionCategory(
            title = "Управление и система",
            icon = Icons.Default.Settings,
            items = listOf(
                SectionItem("Системные настройки", Screen.SystemAdvanced.route, "Имя хоста, время, LED, NTP, бэкап"),
                SectionItem("Компоненты", Screen.Components.route, "Установка и управление пакетами"),
                SectionItem("Менеджер пакетов OPKG", Screen.Opkg.route, "Диск и конфигурация OPKG"),
                SectionItem("KeeneticOS и обновление", Screen.Firmware.route, "Версия прошивки и компоненты"),
                SectionItem("Пользователи и доступ", Screen.UserAccounts.route, "Учетные записи и права"),
                SectionItem("Конфигурация (RCI / CLI)", Screen.Configuration.route, "Running-config, инспектор и команды NDM"),
                SectionItem("SSH-терминал", Screen.SshTerminal.route, "Выполнение команд CLI по SSH"),
                SectionItem("Журнал событий (Логи)", Screen.SystemLogs.route, "Системные события ndm"),
                SectionItem("Диагностика сети", Screen.Diagnostics.route, "Ping, Traceroute, DNS"),
                SectionItem("Диагностика кабеля", Screen.CableDiagnostics.route, "Длина и состояние кабелей")
            )
        ),
SectionCategory(
            title = "Мониторинг",
            icon = Icons.Default.Monitor,
            items = listOf(
                SectionItem("Системный монитор", Screen.SystemMonitor.route, "CPU, RAM, радио, интерфейсы, клиенты"),
                SectionItem("Монитор трафика", Screen.TrafficMonitor.route, "Текущая скорость на интерфейсах"),
                SectionItem("Мониторинг сети", Screen.NetworkMonitor.route, "Conntrack, ARP, IP-правила"),
                SectionItem("Список устройств", Screen.DeviceListDetailed.route, "Все подключённые устройства"),
                SectionItem("Устройства (обзор)", Screen.Devices.route, "Подключённые клиенты")
            )
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(categories) { category ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            category.icon,
                            contentDescription = null,
                            tint = KeeneticColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            category.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = KeeneticColors.TextPrimary,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = KeeneticColors.Divider)

                    category.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate(item.route) }
                                .padding(vertical = 12.dp),
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
                    }
                }
            }
        }
    }
}
