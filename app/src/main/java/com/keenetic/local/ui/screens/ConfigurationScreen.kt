package com.keenetic.local.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keenetic.local.api.KeeneticParsedConfig
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Конфигурация роутера",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = KeeneticColors.TextPrimary
                        )
                        Text(
                            "NDM CLI & RCI команды",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = KeeneticColors.TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveConfigurationToNvram() },
                        enabled = !isExecutingCli
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Сохранить в NVRAM",
                            tint = KeeneticColors.Primary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.fetchRunningConfig() },
                        enabled = !isLoading
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Обновить конфиг",
                            tint = KeeneticColors.TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KeeneticColors.Surface
                )
            )
        },
        containerColor = KeeneticColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Save to NVRAM notification banner
            saveMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = KeeneticColors.Success.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = KeeneticColors.Success)
                            Text(msg, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextPrimary)
                        }
                        IconButton(onClick = { viewModel.clearSaveConfigMessage() }) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = KeeneticColors.TextSecondary)
                        }
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
            Text("Конфигурация еще не загружена с роутера", color = KeeneticColors.TextSecondary)
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Router Header Info
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Router, contentDescription = null, tint = KeeneticColors.Primary)
                            Text(
                                parsed.hostname.ifBlank { "Keenetic" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = KeeneticColors.TextPrimary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = KeeneticColors.Primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                parsed.model.ifBlank { "KeeneticOS" },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = KeeneticColors.Primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = KeeneticColors.Divider)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Версия ОС:", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text(parsed.version.ifBlank { "н/д" }, color = KeeneticColors.TextPrimary, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Часовой пояс:", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text(parsed.timezone.ifBlank { "default" }, color = KeeneticColors.TextPrimary, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Пользователь:", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text(parsed.username.ifBlank { "admin" }, color = KeeneticColors.TextPrimary, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Строк в конфиге:", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text("${parsed.rawLinesCount} команд RCI/CLI", color = KeeneticColors.Primary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Connection Policies (PBR)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.AutoMirrored.Filled.AltRoute, contentDescription = null, tint = KeeneticColors.Primary)
                        Text(
                            "Политики маршрутизации (PBR)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = KeeneticColors.TextPrimary
                        )
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)

                    if (parsed.policies.isEmpty()) {
                        Text("Политики маршрутизации не заданы", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    } else {
                        parsed.policies.forEach { pol ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = KeeneticColors.SurfaceElevated,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(
                                            pol.id,
                                            fontWeight = FontWeight.Bold,
                                            color = KeeneticColors.Primary,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (pol.isMultipath) {
                                            Text(
                                                "multipath",
                                                color = KeeneticColors.Warning,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                    if (pol.description.isNotBlank()) {
                                        Text(
                                            "Описание: ${pol.description}",
                                            color = KeeneticColors.TextSecondary,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    if (pol.permitInterfaces.isNotEmpty()) {
                                        Text(
                                            "Интерфейсы: ${pol.permitInterfaces.joinToString(", ")}",
                                            color = KeeneticColors.Success,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Known Hosts & Policy Assignments
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Devices, contentDescription = null, tint = KeeneticColors.Primary)
                            Text(
                                "Зарегистрированные устройства",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = KeeneticColors.TextPrimary
                            )
                        }
                        Text("${parsed.knownHosts.size} устройств", style = MaterialTheme.typography.labelMedium, color = KeeneticColors.Primary)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)

                    parsed.knownHosts.take(8).forEach { host ->
                        val assignedPolicy = parsed.hotspotAssignments.find { it.mac.equals(host.mac, ignoreCase = true) }?.policy
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(host.name, fontWeight = FontWeight.Medium, color = KeeneticColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
                                Text(host.mac, color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                            }
                            if (!assignedPolicy.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = KeeneticColors.Primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        assignedPolicy,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = KeeneticColors.Primary
                                    )
                                }
                            } else {
                                Text("Основная", style = MaterialTheme.typography.labelSmall, color = KeeneticColors.TextSecondary)
                            }
                        }
                    }
                    if (parsed.knownHosts.size > 8) {
                        Text("+ еще ${parsed.knownHosts.size - 8} устройств", style = MaterialTheme.typography.labelSmall, color = KeeneticColors.TextSecondary)
                    }
                }
            }
        }

        // Wi-Fi Access Points from config
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = KeeneticColors.Primary)
                        Text(
                            "Беспроводные точки доступа",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = KeeneticColors.TextPrimary
                        )
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)

                    if (parsed.wifiNetworks.isEmpty()) {
                        Text("Сети не обнаружены", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    } else {
                        parsed.wifiNetworks.forEach { wifi ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(wifi.ssid, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                                    Text("${wifi.master} / ${wifi.accessPoint} (${wifi.security})", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (wifi.isUp) KeeneticColors.Success.copy(alpha = 0.15f) else KeeneticColors.TextSecondary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        if (wifi.isUp) "Включена" else "Выключена",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = if (wifi.isUp) KeeneticColors.Success else KeeneticColors.TextSecondary,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // DNS Upstreams (DoT / DoH)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Dns, contentDescription = null, tint = KeeneticColors.Primary)
                        Text(
                            "DNS-Proxy Upstreams (DoT / DoH)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = KeeneticColors.TextPrimary
                        )
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)

                    if (parsed.dnsUpstreams.isEmpty()) {
                        Text("Кастомные DNS апстримы не настроены", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    } else {
                        parsed.dnsUpstreams.forEach { dns ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = KeeneticColors.Primary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            dns.type.uppercase(),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = KeeneticColors.Primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(dns.upstream, color = KeeneticColors.TextPrimary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                }
                                if (dns.viaInterface.isNotBlank()) {
                                    Text("через ${dns.viaInterface}", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.labelSmall)
                                }
                            }
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = KeeneticColors.Primary,
                    unfocusedBorderColor = KeeneticColors.Border,
                    focusedTextColor = KeeneticColors.TextPrimary,
                    unfocusedTextColor = KeeneticColors.TextPrimary
                )
            )

            Button(
                onClick = onCopy,
                colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Копия")
            }
        }

        Text(
            "Показано строк: ${filteredLines.size}",
            style = MaterialTheme.typography.labelSmall,
            color = KeeneticColors.TextSecondary
        )

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
        Text(
            "Быстрые RCI / CLI команды:",
            style = MaterialTheme.typography.labelMedium,
            color = KeeneticColors.TextSecondary
        )

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
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = KeeneticColors.Primary,
                unfocusedBorderColor = KeeneticColors.Border,
                focusedTextColor = KeeneticColors.TextPrimary,
                unfocusedTextColor = KeeneticColors.TextPrimary
            )
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
