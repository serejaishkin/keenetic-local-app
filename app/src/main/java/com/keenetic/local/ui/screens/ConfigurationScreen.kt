package com.keenetic.local.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keenetic.local.api.KeeneticParsedConfig
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.EmptyHint
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.RowDivider
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SubGroupHeader
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun ConfigurationScreen(
    viewModel: RouterViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val rawConfig by viewModel.rawRunningConfig.collectAsState()
    val parsedConfig by viewModel.parsedConfig.collectAsState()
    val isLoading by viewModel.configLoading.collectAsState()
    val cliResult by viewModel.cliExecutionResult.collectAsState()
    val isExecutingCli by viewModel.isExecutingCli.collectAsState()
    val saveMessage by viewModel.saveConfigMessage.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var cliCommand by remember { mutableStateOf("") }
    var configSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (rawConfig.isBlank()) {
            viewModel.fetchRunningConfig()
        }
    }

    SectionScaffold(
        title = "Конфигурация роутера",
        subtitle = "NDM CLI & RCI команды",
        onBack = onBack,
        onRefresh = { viewModel.fetchRunningConfig() }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Save to NVRAM notification banner
            saveMessage?.let { msg ->
                SectionCard(
                    title = "Сохранение конфигурации",
                    icon = Icons.Default.CheckCircle
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearSaveConfigMessage() }) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = KeeneticColors.TextSecondary)
                        }
                    }
                    RowDivider()
                    Button(
                        onClick = { viewModel.saveConfigurationToNvram() },
                        enabled = !isExecutingCli,
                        colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Сохранить в NVRAM")
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = KeeneticColors.Surface,
                contentColor = KeeneticColors.Primary,
                divider = { HorizontalDivider(color = KeeneticColors.Divider) }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Инспектор") },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Running-Config") },
                    icon = { Icon(Icons.Default.Description, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("RCI / CLI Терминал") },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = null) }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KeeneticColors.Primary)
                }
            } else {
                when (selectedTab) {
                    0 -> ConfigInspectorTab(parsed = parsedConfig, onRefresh = { viewModel.fetchRunningConfig() })
                    1 -> RawConfigTab(
                        rawText = rawConfig,
                        searchQuery = configSearchQuery,
                        onSearchChange = { configSearchQuery = it },
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Keenetic Running Config", rawConfig))
                            Toast.makeText(context, "Конфигурация скопирована в буфер обмена", Toast.LENGTH_SHORT).show()
                        }
                    )
                    2 -> CliTerminalTab(
                        command = cliCommand,
                        onCommandChange = { cliCommand = it },
                        onExecute = { cmd -> viewModel.executeRawCliOrRci(cmd) },
                        result = cliResult,
                        isExecuting = isExecutingCli,
                        onClearResult = { viewModel.clearCliResult() }
                    )
                }
            }
        }
    }
}

