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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun TerminalScreen(viewModel: RouterViewModel) {
    var command by remember { mutableStateOf("") }
    var sshPort by remember { mutableStateOf("22") }
    var sshLogin by remember { mutableStateOf("") }
    var sshPassword by remember { mutableStateOf("") }
    var sshPasswordVisible by remember { mutableStateOf(false) }
    val output by viewModel.sshOutput.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val routerLogin by viewModel.routerLogin.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(routerLogin) {
        if (sshLogin.isBlank()) {
            sshLogin = routerLogin
        }
    }

    LaunchedEffect(output) {
        if (output.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    val runCommand: (String) -> Unit = { cmd ->
        viewModel.executeSsh(
            command = cmd,
            port = sshPort.toIntOrNull() ?: 22,
            login = sshLogin.ifBlank { null },
            password = sshPassword.ifBlank { null }
        )
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
                onClick = { runCommand("show system") },
                label = { Text("System") },
                leadingIcon = { Icon(Icons.Default.Terminal, null, Modifier.size(18.dp)) }
            )
            AssistChip(
                onClick = { runCommand("show ip hotspot") },
                label = { Text("Clients") },
                leadingIcon = { Icon(Icons.Default.Terminal, null, Modifier.size(18.dp)) }
            )
            AssistChip(
                onClick = { runCommand("show log tail 20") },
                label = { Text("Logs") },
                leadingIcon = { Icon(Icons.Default.Terminal, null, Modifier.size(18.dp)) }
            )
            AssistChip(
                onClick = { runCommand("ping 8.8.8.8 -c 4") },
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

        OutlinedTextField(
            value = sshLogin,
            onValueChange = { sshLogin = it },
            label = { Text("SSH логин") },
            placeholder = { Text(routerLogin.ifBlank { "admin" }) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = sshPassword,
            onValueChange = { sshPassword = it },
            label = { Text("SSH пароль") },
            placeholder = { Text("Если пусто, будет использован сохранённый пароль") },
            singleLine = true,
            visualTransformation = if (sshPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            trailingIcon = {
                IconButton(onClick = { sshPasswordVisible = !sshPasswordVisible }) {
                    Icon(
                        imageVector = if (sshPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = sshPort,
            onValueChange = { sshPort = it.filter(Char::isDigit).take(5) },
            label = { Text("SSH порт") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Ввод команды
        OutlinedTextField(
            value = command,
            onValueChange = { command = it },
            label = { Text("Команда") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                if (command.isNotBlank()) {
                    runCommand(command)
                    command = ""
                }
            }),
            trailingIcon = {
                Row {
                    IconButton(onClick = {
                        if (command.isNotBlank()) {
                            runCommand(command)
                            command = ""
                        }
                    }) {
                        Icon(Icons.Default.PlayArrow, "Выполнить")
                    }
                    IconButton(onClick = { runCommand("system reboot") }) {
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