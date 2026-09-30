package com.keenetic.local.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.RowDivider
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SubGroupHeader
import com.keenetic.local.ui.components.SwitchRow
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun LoginScreen(viewModel: RouterViewModel) {
    val detectedGateway by viewModel.detectedGatewayIp.collectAsState()
    val suggestedIps by viewModel.suggestedIps.collectAsState()
    val savedIp by viewModel.savedIp.collectAsState()
    val savedPort by viewModel.savedPort.collectAsState()
    val savedUsername by viewModel.savedUsername.collectAsState()
    val savedUseHttps by viewModel.savedUseHttps.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val discoveredRouters by viewModel.discoveredRouters.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var host by remember { mutableStateOf("192.168.1.1") }
    var port by remember { mutableStateOf("80") }
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var useHttps by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }

    // Auto-substitute IP from saved settings or detected gateway on initial load
    var hasAutoPopulated by remember { mutableStateOf(false) }
    LaunchedEffect(savedIp, detectedGateway) {
        if (!hasAutoPopulated) {
            val candidate = if (savedIp.isNotBlank() && savedIp != "192.168.1.1") {
                savedIp
            } else if (!detectedGateway.isNullOrBlank()) {
                detectedGateway!!
            } else {
                savedIp
            }
            if (candidate.isNotBlank()) {
                host = candidate
                hasAutoPopulated = true
            }
        }
    }

    LaunchedEffect(savedPort) {
        if (savedPort.isNotBlank()) port = savedPort
    }
    LaunchedEffect(savedUsername) {
        if (savedUsername.isNotBlank()) username = savedUsername
    }
    LaunchedEffect(savedUseHttps) {
        useHttps = savedUseHttps
        if (useHttps && port == "80") port = "443"
    }

    SectionScaffold(
        title = "Keenetic Local",
        subtitle = "Прямое подключение в локальной сети",
        onBack = null
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            detectedGateway?.let { gwIp ->
                SectionCard(
                    title = "Шлюз сети",
                    icon = Icons.Default.Router,
                    subtitle = gwIp,
                    trailing = {
                        FilledTonalButton(
                            onClick = { host = gwIp },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = KeeneticColors.Primary,
                                contentColor = KeeneticColors.Background
                            )
                        ) {
                            Text("Подставить", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                ) {
                    InfoRow(label = "Адрес шлюза", value = gwIp, monospace = true)
                }
            }

            SectionCard(
                title = "Подключение",
                icon = Icons.Default.Dns,
                subtitle = "IP адрес роутера"
            ) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("IP адрес или хост роутера") },
                    leadingIcon = {
                        Icon(Icons.Default.Dns, contentDescription = null, tint = KeeneticColors.TextSecondary)
                    },
                    trailingIcon = {
                        IconButton(onClick = { viewModel.scanNetwork() }) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = KeeneticColors.Primary
                                )
                            } else {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Сканировать сеть",
                                    tint = KeeneticColors.Primary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                SubGroupHeader(title = "Быстрая подстановка IP", count = suggestedIps.size)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestedIps.forEach { ip ->
                        val isSelected = host.trim() == ip
                        FilterChip(
                            selected = isSelected,
                            onClick = { host = ip },
                            label = {
                                Text(
                                    if (ip == detectedGateway) "★ $ip (шлюз)" else ip,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = if (ip == detectedGateway) {
                                { Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null
                        )
                    }
                }

                AnimatedVisibility(visible = discoveredRouters.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RowDivider()
                        SubGroupHeader(title = "Найденные роутеры Keenetic", count = discoveredRouters.size)
                        discoveredRouters.forEach { router ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        router.hostname ?: "Keenetic",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = KeeneticColors.TextPrimary
                                    )
                                    Text(
                                        router.ip,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = KeeneticColors.TextSecondary
                                    )
                                }
                                TextButton(onClick = { host = router.ip }) {
                                    Text("Выбрать")
                                }
                            }
                        }
                    }
                }
            }

            SectionCard(
                title = "Учётные данные",
                icon = Icons.Default.Person,
                subtitle = "Логин и пароль администратора"
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Логин") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = KeeneticColors.TextSecondary)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = KeeneticColors.TextSecondary)
                    },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) "Скрыть" else "Показать"
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SectionCard(
                title = "Дополнительно",
                icon = if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                subtitle = "Порт и протокол (HTTPS)",
                trailing = {
                    TextButton(onClick = { showAdvanced = !showAdvanced }) {
                        Text(if (showAdvanced) "Скрыть" else "Показать")
                    }
                }
            ) {
                AnimatedVisibility(visible = showAdvanced) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SwitchRow(
                            label = "HTTPS соединение",
                            checked = useHttps,
                            onCheckedChange = {
                                useHttps = it
                                port = if (it) "443" else "80"
                            }
                        )
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text("Порт (80 для HTTP, 443 для HTTPS)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                if (!showAdvanced) {
                    InfoRow(label = "Протокол", value = if (useHttps) "HTTPS : $port" else "HTTP : $port")
                }
            }

            error?.let {
                SectionCard(title = "Ошибка входа", icon = Icons.Default.Warning) {
                    Text(
                        it,
                        color = KeeneticColors.Error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Button(
                onClick = {
                    viewModel.login(
                        host = host,
                        port = port,
                        user = username,
                        pass = password,
                        useHttps = useHttps
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary),
                enabled = !isLoading && host.isNotBlank() && username.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = KeeneticColors.Background,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        "Подключиться к роутеру",
                        color = KeeneticColors.Background,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            OutlinedButton(
                onClick = { viewModel.loadDemoData() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = KeeneticColors.Primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Открыть Демо-режим", color = KeeneticColors.Primary)
            }
        }
    }
}
