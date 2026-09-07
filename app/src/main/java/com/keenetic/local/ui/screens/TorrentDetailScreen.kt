package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun TorrentDetailScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val status by viewModel.torrentStatusFull.collectAsState()
    val account by viewModel.torrentLocalAccount.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadTorrentStatusFull() }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary) }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Торрент-клиент", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Статус", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Включён", color = KeeneticColors.TextPrimary)
                        Text(if (status.enabled) "Да" else "Нет", color = if (status.enabled) KeeneticColors.Primary else KeeneticColors.TextSecondary, fontWeight = FontWeight.Bold)
                    }
                    InfoRow("RPC порт", "${status.rpcPort}")
                    InfoRow("RPC публичный", if (status.rpcPublic) "Да" else "Нет")
                    InfoRow("Пир порт", "${status.peerPort}")
                    InfoRow("Каталог загрузки", status.downloadDir)
                    InfoRow("Активных торрентов", "${status.activeTorrents}")
                    InfoRow("Всего торрентов", "${status.totalTorrents}")
                    InfoRow("Скорость загрузки", formatSpeed(status.downloadSpeed))
                    InfoRow("Скорость отдачи", formatSpeed(status.uploadSpeed))
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Локальная учётная запись", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    InfoRow("Пользователь", account.username)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Включён", color = KeeneticColors.TextPrimary)
                        Text(if (account.enabled) "Да" else "Нет", color = if (account.enabled) KeeneticColors.Primary else KeeneticColors.TextSecondary, fontWeight = FontWeight.Bold)
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

private fun formatSpeed(bytesPerSec: Long): String {
    if (bytesPerSec <= 0) return "0 B/s"
    val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s")
    var value = bytesPerSec.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.size - 1) { value /= 1024; unitIndex++ }
    return "%.1f %s".format(value, units[unitIndex])
}