@Composable
fun ConfigInspectorTab(
    parsed: KeeneticParsedConfig?,
    onRefresh: () -> Unit
) {
    if (parsed == null || parsed.rawLinesCount == 0) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.SettingsEthernet, contentDescription = null, tint = KeeneticColors.TextSecondary, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            EmptyHint(text = "Конфигурация еще не загружена с роутера")
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
            ) {
                Text("Загрузить с роутера")
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Router Header Info
        item {
            SectionCard(
                title = parsed.hostname.ifBlank { "Keenetic" },
                icon = Icons.Default.Router,
                subtitle = parsed.model.ifBlank { "KeeneticOS" }
            ) {
                InfoRow(label = "Версия ОС", value = parsed.version.ifBlank { "н/д" })
                RowDivider()
                InfoRow(label = "Часовой пояс", value = parsed.timezone.ifBlank { "default" })
                RowDivider()
                InfoRow(label = "Пользователь", value = parsed.username.ifBlank { "admin" })
                RowDivider()
                InfoRow(label = "Строк в конфиге", value = "${parsed.rawLinesCount} команд RCI/CLI")
            }
        }

        // Connection Policies (PBR)
        item {
            SectionCard(
                title = "Политики маршрутизации (PBR)",
                icon = Icons.AutoMirrored.Filled.AltRoute,
                subtitle = if (parsed.policies.isEmpty()) "Не заданы" else "${parsed.policies.size} шт."
            ) {
                if (parsed.policies.isEmpty()) {
                    EmptyHint(text = "Политики маршрутизации не заданы")
                } else {
                    parsed.policies.forEachIndexed { index, pol ->
                        if (index > 0) RowDivider()
                        SubGroupHeader(title = pol.id)
                        InfoRow(label = "Описание", value = pol.description.ifBlank { "—" })
                        if (pol.permitInterfaces.isNotEmpty()) {
                            InfoRow(label = "Интерфейсы", value = pol.permitInterfaces.joinToString(", "))
                        }
                        if (pol.isMultipath) {
                            InfoRow(label = "Режим", value = "multipath")
                        }
                    }
                }
            }
        }

        // Known Hosts & Policy Assignments
        item {
            SectionCard(
                title = "Зарегистрированные устройства",
                icon = Icons.Default.Devices,
                subtitle = "${parsed.knownHosts.size} устройств"
            ) {
                if (parsed.knownHosts.isEmpty()) {
                    EmptyHint(text = "Устройства не найдены")
                } else {
                    SubGroupHeader(title = "Устройства", count = parsed.knownHosts.size)
                    parsed.knownHosts.take(8).forEach { host ->
                        val assignedPolicy = parsed.hotspotAssignments.find { it.mac.equals(host.mac, ignoreCase = true) }?.policy
                        RowDivider()
                        InfoRow(
                            label = host.name,
                            value = if (!assignedPolicy.isNullOrBlank()) assignedPolicy else "Основная",
                            monospace = true
                        )
                        Text(
                            host.mac,
                            color = KeeneticColors.TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (parsed.knownHosts.size > 8) {
                        EmptyHint(text = "+ еще ${parsed.knownHosts.size - 8} устройств")
                    }
                }
            }
        }

        // Wi-Fi Access Points from config
        item {
            SectionCard(
                title = "Беспроводные точки доступа",
                icon = Icons.Default.Wifi,
                subtitle = if (parsed.wifiNetworks.isEmpty()) "Не обнаружены" else "${parsed.wifiNetworks.size} шт."
            ) {
                if (parsed.wifiNetworks.isEmpty()) {
                    EmptyHint(text = "Сети не обнаружены")
                } else {
                    parsed.wifiNetworks.forEachIndexed { index, wifi ->
                        if (index > 0) RowDivider()
                        InfoRow(label = wifi.ssid, value = if (wifi.isUp) "Включена" else "Выключена")
                        Text(
                            "${wifi.master} / ${wifi.accessPoint} (${wifi.security})",
                            color = KeeneticColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // DNS Upstreams (DoT / DoH)
        item {
            SectionCard(
                title = "DNS-Proxy Upstreams (DoT / DoH)",
                icon = Icons.Default.Dns,
                subtitle = if (parsed.dnsUpstreams.isEmpty()) "Не настроены" else "${parsed.dnsUpstreams.size} шт."
            ) {
                if (parsed.dnsUpstreams.isEmpty()) {
                    EmptyHint(text = "Кастомные DNS апстримы не настроены")
                } else {
                    parsed.dnsUpstreams.forEachIndexed { index, dns ->
                        if (index > 0) RowDivider()
                        InfoRow(label = dns.type.uppercase(), value = dns.upstream)
                        if (dns.viaInterface.isNotBlank()) {
                            InfoRow(label = "Интерфейс", value = dns.viaInterface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RawConfigTab(
    rawText: String,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onCopy: () -> Unit
) {
    val filteredLines = remember(rawText, searchQuery) {
        if (searchQuery.isBlank()) {
            rawText.lines()
        } else {
            rawText.lines().filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "Поиск и копия", icon = Icons.Default.Search) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Поиск команд...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = KeeneticColors.TextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Очистить", tint = KeeneticColors.TextSecondary)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            InfoRow(label = "Показано строк", value = "${filteredLines.size}")
            Button(
                onClick = onCopy,
                colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Копия")
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = KeeneticColors.TerminalBg),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            SelectionContainer {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    items(filteredLines) { line ->
                        val textColor = when {
                            line.startsWith("!") -> KeeneticColors.TextSecondary
                            line.startsWith("interface ") -> KeeneticColors.Primary
                            line.startsWith("ip policy ") -> KeeneticColors.Warning
                            line.startsWith("known host ") -> KeeneticColors.Success
                            line.startsWith("no ") -> KeeneticColors.Error.copy(alpha = 0.85f)
                            else -> KeeneticColors.TextPrimary
                        }
                        Text(
                            text = line,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = textColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CliTerminalTab(
    command: String,
    onCommandChange: (String) -> Unit,
    onExecute: (String) -> Unit,
    result: String?,
    isExecuting: Boolean,
    onClearResult: () -> Unit
) {
    val presets = listOf(
        "show running-config",
        "system configuration save",
        "show ip hotspot",
        "show ip policy",
        "show version",
        "show interface",
        "system reboot"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "Быстрые RCI / CLI команды", icon = Icons.Default.Terminal) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    ActionChip(
                        label = preset,
                        onClick = {
                            onCommandChange(preset)
                            onExecute(preset)
                        }
                    )
                }
            }
            OutlinedTextField(
                value = command,
                onValueChange = onCommandChange,
                placeholder = { Text("Введите CLI команду или JSON RCI...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onExecute(command) },
                    enabled = command.isNotBlank() && !isExecuting,
                    colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isExecuting) {
                        CircularProgressIndicator(color = KeeneticColors.Background, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Выполнить")
                    }
                }

                if (result != null) {
                    OutlinedButton(
                        onClick = onClearResult,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = KeeneticColors.TextSecondary)
                    ) {
                        Text("Очистить вывод")
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = KeeneticColors.TerminalBg),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            SelectionContainer {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    item {
                        Text(
                            text = result ?: "# Терминал готов к выполнению команд роутера Keenetic.\n# Введите команду CLI (например: 'show ip policy' или 'system configuration save')\n# Либо JSON запрос к /rci/ (например: [{\"show\": {\"version\": {}}}])",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = if (result != null) KeeneticColors.TerminalText else KeeneticColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = KeeneticColors.SurfaceElevated,
        modifier = Modifier.height(32.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = KeeneticColors.Primary,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
