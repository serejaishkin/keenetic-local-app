package com.keenetic.local.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.keenetic.local.ui.Screen
import com.keenetic.local.ui.theme.KeeneticColors

data class SectionItem(
    val label: String,
    val route: String? = null,
    val subtitle: String? = null,
    val soon: Boolean = false
)

private data class SectionCard(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: String,
    val items: List<SectionItem>
)

private fun sectionCards(): List<SectionCard> = listOf(
    SectionCard(
        title = "Интернет",
        description = "Подключения, каналы связи и приоритеты",
        icon = Icons.Default.NetworkCheck,
        route = Screen.InternetSection.route,
        items = listOf(
            SectionItem("Кабель Ethernet", Screen.Internet.route, "WAN/LAN и состояние интерфейсов"),
            SectionItem("Wireless ISP", Screen.WiFi.route, "Беспроводные провайдеры"),
            SectionItem("Mobile", soon = true, subtitle = "Модемы и мобильные соединения")
        )
    ),
    SectionCard(
        title = "Wi-Fi",
        description = "Точки доступа, клиенты и сегменты",
        icon = Icons.Default.Wifi,
        route = Screen.WiFiSection.route,
        items = listOf(
            SectionItem("Точки доступа", Screen.WiFi.route, "SSID, безопасность, состояние"),
            SectionItem("Списки клиентов", Screen.Devices.route, "Подключённые устройства"),
            SectionItem("Сегменты", soon = true, subtitle = "LAN-сегменты и изоляция")
        )
    ),
    SectionCard(
        title = "Сеть",
        description = "Маршрутизация, правила и доступ",
        icon = Icons.Default.Route,
        route = Screen.NetworkSection.route,
        items = listOf(
            SectionItem("Маршрутизация", Screen.Devices.route, "Политики и статические маршруты"),
            SectionItem("Переадресация портов", Screen.PortForwarding.route, "Внешние и внутренние порты"),
            SectionItem("Межсетевой экран", Screen.Firewall.route, "Правила безопасности"),
            SectionItem("LAN-сегменты", Screen.LanSegments.route, "Сегменты и изоляция")
        )
    ),
    SectionCard(
        title = "Устройства",
        description = "Клиенты, интерфейсы и локальная сеть",
        icon = Icons.Default.Devices,
        route = Screen.Devices.route,
        items = listOf(
            SectionItem("Список устройств", Screen.Devices.route, "Подключённые клиенты"),
            SectionItem("Интерфейсы", Screen.Internet.route, "WAN/LAN и состояние"),
            SectionItem("Сеть и доступ", Screen.Devices.route, "Права и ограничение")
        )
    ),
    SectionCard(
        title = "Управление",
        description = "Система, приложения и диагностика",
        icon = Icons.Default.Settings,
        route = Screen.ManagementSection.route,
        items = listOf(
            SectionItem("Настройки системы", Screen.SystemSettings.route, "VPN, DoH, расписания"),
            SectionItem("Приложения", Screen.Apps.route, "OPKG, пакеты и сервисы"),
            SectionItem("Терминал", Screen.Terminal.route, "CLI и SSH")
        )
    ),
    SectionCard(
        title = "Терминал",
        description = "CLI и SSH для прямого управления роутером",
        icon = Icons.Default.Terminal,
        route = Screen.Terminal.route,
        items = listOf(
            SectionItem("CLI", Screen.Terminal.route, "Команды и диагностика"),
            SectionItem("SSH", Screen.Terminal.route, "Удалённый доступ")
        )
    ),
    SectionCard(
        title = "Приложения",
        description = "Дополнительные сервисы и пакеты",
        icon = Icons.Default.Apps,
        route = Screen.Apps.route,
        items = listOf(
            SectionItem("OPKG", Screen.Apps.route, "Пакеты и сервисы"),
            SectionItem("Сервисы", Screen.Apps.route, "Интеллектуальные функции")
        )
    ),
    SectionCard(
        title = "Веб-сервисы",
        description = "Мини-браузер для awg manager, nfqws2 и других сервисов",
        icon = Icons.Default.Public,
        route = Screen.WebServices.route,
        items = listOf(
            SectionItem("AWG Manager", Screen.WebServices.route, "Быстрый доступ к сервису"),
            SectionItem("Nfqws2", Screen.WebServices.route, "Внутренний веб-интерфейс"),
            SectionItem("Свой URL", Screen.WebServices.route, "IP + порт")
        )
    ),
    SectionCard(
        title = "VPN",
        description = "Отдельный блок для VPN-сервера",
        icon = Icons.Default.Lock,
        route = Screen.VpnSettings.route,
        items = listOf(
            SectionItem("Состояние VPN", Screen.VpnSettings.route, "Проверка подключения"),
            SectionItem("Параметры сервера", Screen.VpnAdvanced.route, "Сеть, сегменты, доступ")
        )
    ),
    SectionCard(
        title = "DNS",
        description = "Управление DNS, фильтрами и DoH",
        icon = Icons.Default.Dns,
        route = Screen.DnsSection.route,
        items = listOf(
            SectionItem("DNS-фильтры", Screen.DnsFilters.route, "Фильтрация доменов и блокировки"),
            SectionItem("DNS-over-HTTPS", Screen.DohSettings.route, "Настройка DoH"),
            SectionItem("Текущие DNS", Screen.DnsSection.route, "Серверы и интерфейсы")
        )
    ),
    SectionCard(
        title = "DoH",
        description = "Отдельный блок для DNS-over-HTTPS",
        icon = Icons.Default.Security,
        route = Screen.DohSettings.route,
        items = listOf(
            SectionItem("Настройка DoH", Screen.DohSettings.route, "URL и интерфейс"),
            SectionItem("Список DNS", Screen.DohSettings.route, "Текущие серверы")
        )
    ),
    SectionCard(
        title = "Расписание",
        description = "Отдельный блок для расписаний доступа",
        icon = Icons.Default.Schedule,
        route = Screen.SchedulesSettings.route,
        items = listOf(
            SectionItem("Создать расписание", Screen.SchedulesSettings.route, "Дни, время, правила"),
            SectionItem("Управление расписаниями", Screen.SchedulesSettings.route, "Редактирование и просмотр")
        )
    )
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AllSectionsScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Разделы",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Выбирай главный блок как в веб-интерфейсе Keenetic",
            style = MaterialTheme.typography.bodySmall,
            color = KeeneticColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(sectionCards()) { card ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate(card.route) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(KeeneticColors.Primary.copy(alpha = 0.12f), shape = MaterialTheme.shapes.medium),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(card.icon, contentDescription = null, tint = KeeneticColors.Primary, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(card.title, fontWeight = FontWeight.SemiBold, color = KeeneticColors.TextPrimary)
                                Text(card.description, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                            }
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = KeeneticColors.TextSecondary)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            card.items.forEach { item ->
                                val implemented = item.route != null && !item.soon
                                AssistChip(
                                    onClick = {
                                        if (implemented) item.route?.let { navController.navigate(it) }
                                    },
                                    enabled = implemented,
                                    label = { Text(item.label) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (implemented) KeeneticColors.Primary.copy(alpha = 0.08f) else KeeneticColors.Surface,
                                        labelColor = if (implemented) KeeneticColors.Primary else KeeneticColors.TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun PlaceholderSectionScreen(
    title: String,
    description: String,
    items: List<String>,
    onBack: (() -> Unit)? = null
) {
    // Заглушка для разделов без подтверждённого API: показываем структуру UI
    // и список будущих функций, чтобы интерфейс выглядел как полноценный сайт.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                }
            }
            if (onBack != null) Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Text(description, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Структура заглушки", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Подтверждённого API для этого раздела пока нет, поэтому добавлена готовая структура и список элементов для следующей реализации без API.",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                items.forEachIndexed { index, item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = KeeneticColors.Primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(item, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (index < items.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SectionCategoryScreen(
    navController: NavController,
    title: String,
    description: String,
    items: List<SectionItem>
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(description, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                items.forEachIndexed { index, item ->
                    val implemented = item.route != null && !item.soon
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = implemented) { item.route?.let { navController.navigate(it) } }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.label, color = if (implemented) KeeneticColors.TextPrimary else KeeneticColors.TextSecondary)
                                if (item.soon) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    AssistChip(onClick = {}, enabled = false, label = { Text("Скоро", style = MaterialTheme.typography.labelSmall) })
                                }
                            }
                            if (!item.subtitle.isNullOrBlank()) {
                                Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                        if (implemented) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = KeeneticColors.TextSecondary)
                        }
                    }
                    if (index < items.lastIndex) {
                        Divider(color = KeeneticColors.Divider, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        }
    }
}
