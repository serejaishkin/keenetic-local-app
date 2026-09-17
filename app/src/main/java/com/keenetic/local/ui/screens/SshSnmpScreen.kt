package com.keenetic.local.ui.screens

import androidx.compose.foundation.clickable
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
import com.keenetic.local.api.ComponentInfo
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun SshSnmpScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val sshSettings by viewModel.sshSettings.collectAsState()
    val snmpView by viewModel.snmpView.collectAsState()
    val ftpSettings by viewModel.ftpSettings.collectAsState()
    val telnetSettings by viewModel.telnetSettings.collectAsState()
    val httpProxySettings by viewModel.httpProxySettings.collectAsState()
    val componentList by viewModel.componentList.collectAsState()

    var showSshPortDialog by remember { mutableStateOf(false) }
    var showSnmpCommunityDialog by remember { mutableStateOf(false) }
    var showFtpPortDialog by remember { mutableStateOf(false) }
    var showTelnetPortDialog by remember { mutableStateOf(false) }
    var showHttpProxyPortDialog by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadSshSettings()
        viewModel.loadSnmpSettings()
        viewModel.loadFtpSettings()
        viewModel.loadTelnetSettings()
        viewModel.loadHttpProxySettings()
        viewModel.loadComponents()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                Icon(Icons.Default.Security, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Сетевые сервисы",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = KeeneticColors.TextPrimary
                )
            }
        }

        // SSH
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("SSH", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Включён", color = KeeneticColors.TextPrimary)
                        Switch(checked = sshSettings.enabled, onCheckedChange = { viewModel.setSshEnabled(it) })
                    }
                    InfoRow("Порт", "${sshSettings.port}", onClick = { showSshPortDialog = true })
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SFTP", color = KeeneticColors.TextSecondary)
                        Text(if (sshSettings.sftpEnabled) "Включён" else "Выключен", color = KeeneticColors.TextPrimary)
                    }
                }
            }
        }

        // SNMP
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("SNMP", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Включён", color = KeeneticColors.TextPrimary)
                        Switch(checked = snmpView.enabled, onCheckedChange = { viewModel.setSnmpEnabled(it) })
                    }
                    InfoRow("Community", snmpView.community, onClick = { showSnmpCommunityDialog = true })
                }
            }
        }

        // FTP
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("FTP", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Включён", color = KeeneticColors.TextPrimary)
                        Switch(checked = ftpSettings.enabled, onCheckedChange = { viewModel.setFtpEnabled(it) })
                    }
                    InfoRow("Порт", "${ftpSettings.port}", onClick = { showFtpPortDialog = true })
                    InfoRow("Анонимный доступ", if (ftpSettings.anonymousAccess) "Да" else "Нет", onClick = { viewModel.setFtpAnonymousAccess(!ftpSettings.anonymousAccess) })
                }
            }
        }

        // Telnet
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Telnet", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Включён", color = KeeneticColors.TextPrimary)
                        Switch(checked = telnetSettings.enabled, onCheckedChange = { viewModel.setTelnetEnabled(it) })
                    }
                    InfoRow("Порт", "${telnetSettings.port}", onClick = { showTelnetPortDialog = true })
                }
            }
        }

        // HTTP Proxy
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("HTTP Proxy", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Включён", color = KeeneticColors.TextPrimary)
                        Switch(checked = httpProxySettings.enabled, onCheckedChange = { viewModel.setHttpProxyEnabled(it) })
                    }
                    InfoRow("Порт", "${httpProxySettings.port}", onClick = { showHttpProxyPortDialog = true })
                }
            }
        }

        // Компоненты и сервисы
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Extension, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Компоненты и сервисы", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    Text(
                        "Управление пакетами SMB, DLNA, Transmission и других сервисов через RCI (components.component). Установка возможна при доступности пакета.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                    val relevantComponents = remember(componentList) {
                        componentList.filter { c ->
                            val n = c.name.lowercase()
                            val t = c.title.lowercase()
                            n.contains("smb") || t.contains("smb") || n.contains("cifs") ||
                                    n.contains("dlna") || t.contains("dlna") ||
                                    n.contains("torrent") || t.contains("torrent") || n.contains("transmission") ||
                                    n.contains("ftp") || n.contains("telnet") || n.contains("keepalived") ||
                                    n.contains("opkg") || t.contains("оптимизация")
                        }
                    }
                    if (relevantComponents.isEmpty()) {
                        Text("Нет данных о компонентах", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                    } else {
                        relevantComponents.forEach { comp ->
                            SshSnmpComponentRow(comp, viewModel, feedbackMessage, { feedbackMessage = it })
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { viewModel.loadComponents() }) {
                            Text("Показать все компоненты →", color = KeeneticColors.Primary)
                        }
                    }
                }
            }
        }
    }

    if (feedbackMessage != null) {
        AlertDialog(
            onDismissRequest = { feedbackMessage = null },
            confirmButton = { TextButton(onClick = { feedbackMessage = null }) { Text("OK", color = KeeneticColors.Primary) } },
            title = { Text("Действие", fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary) },
            text = { Text(feedbackMessage ?: "", color = KeeneticColors.TextPrimary) }
        )
    }

    if (showSshPortDialog) {
        var portInput by remember { mutableStateOf("${sshSettings.port}") }
        PortDialog(
            title = "Порт SSH",
            portInput = portInput,
            onPortChange = { portInput = it },
            onDismiss = { showSshPortDialog = false },
            onSave = {
                portInput.toIntOrNull()?.let { viewModel.setSshPort(it) }
                showSshPortDialog = false
            }
        )
    }

    if (showSnmpCommunityDialog) {
        var communityInput by remember { mutableStateOf(snmpView.community) }
        AlertDialog(
            onDismissRequest = { showSnmpCommunityDialog = false },
            title = { Text("Community (SNMP)", fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary) },
            text = {
                OutlinedTextField(
                    value = communityInput,
                    onValueChange = { communityInput = it },
                    label = { Text("Community строка") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setSnmpCommunity(communityInput)
                    showSnmpCommunityDialog = false
                }) { Text("Сохранить", color = KeeneticColors.Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showSnmpCommunityDialog = false }) { Text("Отмена", color = KeeneticColors.TextSecondary) }
            }
        )
    }

    if (showFtpPortDialog) {
        var portInput by remember { mutableStateOf("${ftpSettings.port}") }
        PortDialog(
            title = "Порт FTP",
            portInput = portInput,
            onPortChange = { portInput = it },
            onDismiss = { showFtpPortDialog = false },
            onSave = {
                portInput.toIntOrNull()?.let { viewModel.setFtpPort(it) }
                showFtpPortDialog = false
            }
        )
    }

    if (showTelnetPortDialog) {
        var portInput by remember { mutableStateOf("${telnetSettings.port}") }
        PortDialog(
            title = "Порт Telnet",
            portInput = portInput,
            onPortChange = { portInput = it },
            onDismiss = { showTelnetPortDialog = false },
            onSave = {
                portInput.toIntOrNull()?.let { viewModel.setTelnetPort(it) }
                showTelnetPortDialog = false
            }
        )
    }

    if (showHttpProxyPortDialog) {
        var portInput by remember { mutableStateOf("${httpProxySettings.port}") }
        PortDialog(
            title = "Порт HTTP Proxy",
            portInput = portInput,
            onPortChange = { portInput = it },
            onDismiss = { showHttpProxyPortDialog = false },
            onSave = {
                portInput.toIntOrNull()?.let { viewModel.setHttpProxyPort(it) }
                showHttpProxyPortDialog = false
            }
        )
    }
}

