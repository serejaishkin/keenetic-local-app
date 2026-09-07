package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.*
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun DdnsScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val dyndnsStatus by viewModel.dyndnsStatus.collectAsState()
    val profiles by viewModel.dyndnsProfiles.collectAsState()
    val updaters by viewModel.dyndnsUpdaters.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadDyndnsStatus() }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary) }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Public, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Динамический DNS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Статус", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Включён", color = KeeneticColors.TextPrimary)
                        Switch(checked = dyndnsStatus.enabled, onCheckedChange = { viewModel.setDyndnsEnabled(it) })
                    }
                    InfoRow("Провайдер", dyndnsStatus.provider)
                    InfoRow("Хост", dyndnsStatus.hostname)
                    InfoRow("Последнее обновление", dyndnsStatus.lastUpdate)
                }
            }
        }

        if (profiles.isNotEmpty()) {
            item { Text("Профили", style = MaterialTheme.typography.titleSmall, color = KeeneticColors.TextPrimary, modifier = Modifier.padding(horizontal = 4.dp)) }
            items(profiles) { profile ->
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(profile.name, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                        Text("Хост: ${profile.hostname}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                        Text("Пользователь: ${profile.username}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                    }
                }
            }
        }

        if (updaters.isNotEmpty()) {
            item { Text("Обновления", style = MaterialTheme.typography.titleSmall, color = KeeneticColors.TextPrimary, modifier = Modifier.padding(horizontal = 4.dp)) }
            items(updaters) { updater ->
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(updater.name, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                        Text("Хост: ${updater.hostname}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                        Text("Статус: ${updater.status}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                        Text("Обновлено: ${updater.lastUpdate}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Medium)
    }
}
