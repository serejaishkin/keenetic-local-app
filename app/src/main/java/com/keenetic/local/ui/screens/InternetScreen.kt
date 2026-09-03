package com.keenetic.local.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.RouterInterface
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun InternetScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val interfaces by viewModel.interfaces.collectAsState()
    var selectedIface by remember { mutableStateOf<RouterInterface?>(null) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadInterfaces()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Language, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Подключения и интерфейсы",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = KeeneticColors.TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.loadInterfaces() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = KeeneticColors.Primary)
                }
            }
        }

        if (feedbackMessage != null) {
            item {
                Snackbar(
                    action = {
                        TextButton(onClick = { feedbackMessage = null }) {
                            Text("OK", color = KeeneticColors.Primary)
                        }
                    }
                ) {
                    Text(feedbackMessage ?: "")
                }
            }
        }

        if (interfaces.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Language,
                            contentDescription = null,
                            tint = KeeneticColors.TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "Сетевые интерфейсы опрашиваются...",
                            style = MaterialTheme.typography.titleMedium,
                            color = KeeneticColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Button(
                            onClick = { viewModel.loadInterfaces() },
                            colors = ButtonDefaults.outlinedButtonColors()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = KeeneticColors.Primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Обновить интерфейсы", color = KeeneticColors.Primary)
                        }
                    }
                }
            }
        } else {
            items(interfaces) { iface ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedIface = iface },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = KeeneticColors.Primary)
                                Text(iface.name, style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary)
                            }
                            Surface(
                                color = if (iface.isUp) KeeneticColors.Success.copy(alpha = 0.15f) else KeeneticColors.Error.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    if (iface.isUp) "Подключено" else "Отключено",
                                    color = if (iface.isUp) KeeneticColors.Success else KeeneticColors.Error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (iface.description.isNotBlank()) {
                            Text(iface.description, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                        }
                        if (iface.ip != null) {
                            Text(
                                "IP: ${iface.ip} ${if (iface.mask != null) "• Маска: ${iface.mask}" else ""}",
                                color = KeeneticColors.TextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }

    // Interface Details and Sub-actions Dialog
    selectedIface?.let { iface ->
        AlertDialog(
            onDismissRequest = { selectedIface = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = KeeneticColors.Primary)
                    Text(iface.name, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(iface.description.ifBlank { "Сетевой интерфейс KeeneticOS" }, style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextSecondary)
                    HorizontalDivider(color = KeeneticColors.Divider)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Статус подключения", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextSecondary)
                        Text(
                            if (iface.isUp) "Активен (UP)" else "Не активен (DOWN)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (iface.isUp) KeeneticColors.Success else KeeneticColors.Error
                        )
                    }
                    if (iface.ip != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("IP-адрес", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextSecondary)
                            Text(iface.ip, style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary)
                        }
                    }
                    if (iface.mask != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Маска подсети", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextSecondary)
                            Text(iface.mask, style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary)
                        }
                    }

                    HorizontalDivider(color = KeeneticColors.Divider)

                    Button(
                        onClick = {
                            viewModel.reconnectInterface(iface.id)
                            feedbackMessage = "Запущен перезапуск сессии для интерфейса «${iface.name}»"
                            selectedIface = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Переподключить (Рестарт сессии)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedIface = null }) {
                    Text("Закрыть", color = KeeneticColors.TextPrimary)
                }
            }
        )
    }
}
