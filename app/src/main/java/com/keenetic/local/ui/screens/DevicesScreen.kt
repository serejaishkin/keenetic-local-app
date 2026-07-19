package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.Client
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun DevicesScreen(viewModel: RouterViewModel) {
    val clients by viewModel.clients.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Подключённые устройства",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${clients.size} устройств",
            style = MaterialTheme.typography.bodyMedium,
            color = KeeneticColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(clients.size) { index ->
                ClientCard(client = clients[index], viewModel = viewModel)
            }
            if (clients.isEmpty()) {
                item {
                    Text(
                        "Нет данных. Нажмите 🔄 для обновления.",
                        color = KeeneticColors.TextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    if (isLoading && clients.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = KeeneticColors.Primary)
        }
    }
}

@Composable
fun ClientCard(client: Client, viewModel: RouterViewModel) {
    val isBlocked = client.access == "deny"
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Smartphone,
                contentDescription = null,
                tint = if (isBlocked) KeeneticColors.Error else KeeneticColors.Primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = client.name ?: "Unknown",
                    fontWeight = FontWeight.Medium,
                    color = if (isBlocked) KeeneticColors.Error else KeeneticColors.TextPrimary
                )
                Text(
                    text = "${client.ip ?: "—"}  •  ${client.mac ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
                if (isBlocked) {
                    Text(
                        text = "Заблокировано",
                        style = MaterialTheme.typography.labelSmall,
                        color = KeeneticColors.Error
                    )
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (isBlocked) "Разблокировать" else "Заблокировать") },
                        onClick = {
                            viewModel.toggleClient(client.mac ?: "", !isBlocked)
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                if (isBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                                contentDescription = null,
                                tint = if (isBlocked) KeeneticColors.Accent else KeeneticColors.Error
                            )
                        }
                    )
                }
            }
        }
    }
}
