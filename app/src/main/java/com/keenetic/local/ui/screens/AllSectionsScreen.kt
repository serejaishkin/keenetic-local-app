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
    val route: String
)

private fun sectionCards(navController: NavController): List<SectionCard> = listOf(
    SectionCard("Интернет", "Подключения, каналы связи и приоритеты", Icons.Default.NetworkCheck, Screen.InternetSection.route),
    SectionCard("Wi-Fi", "Точки доступа, клиенты и сегменты", Icons.Default.Wifi, Screen.WiFiSection.route),
    SectionCard("Сеть", "Маршрутизация, правила и доступ", Icons.Default.Route, Screen.NetworkSection.route),
    SectionCard("Управление", "Система, приложения и диагностика", Icons.Default.Settings, Screen.ManagementSection.route)
)

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
            items(sectionCards(navController)) { card ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { navController.navigate(card.route) },
                    colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(card.icon, contentDescription = null, tint = KeeneticColors.Primary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(card.title, fontWeight = FontWeight.SemiBold, color = KeeneticColors.TextPrimary)
                            Text(card.description, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                        }
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = KeeneticColors.TextSecondary)
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
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
