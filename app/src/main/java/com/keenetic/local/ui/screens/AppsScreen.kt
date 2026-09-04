package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.*
import com.keenetic.local.ui.theme.KeeneticColors

/**
 * Отдельный экран "Приложения" (opkg/торрент/IntelliQoS), раньше жил вместе
 * с системными настройками подключения в одном SettingsScreen - развели по
 * аналогии с реальным сайтом, где это разные разделы ("Управление →
 * Настройки системы" отдельно от "Управление → Приложения").
 */
@Composable
fun AppsScreen(viewModel: RouterViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Приложения",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "IntelliQoS, менеджер пакетов, торрент-клиент",
            style = MaterialTheme.typography.bodySmall,
            color = KeeneticColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { ExtraServicesCard(viewModel) }
            item { TorrentSettingsCard(viewModel) }
        }
    }
}

/**
 * Параметры торрент-клиента. ПОДТВЕРЖДЕНО HAR (06.08, реальный успешный
 * вызов): {"torrent":{"directory":..,"rpc-port":{"port":..,"public":..},
 * "peer-port":..}}. Это отдельно от простого тумблера вкл/выкл в
 * ExtraServicesCard (тот использует неподтверждённый service.torrent) -
 * здесь именно реальные параметры конфигурации.
 */
@Composable
private fun TorrentSettingsCard(viewModel: RouterViewModel) {
    var directory by remember { mutableStateOf("OPKG:") }
    var rpcPort by remember { mutableStateOf("8090") }
    var rpcPublic by remember { mutableStateOf(false) }
    var peerPort by remember { mutableStateOf("51413") }
    val error by viewModel.error.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Параметры торрент-клиента", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Подтверждено HAR - реальный успешный вызов на роутере",
                style = MaterialTheme.typography.labelSmall,
                color = KeeneticColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = directory, onValueChange = { directory = it },
                label = { Text("Директория загрузок") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rpcPort, onValueChange = { rpcPort = it },
                    label = { Text("RPC-порт") }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = peerPort, onValueChange = { peerPort = it },
                    label = { Text("Peer-порт") }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = rpcPublic, onCheckedChange = { rpcPublic = it })
                Text("RPC доступен из интернета", style = MaterialTheme.typography.bodySmall)
            }
            if (error != null) {
                Text(error ?: "", color = KeeneticColors.Error, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
            }
            Button(
                onClick = {
                    viewModel.setTorrentSettings(
                        directory = directory,
                        rpcPort = rpcPort.toIntOrNull() ?: 8090,
                        rpcPublic = rpcPublic,
                        peerPort = peerPort.toIntOrNull() ?: 51413
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить")
            }
        }
    }
}
