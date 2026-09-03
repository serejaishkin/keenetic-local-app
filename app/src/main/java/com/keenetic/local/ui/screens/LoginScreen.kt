package com.keenetic.local.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@OptIn(ExperimentalMaterial3Api::class)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Keenetic Local",
                        style = MaterialTheme.typography.headlineMedium,
                        color = KeeneticColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Прямое подключение в локальной сети",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KeeneticColors.TextSecondary
                    )
                }

                // Gateway detection banner if available
                detectedGateway?.let { gwIp ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = KeeneticColors.Primary.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Router,
                                    contentDescription = null,
                                    tint = KeeneticColors.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "Шлюз сети:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = KeeneticColors.TextSecondary
                                    )
                                    Text(
                                        gwIp,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = KeeneticColors.TextPrimary
                                    )
                                }
                            }
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
                    }
                }

                // IP Address input field
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

                // Quick IP Substitution Chips
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Быстрая подстановка IP:",
                        style = MaterialTheme.typography.labelSmall,
                        color = KeeneticColors.TextSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
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
                }

                // Discovered Routers List (if any discovered via scan)
                AnimatedVisibility(visible = discoveredRouters.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Найденные роутеры Keenetic:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = KeeneticColors.Success
                        )
                        discoveredRouters.forEach { router ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = KeeneticColors.CardBorder.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
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

                // Username input
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

                // Password input with visibility toggle
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

                // Advanced connection settings (HTTPS / Port)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showAdvanced = !showAdvanced }) {
                        Icon(
                            if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showAdvanced) "Скрыть доп. настройки" else "Порт и протокол (HTTPS)")
                    }
                }

                AnimatedVisibility(visible = showAdvanced) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("HTTPS соединение", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = useHttps,
                                onCheckedChange = {
                                    useHttps = it
                                    port = if (it) "443" else "80"
                                }
                            )
                        }
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text("Порт (80 для HTTP, 443 для HTTPS)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Error Message Card
                error?.let {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Error.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = KeeneticColors.Error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                it,
                                color = KeeneticColors.Error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Connect Button
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

                // Demo Mode Button
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
}
