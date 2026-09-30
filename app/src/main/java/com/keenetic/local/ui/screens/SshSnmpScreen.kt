package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.ComponentInfo
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.ConfirmDialog
import com.keenetic.local.ui.components.EditableRow
import com.keenetic.local.ui.components.EmptyHint
import com.keenetic.local.ui.components.FormDialog
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.RowDivider
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SwitchRow
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

    SectionScaffold(
        title = "Сетевые сервисы",
        subtitle = "SSH, SNMP, FTP, Telnet, HTTP Proxy",
        onBack = onBack,
        onRefresh = {
            viewModel.loadSshSettings()
            viewModel.loadSnmpSettings()
            viewModel.loadFtpSettings()
            viewModel.loadTelnetSettings()
            viewModel.loadHttpProxySettings()
            viewModel.loadComponents()
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "ssh") {
                SectionCard(title = "SSH", icon = Icons.Default.Terminal) {
                    SwitchRow(
                        label = "Включён",
                        checked = sshSettings.enabled,
                        onCheckedChange = { viewModel.setSshEnabled(it) }
                    )
                    RowDivider()
                    EditableRow(
                        label = "Порт",
                        value = "${sshSettings.port}",
                        onClick = { showSshPortDialog = true },
                        monospaceValue = true
                    )
                    RowDivider()
                    InfoRow(
                        label = "SFTP",
                        value = if (sshSettings.sftpEnabled) "Включён" else "Выключен"
                    )
                }
            }
            item(key = "snmp") {
                SectionCard(title = "SNMP", icon = Icons.Default.NetworkCheck) {
                    SwitchRow(
                        label = "Включён",
                        checked = snmpView.enabled,
                        onCheckedChange = { viewModel.setSnmpEnabled(it) }
                    )
                    RowDivider()
                    EditableRow(
                        label = "Community",
                        value = snmpView.community,
                        onClick = { showSnmpCommunityDialog = true },
                        monospaceValue = true
                    )
                }
            }
            item(key = "ftp") {
                SectionCard(title = "FTP", icon = Icons.Default.Folder) {
                    SwitchRow(
                        label = "Включён",
                        checked = ftpSettings.enabled,
                        onCheckedChange = { viewModel.setFtpEnabled(it) }
                    )
                    RowDivider()
                    EditableRow(
                        label = "Порт",
                        value = "${ftpSettings.port}",
                        onClick = { showFtpPortDialog = true },
                        monospaceValue = true
                    )
                    RowDivider()
                    SwitchRow(
                        label = "Анонимный доступ",
                        checked = ftpSettings.anonymousAccess,
                        onCheckedChange = { viewModel.setFtpAnonymousAccess(it) }
                    )
                }
            }
            item(key = "telnet") {
                SectionCard(title = "Telnet", icon = Icons.Default.Phone) {
                    SwitchRow(
                        label = "Включён",
                        checked = telnetSettings.enabled,
                        onCheckedChange = { viewModel.setTelnetEnabled(it) }
                    )
                    RowDivider()
                    EditableRow(
                        label = "Порт",
                        value = "${telnetSettings.port}",
                        onClick = { showTelnetPortDialog = true },
                        monospaceValue = true
                    )
                }
            }
            item(key = "http_proxy") {
                SectionCard(title = "HTTP Proxy", icon = Icons.Default.Public) {
                    SwitchRow(
                        label = "Включён",
                        checked = httpProxySettings.enabled,
                        onCheckedChange = { viewModel.setHttpProxyEnabled(it) }
                    )
                    RowDivider()
                    EditableRow(
                        label = "Порт",
                        value = "${httpProxySettings.port}",
                        onClick = { showHttpProxyPortDialog = true },
                        monospaceValue = true
                    )
                }
            }
            item(key = "components") {
                SectionCard(
                    title = "Компоненты и сервисы",
                    icon = Icons.Default.Extension,
                    subtitle = "SMB, DLNA, Transmission и другие через RCI",
                    trailing = {
                        TextButton(onClick = { viewModel.loadComponents() }) {
                            Text("Обновить", color = KeeneticColors.Primary)
                        }
                    }
                ) {
                    Text(
                        "Управление пакетами через RCI (components.component). Установка возможна при доступности пакета.",
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
                        EmptyHint("Нет данных о компонентах")
                    } else {
                        relevantComponents.forEach { comp ->
                            SshSnmpComponentRow(comp, viewModel, { feedbackMessage = it })
                        }
                    }
                }
            }
        }
    }

    if (feedbackMessage != null) {
        ConfirmDialog(
            title = "Действие",
            message = feedbackMessage ?: "",
            confirmLabel = "OK",
            onConfirm = { feedbackMessage = null },
            onDismiss = { feedbackMessage = null }
        )
    }

    if (showSshPortDialog) {
        var portInput by remember { mutableStateOf("${sshSettings.port}") }
        val portValid = portInput.toIntOrNull() in 1..65535
        FormDialog(
            title = "Порт SSH",
            onDismiss = { showSshPortDialog = false },
            onConfirm = {
                portInput.toIntOrNull()?.let { viewModel.setSshPort(it) }
                showSshPortDialog = false
            },
            confirmEnabled = portValid
        ) {
            OutlinedTextField(
                value = portInput,
                onValueChange = { portInput = it },
                label = { Text("Номер порта (1-65535)") },
                singleLine = true,
                isError = !portValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showSnmpCommunityDialog) {
        var communityInput by remember { mutableStateOf(snmpView.community) }
        FormDialog(
            title = "Community (SNMP)",
            onDismiss = { showSnmpCommunityDialog = false },
            onConfirm = {
                viewModel.setSnmpCommunity(communityInput)
                showSnmpCommunityDialog = false
            },
            confirmEnabled = communityInput.isNotBlank()
        ) {
            OutlinedTextField(
                value = communityInput,
                onValueChange = { communityInput = it },
                label = { Text("Community строка") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showFtpPortDialog) {
        var portInput by remember { mutableStateOf("${ftpSettings.port}") }
        val portValid = portInput.toIntOrNull() in 1..65535
        FormDialog(
            title = "Порт FTP",
            onDismiss = { showFtpPortDialog = false },
            onConfirm = {
                portInput.toIntOrNull()?.let { viewModel.setFtpPort(it) }
                showFtpPortDialog = false
            },
            confirmEnabled = portValid
        ) {
            OutlinedTextField(
                value = portInput,
                onValueChange = { portInput = it },
                label = { Text("Номер порта (1-65535)") },
                singleLine = true,
                isError = !portValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showTelnetPortDialog) {
        var portInput by remember { mutableStateOf("${telnetSettings.port}") }
        val portValid = portInput.toIntOrNull() in 1..65535
        FormDialog(
            title = "Порт Telnet",
            onDismiss = { showTelnetPortDialog = false },
            onConfirm = {
                portInput.toIntOrNull()?.let { viewModel.setTelnetPort(it) }
                showTelnetPortDialog = false
            },
            confirmEnabled = portValid
        ) {
            OutlinedTextField(
                value = portInput,
                onValueChange = { portInput = it },
                label = { Text("Номер порта (1-65535)") },
                singleLine = true,
                isError = !portValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showHttpProxyPortDialog) {
        var portInput by remember { mutableStateOf("${httpProxySettings.port}") }
        val portValid = portInput.toIntOrNull() in 1..65535
        FormDialog(
            title = "Порт HTTP Proxy",
            onDismiss = { showHttpProxyPortDialog = false },
            onConfirm = {
                portInput.toIntOrNull()?.let { viewModel.setHttpProxyPort(it) }
                showHttpProxyPortDialog = false
            },
            confirmEnabled = portValid
        ) {
            OutlinedTextField(
                value = portInput,
                onValueChange = { portInput = it },
                label = { Text("Номер порта (1-65535)") },
                singleLine = true,
                isError = !portValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SshSnmpComponentRow(comp: ComponentInfo, viewModel: RouterViewModel, onFeedback: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
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
