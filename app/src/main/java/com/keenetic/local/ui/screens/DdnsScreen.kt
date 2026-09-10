package com.keenetic.local.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

private val ddnsProviders = listOf(
    "noip" to "No-IP",
    "dyndns" to "DynDNS",
    "regru" to "regru",
    "rucenter" to "RU-CENTER",
    "opendns" to "OpenDNS",
    "dnsomatic" to "DNS-O-Matic",
    "anydns" to "anydns",
    "dnshome" to "dnshome",
    "duckdns" to "duckdns",
    "dyndnsfree" to "dyndnsfree",
    "desec" to "deSEC",
    "dynu" to "dynu",
    "custom" to "Другой"
)

@Composable
fun DdnsScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val dyndnsStatus by viewModel.dyndnsStatus.collectAsState()
    val updaters by viewModel.dyndnsUpdaters.collectAsState()
    val profiles by viewModel.dyndnsProfiles.collectAsState()
    val interfaces by viewModel.interfaces.collectAsState()

    var provider by remember { mutableStateOf("noip") }
    var url by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var autoDetectIp by remember { mutableStateOf(true) }
    var selectedInterfaces by remember { mutableStateOf(setOf<String>()) }
    var showPassword by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    // Prefill from current router settings when they arrive.
    LaunchedEffect(dyndnsStatus) {
        if (dyndnsStatus.hostname.isNotBlank() && ynderAlias(dyndnsStatus.provider) != null) {
            provider = ynderAlias(dyndnsStatus.provider) ?: provider
            domain = dyndnsStatus.hostname
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadDyndnsStatus()
        if (interfaces.isEmpty()) viewModel.loadInterfaces()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary) }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Public, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Динамический DNS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.loadDyndnsStatus() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = KeeneticColors.Primary)
                }
            }
        }

        if (feedbackMessage != null) {
            item {
                Snackbar(
                    action = {
                        TextButton(onClick = { feedbackMessage = null }) {
                            Text("OK", color = KeeneticColors.Primary)
                        }
                    }
                ) {
                    Text(feedbackMessage ?: "")
                }
            }
        }

        // Статус
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Статус", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Включён", color = KeeneticColors.TextPrimary)
                        Switch(checked = dyndnsStatus.enabled, onCheckedChange = { viewModel.setDyndnsEnabled(it) })
                    }
                    InfoRow("Доменное имя", dyndnsStatus.hostname.ifBlank { "—" })
                    InfoRow("Последнее обновление", dyndnsStatus.lastUpdate.ifBlank { "—" })
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Настройки сервиса", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)

                    // Провайдер
                    Text("Провайдер DDNS", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                    var expandedProvider by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { expandedProvider = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(ddnsProviders.firstOrNull { it.first == provider }?.second ?: provider, color = KeeneticColors.TextPrimary, modifier = Modifier.weight(1f))
                        }
                        DropdownMenu(
                            expanded = expandedProvider,
                            onDismissRequest = { expandedProvider = false },
                            modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 400.dp)
                        ) {
                            ddnsProviders.forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        provider = key
                                        expandedProvider = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Адрес сервиса (URL)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = domain,
                        onValueChange = { domain = it },
                        label = { Text("Доменное имя") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Имя пользователя") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Пароль") },
                        singleLine = true,
                        visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None
                            else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Показать пароль",
                                    tint = KeeneticColors.TextSecondary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = KeeneticColors.Divider)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Автоматически из IP-адреса подключения", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary, modifier = Modifier.weight(1f))
                        Switch(checked = autoDetectIp, onCheckedChange = { autoDetectIp = it })
                    }

                    HorizontalDivider(color = KeeneticColors.Divider)

                    Text("Интерфейсы, для которых действует DDNS", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextSecondary)
                    if (interfaces.isEmpty()) {
                        Text("Интерфейсы не загружены", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                    } else {
                        interfaces.forEach { iface ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedInterfaces = if (iface.name in selectedInterfaces) selectedInterfaces - iface.name else selectedInterfaces + iface.name
                                    }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = iface.name in selectedInterfaces,
                                    onCheckedChange = { checked ->
                                        selectedInterfaces = if (checked) selectedInterfaces + iface.name else selectedInterfaces - iface.name
                                    }
                                )
                                Column {
                                    Text(iface.name, style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary)
                                    Text(iface.description, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary, maxLines = 1)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = KeeneticColors.Divider)

                    Button(
                        onClick = {
                            if (domain.isBlank()) {
                                feedbackMessage = "Укажите доменное имя"
                                return@Button
                            }
                            viewModel.saveDyndnsProfile(
                                provider = provider,
                                url = url,
                                domain = domain,
                                username = username,
                                password = password,
                                autoDetectIp = autoDetectIp,
                                interfaces = selectedInterfaces.toList()
                            )
                            feedbackMessage = "Настройки DDNS отправлены на роутер"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Применить")
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.deleteDyndnsProfile()
                            feedbackMessage = "Профиль DDNS удалён"
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = KeeneticColors.Error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Удалить провайдера")
                    }
                }
            }
        }

        if (profiles.isNotEmpty()) {
            item { Text("Сохранённые профили", style = MaterialTheme.typography.titleSmall, color = KeeneticColors.TextPrimary, modifier = Modifier.padding(horizontal = 4.dp)) }
            items(profiles) { profile ->
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(profile.name.ifBlank { profile.hostname }, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                        Text("Хост: ${profile.hostname}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                        Text("Пользователь: ${profile.username}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                    }
                }
            }
        }

        if (updaters.isNotEmpty()) {
            item { Text("Обновления", style = MaterialTheme.typography.titleSmall, color = KeeneticColors.TextPrimary, modifier = Modifier.padding(horizontal = 4.dp)) }
            items(updaters) { updater ->
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(updater.name, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                        Text("Хост: ${updater.hostname}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                        Text("Статус: ${updater.status}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                        Text("Обновлено: ${updater.lastUpdate}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                    }
                }
            }
        }
    }
}

private fun ynderAlias(key: String): String? =
    ddnsProviders.firstOrNull { it.first == key }?.first

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Medium)
    }
}