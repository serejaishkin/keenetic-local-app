package com.keenetic.local.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun SshTerminalScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val terminalLog by viewModel.sshTerminalLog.collectAsState()
    val isExecuting by viewModel.isSshExecuting.collectAsState()
    val sshPort by viewModel.sshPort.collectAsState()

    var command by remember { mutableStateOf("") }

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
                Icon(Icons.Default.Terminal, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "SSH-терминал",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = KeeneticColors.TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.clearSshTerminal() }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Очистить", tint = KeeneticColors.TextSecondary)
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Быстрые команды", style = MaterialTheme.typography.titleSmall, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickCommand("show version", command) { command = it }
                        QuickCommand("show interface", command) { command = it }
                        QuickCommand("show log tail 30", command) { command = it }
                        QuickCommand("show ip route full", command) { command = it }
                        QuickCommand("system configuration save", command) { command = it }
                        QuickCommand("ping 192.168.3.1 -c 4", command) { command = it }
                    }
                    Text(
                        "Требуется включённый SSH-сервер на роутере и пароль администратора (раздел «Сетевые сервисы»).",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Команда CLI (например: show ip route)") },
                    singleLine = true,
                    enabled = !isExecuting
                )
                Button(
                    onClick = {
                        viewModel.runSshCommand(command)
                        command = ""
                    },
                    enabled = command.isNotBlank() && !isExecuting,
                    colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isExecuting) "Выполняется..." else "Выполнить")
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Article, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Вывод", style = MaterialTheme.typography.titleSmall, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("SSH порт: ${sshPort.ifBlank { "22" }}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 500.dp)
                    ) {
                        Text(
                            text = terminalLog.ifBlank { "Введите команду CLI и нажмите «Выполнить».\nВывод появится здесь." },
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = KeeneticColors.TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickCommand(label: String, current: String, onPick: (String) -> Unit) {
    val selected = current.trim().equals(label.trim(), ignoreCase = true)
    OutlinedButton(
        onClick = { onPick(label) },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) KeeneticColors.Primary.copy(alpha = 0.15f) else KeeneticColors.Surface,
            contentColor = KeeneticColors.TextPrimary
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) KeeneticColors.Primary else KeeneticColors.Divider
        )
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}