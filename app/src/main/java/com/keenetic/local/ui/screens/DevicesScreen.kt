package com.keenetic.local.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.ConnectedClient
import com.keenetic.local.api.ConnectionPolicy
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun DevicesScreen(viewModel: RouterViewModel) {
    val clients by viewModel.clients.collectAsState()
    val connectionPolicies by viewModel.connectionPolicies.collectAsState()
    var selectedClientForDetails by remember { mutableStateOf<ConnectedClient?>(null) }
    var wolMessage by remember { mutableStateOf<String?>(null) }

    val onlineCount = clients.count { it.active }
    val offlineCount = clients.size - onlineCount

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Устройства сети",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = KeeneticColors.TextPrimary
                )
                Text(
                    "Онлайн: $onlineCount  •  Всего: ${clients.size}  •  Политик: ${connectionPolicies.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
            }
            FilledTonalButton(
                onClick = {
                    viewModel.loadConnectionPolicies()
                    viewModel.loadClients()
                },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = KeeneticColors.SurfaceElevated,
                    contentColor = KeeneticColors.Primary
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Обновить", style = MaterialTheme.typography.labelMedium)
            }
        }

        if (wolMessage != null) {
            Snackbar(
                modifier = Modifier.padding(bottom = 8.dp),
                action = {
                    TextButton(onClick = { wolMessage = null }) {
                        Text("OK", color = KeeneticColors.Primary)
                    }
                }
            ) {
                Text(wolMessage ?: "")
            }
        }

        if (clients.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.DevicesOther,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = KeeneticColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Устройства не найдены или загружаются...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KeeneticColors.TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(clients, key = { it.mac }) { client ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedClientForDetails = client },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (client.active) KeeneticColors.Primary.copy(alpha = 0.15f)
                                            else KeeneticColors.SurfaceElevated
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (client.wifiSsid != null) Icons.Default.Wifi else Icons.Default.Lan,
                                        contentDescription = null,
                                        tint = if (client.active) KeeneticColors.Primary else KeeneticColors.TextSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            client.displayName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = KeeneticColors.TextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        if (client.isBlocked) {
                                            Surface(
                                                color = KeeneticColors.Error.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "Блок",
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = KeeneticColors.Error,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "${client.ip}  •  ${client.mac}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = KeeneticColors.TextSecondary
                                    )

                                    // Badges for sub-settings
                                    Row(
                                        modifier = Modifier.padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (client.isStaticIp) {
                                            Surface(
                                                color = KeeneticColors.Primary.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "Фикс. IP",
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = KeeneticColors.Primary
                                                )
                                            }
                                        }
                                        if (client.policyId.isNotBlank() || (client.policy != "Основная" && !client.policy.contains("по умолчанию", ignoreCase = true))) {
                                            Surface(
                                                color = KeeneticColors.Warning.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    client.policy,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = KeeneticColors.Warning
                                                )
                                            }
                                        }
                                        if (client.wifiBandPreference != "Авто") {
                                            Surface(
                                                color = KeeneticColors.Secondary.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    client.wifiBandPreference,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = KeeneticColors.Secondary
                                                )
                                            }
                                        }
                                        if (client.speedLimitMbps > 0) {
                                            Surface(
                                                color = KeeneticColors.TextSecondary.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "Лимит ${client.speedLimitMbps} Мб/с",
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = KeeneticColors.TextPrimary
                                                )
                                            }
                                        }
                                    }

                                    if (client.active) {
                                        Row(
                                            modifier = Modifier.padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (client.wifiBand != null) {
                                                Text(
                                                    client.wifiBand,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = KeeneticColors.Primary
                                                )
                                            }
                                            if (client.rxSpeedKbps > 0 || client.txSpeedKbps > 0) {
                                                Text(
                                                    "↓ ${client.rxSpeedKbps / 1000f} Мб/с  ↑ ${client.txSpeedKbps / 1000f} Мб/с",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = KeeneticColors.Success
                                                )
                                            }
                                        }
                                    }
                                }

                                // Quick Actions (WoL + Block)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            viewModel.wakeOnLan(client.mac)
                                            wolMessage = "Пакет Wake-on-LAN отправлен на ${client.displayName}"
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.PowerSettingsNew,
                                            contentDescription = "Разбудить (WoL)",
                                            tint = KeeneticColors.Primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.toggleClientBlock(client) }
                                    ) {
                                        Icon(
                                            if (client.isBlocked) Icons.Default.Block else Icons.Default.CheckCircle,
                                            contentDescription = if (client.isBlocked) "Разблокировать" else "Заблокировать",
                                            tint = if (client.isBlocked) KeeneticColors.Error else KeeneticColors.Success,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Client Details Modal Dialog with full sub-items & configuration
    selectedClientForDetails?.let { client ->
        var editedName by remember(client.mac) { mutableStateOf(client.displayName) }
        var isStaticIp by remember(client.mac) { mutableStateOf(client.isStaticIp) }
        var editedIp by remember(client.mac) { mutableStateOf(client.ip) }
        var selectedPolicyId by remember(client.mac) {
            mutableStateOf(
                if (client.policyId.isNotBlank()) client.policyId
                else connectionPolicies.find { it.name.equals(client.policy, ignoreCase = true) }?.id ?: ""
            )
        }
        var selectedBand by remember(client.mac) { mutableStateOf(client.wifiBandPreference) }
        var selectedSpeedLimit by remember(client.mac) { mutableStateOf(client.speedLimitMbps) }
        var isBlockedState by remember(client.mac) { mutableStateOf(client.isBlocked) }

        AlertDialog(
            onDismissRequest = { selectedClientForDetails = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        if (client.wifiSsid != null) Icons.Default.Wifi else Icons.Default.Lan,
                        contentDescription = null,
                        tint = KeeneticColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            "Параметры устройства",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = KeeneticColors.TextPrimary
                        )
                        Text(
                            client.mac,
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Rename
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("Имя устройства") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 2. Fix IP (Static DHCP Reservation)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = KeeneticColors.SurfaceElevated),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Зафиксировать IP-адрес",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KeeneticColors.TextPrimary
                                    )
                                    Text(
                                        "Привязка к MAC в DHCP",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = KeeneticColors.TextSecondary
                                    )
                                }
                                Switch(
                                    checked = isStaticIp,
                                    onCheckedChange = { isStaticIp = it }
                                )
                            }
                            if (isStaticIp) {
                                OutlinedTextField(
                                    value = editedIp,
                                    onValueChange = { editedIp = it },
                                    label = { Text("Постоянный IP") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // 3. Connection Policy (fetched from router via RCI /rci/show/ip/policy)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Политика подключения",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = KeeneticColors.TextPrimary
                            )
                            Text(
                                "show ip policy",
                                style = MaterialTheme.typography.labelSmall,
                                color = KeeneticColors.Primary
                            )
                        }

                        val availablePolicies = if (connectionPolicies.isNotEmpty()) connectionPolicies else listOf(
                            ConnectionPolicy("", "Основная (по умолчанию)", "Следовать политике сегмента сети")
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            availablePolicies.forEach { pol ->
                                val isSelected = (selectedPolicyId == pol.id)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedPolicyId = pol.id },
                                    color = if (isSelected) KeeneticColors.Primary.copy(alpha = 0.12f) else KeeneticColors.SurfaceElevated,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) KeeneticColors.Primary else KeeneticColors.Border
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedPolicyId = pol.id },
                                            colors = RadioButtonDefaults.colors(selectedColor = KeeneticColors.Primary),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    pol.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) KeeneticColors.Primary else KeeneticColors.TextPrimary
                                                )
                                                if (pol.id.isNotBlank()) {
                                                    Surface(
                                                        color = KeeneticColors.Surface,
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            pol.id,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = KeeneticColors.TextSecondary
                                                        )
                                                    }
                                                }
                                            }
                                            if (pol.description.isNotBlank() && pol.description != pol.name) {
                                                Text(
                                                    pol.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = KeeneticColors.TextSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Wi-Fi Band Steering / Frequency
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Частота Wi-Fi диапазона",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = KeeneticColors.TextPrimary
                        )
                        val bands = listOf("Авто", "5 ГГц", "2.4 ГГц")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            bands.forEach { b ->
                                FilterChip(
                                    selected = selectedBand == b,
                                    onClick = { selectedBand = b },
                                    label = { Text(b, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // 5. Speed Limit
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Ограничение скорости",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = KeeneticColors.TextPrimary
                        )
                        val speedLimits = listOf(0 to "Без лимита", 5 to "5 Мб/с", 10 to "10 Мб/с", 25 to "25 Мб/с", 50 to "50 Мб/с")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            speedLimits.forEach { (mbps, label) ->
                                FilterChip(
                                    selected = selectedSpeedLimit == mbps,
                                    onClick = { selectedSpeedLimit = mbps },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    // 6. Block Internet Access switch & WoL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Блокировать доступ в интернет",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isBlockedState) KeeneticColors.Error else KeeneticColors.TextPrimary
                        )
                        Switch(
                            checked = isBlockedState,
                            onCheckedChange = { isBlockedState = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.wakeOnLan(client.mac)
                                wolMessage = "Пакет Wake-on-LAN отправлен на ${client.displayName}"
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WoL пакет", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                viewModel.deleteKnownDevice(client.mac)
                                wolMessage = "Устройство ${client.displayName} удалено из известных"
                                selectedClientForDetails = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = KeeneticColors.Error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Удалить", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val chosenPolicy = connectionPolicies.find { it.id == selectedPolicyId }
                        viewModel.updateClientFullSettings(
                            mac = client.mac,
                            newName = editedName,
                            ip = editedIp,
                            isStatic = isStaticIp,
                            policy = chosenPolicy?.name ?: "Основная",
                            policyId = selectedPolicyId,
                            wifiBandPreference = selectedBand,
                            speedLimitMbps = selectedSpeedLimit
                        )
                        if (isBlockedState != client.isBlocked) {
                            viewModel.toggleClientBlock(client)
                        }
                        wolMessage = "Настройки для «$editedName» успешно сохранены!"
                        selectedClientForDetails = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedClientForDetails = null }) {
                    Text("Отмена", color = KeeneticColors.TextSecondary)
                }
            }
        )
    }
}