@Composable
private fun PortDialog(title: String, portInput: String, onPortChange: (String) -> Unit, onDismiss: () -> Unit, onSave: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary) },
        text = {
            OutlinedTextField(
                value = portInput,
                onValueChange = onPortChange,
                label = { Text("Номер порта (1-65535)") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text("Сохранить", color = KeeneticColors.Primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = KeeneticColors.TextSecondary) }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextSecondary)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = if (onClick != null) KeeneticColors.Primary else KeeneticColors.TextPrimary, fontWeight = FontWeight.Medium)
            if (onClick != null) {
                Icon(Icons.Default.Edit, contentDescription = "Изменить", tint = KeeneticColors.Primary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SshSnmpComponentRow(comp: ComponentInfo, viewModel: RouterViewModel, feedbackMessage: String?, onFeedback: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                comp.title.ifBlank { comp.name },
                style = MaterialTheme.typography.bodyMedium,
                color = KeeneticColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                comp.description,
                style = MaterialTheme.typography.bodySmall,
                color = KeeneticColors.TextSecondary,
                maxLines = 2
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            if (comp.installed) "✓ Уст." else if (comp.available) "Доступен" else "Нет",
            style = MaterialTheme.typography.bodySmall,
            color = when {
                comp.installed -> KeeneticColors.Success
                comp.available -> KeeneticColors.Warning
                else -> KeeneticColors.TextSecondary
            },
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(6.dp))
        if (comp.installed) {
            IconButton(
                onClick = {
                    viewModel.removeComponent(comp.name)
                    onFeedback("Удаление компонента «${comp.title.ifBlank { comp.name }}»")
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Удалить", tint = KeeneticColors.Error, modifier = Modifier.size(18.dp))
            }
        } else if (comp.available) {
            IconButton(
                onClick = {
                    viewModel.installComponent(comp.name)
                    onFeedback("Установка компонента «${comp.title.ifBlank { comp.name }}»")
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Установить", tint = KeeneticColors.Primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}