package com.keenetic.local.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.keenetic.local.ui.Screen
import com.keenetic.local.ui.theme.KeeneticColors

/**
 * Пункт меню, повторяющий структуру реального веб-интерфейса Keenetic
 * (см. keenetic-web-ui-structure.md - извлечено из реального DOM
 * веб-морды, не догадка). route == null означает "ещё не реализовано" -
 * такие пункты показываются, но неактивны, с пометкой "Скоро".
 */
private data class SectionItem(val label: String, val route: String?)
private data class SectionGroup(val title: String, val items: List<SectionItem>)

private fun sections(): List<SectionGroup> = listOf(
    SectionGroup("Статус", listOf(
        SectionItem("Системный монитор", Screen.Dashboard.route),
        SectionItem("Монитор трафика", null),
        SectionItem("Анализатор трафика приложений", null),
        SectionItem("Монитор Wi-Fi", null)
    )),
    SectionGroup("Интернет", listOf(
        SectionItem("Кабель Ethernet", Screen.Internet.route),
        SectionItem("Mobile", null),
        SectionItem("Wireless ISP", Screen.WiFi.route),
        SectionItem("Другие подключения", Screen.Dashboard.route),
        SectionItem("Приоритеты подключений", null)
    )),
    SectionGroup("Мои сети и Wi-Fi", listOf(
        SectionItem("Списки клиентов", Screen.Devices.route),
        SectionItem("Точки доступа", Screen.WiFi.route),
        SectionItem("Сегменты", null),
        SectionItem("Wi-Fi-система", Screen.WiFi.route),
        SectionItem("IntelliQoS", Screen.Apps.route)
    )),
    SectionGroup("Сетевые правила", listOf(
        SectionItem("Интернет-фильтры (DNS)", Screen.Settings.route),
        SectionItem("Межсетевой экран", null),
        SectionItem("Переадресация портов", null),
        SectionItem("Маршрутизация", Screen.Devices.route),
        SectionItem("Доменное имя", null),
        SectionItem("Контроль доступа Wi-Fi", null)
    )),
    SectionGroup("Управление", listOf(
        SectionItem("Настройки системы", Screen.Settings.route),
        SectionItem("Накопители и устройства", null),
        SectionItem("Приложения", Screen.Apps.route),
        SectionItem("Пользователи и доступ", null),
        SectionItem("Диагностика", Screen.Terminal.route),
        SectionItem("OPKG", Screen.Apps.route)
    ))
)

@Composable
fun AllSectionsScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Все разделы",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Структура повторяет веб-интерфейс роутера",
            style = MaterialTheme.typography.bodySmall,
            color = KeeneticColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(sections()) { group ->
                Column {
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = KeeneticColors.Primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column {
                            group.items.forEachIndexed { index, item ->
                                val implemented = item.route != null
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = implemented) {
                                            item.route?.let { navController.navigate(it) }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.label,
                                        color = if (implemented) KeeneticColors.TextPrimary else KeeneticColors.TextSecondary
                                    )
                                    if (implemented) {
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = KeeneticColors.TextSecondary
                                        )
                                    } else {
                                        AssistChip(
                                            onClick = {},
                                            enabled = false,
                                            label = { Text("Скоро", style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                                if (index < group.items.lastIndex) {
                                    Divider(color = KeeneticColors.Divider)
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
