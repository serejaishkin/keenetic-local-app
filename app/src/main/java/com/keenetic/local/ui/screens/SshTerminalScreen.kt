package com.keenetic.local.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.RowDivider
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SubGroupHeader
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun SshTerminalScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val terminalLog by viewModel.sshTerminalLog.collectAsState()
    val isExecuting by viewModel.isSshExecuting.collectAsState()
    val sshPort by viewModel.sshPort.collectAsState()

    var command by remember { mutableStateOf("") }

    SectionScaffold(
        title = "SSH-терминал",
        subtitle = "Выполнение команд CLI по SSH",
        onBack = onBack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard(
                title = "Быстрые команды",
                icon = Icons.Default.Terminal,
                subtitle = "Нажмите, чтобы подставить в поле ввода",
                trailing = {
                    IconButton(onClick = { viewModel.clearSshTerminal() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Очистить", tint = KeeneticColors.TextSecondary)
                    }
                }
            ) {
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
                RowDivider()
                SubGroupHeader(title = "Подсказка")
                Text(
                    "Требуется включённый SSH-сервер на роутере и пароль администратора (раздел «Сетевые сервисы»).",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
            }

            SectionCard(
                title = "Команда",
                icon = Icons.Default.Send,
                subtitle = "SSH порт: ${sshPort.ifBlank { "22" }}"
            ) {
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    modifier = Modifier.fillMaxWidth(),
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
                    colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isExecuting) "Выполняется..." else "Выполнить")
                }
            }

            SectionCard(
                title = "Вывод",
                icon = Icons.Default.Article,
                subtitle = "Тёмное терминальное окно"
            ) {
                InfoRow(label = "SSH порт", value = sshPort.ifBlank { "22" }, monospace = true)
                RowDivider()
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = KeeneticColors.TerminalBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = terminalLog.ifBlank { "Введите команду CLI и нажмите «Выполнить».\nВывод появится здесь." },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = if (terminalLog.isBlank()) KeeneticColors.TextSecondary else KeeneticColors.TerminalText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp)
                            .padding(12.dp)
                    )
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
