package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun TerminalScreen(viewModel: RouterViewModel) {
    var command by remember { mutableStateOf("") }
    val output by viewModel.sshOutput.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(output) {
        if (output.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "SSH Терминал",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        Text(
            text = "Выполняйте команды NDMS CLI напрямую",
            style = MaterialTheme.typography.bodyMedium,
            color = KeeneticColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Быстрые команды
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = { viewModel.executeSsh("show system") },
                label = { Text("System") },
                leadingIcon = { Icon(Icons.Default.Terminal, null, Modifier.size(18.dp)) }
            )
            AssistChip(
                onClick = { viewModel.executeSsh("show ip hotspot") },
                label = { Text("Clients") },
                leadingIcon = { Icon(Icons.Default.Terminal, null, Modifier.size(18.dp)) }
            )
            AssistChip(
                onClick = { viewModel.executeSsh("show log tail 20") },
                label = { Text("Logs") },
                leadingIcon = { Icon(Icons.Default.Terminal, null, Modifier.size(18.dp)) }
            )
            AssistChip(
                onClick = { viewModel.executeSsh("ping 8.8.8.8 -c 4") },
                label = { Text("Ping") },
                leadingIcon = { Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp)) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Вывод
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                item {
                    Text(
                        text = output.ifEmpty { "Введите команду или выберите быструю..." },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFF00FF00),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Ввод команды
        OutlinedTextField(
            value = command,
            onValueChange = { command = it },
            label = { Text("Команда") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                if (command.isNotBlank()) {
                    viewModel.executeSsh(command)
                    command = ""
                }
            }),
            trailingIcon = {
                Row {
                    IconButton(onClick = {
                        if (command.isNotBlank()) {
                            viewModel.executeSsh(command)
                            command = ""
                        }
                    }) {
                        Icon(Icons.Default.PlayArrow, "Выполнить")
                    }
                    IconButton(onClick = { viewModel.executeSsh("system reboot") }) {
                        Icon(Icons.Default.RestartAlt, "Reboot", tint = KeeneticColors.Error)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = KeeneticColors.Primary)
        }
    }
}
