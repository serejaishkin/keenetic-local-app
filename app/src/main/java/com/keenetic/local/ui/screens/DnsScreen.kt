package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.RouterInterface
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.screens.common.ApiCallState
import com.keenetic.local.ui.theme.KeeneticColors

private enum class DnsServerKind(val title: String, val addLabel: String) {
    Doh("Защищённые DNS (DoH)", "Добавить DoH-сервер"),
    Dot("Защищённые DNS (DoT)", "Добавить DoT-сервер"),
    Plain("Обычные DNS-серверы", "Добавить DNS-сервер")
}

private data class DnsServerEntry(
    val value: String,
    val interfaceName: String?,
    val deletable: Boolean = true
)

private data class DnsServerEditor(
    val kind: DnsServerKind,
    val address: String = "",
    val fqdn: String = "",
    val interfaceName: String = "",
    val isEdit: Boolean = false
)

private data class DnsServerRemoval(
    val kind: DnsServerKind,
    val address: String,
    val interfaceName: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val nameServers by viewModel.nameServers.collectAsState()
    val dohServers by viewModel.dohServers.collectAsState()
    val dotServers by viewModel.dotServers.collectAsState()
    val interfaces by viewModel.interfaces.collectAsState()
    val intercept by viewModel.dnsInterceptEnabled.collectAsState()
    val filterPresets by viewModel.dnsFilterPresetList.collectAsState()
    val filterProfiles by viewModel.dnsFilterProfileList.collectAsState()
    val filterPresetsState by viewModel.dnsFilterPresets.collectAsState()
    val filterProfilesState by viewModel.dnsFilterProfiles.collectAsState()
    val filterInstalled by viewModel.dnsFilterInstalled.collectAsState()

    var editor by remember { mutableStateOf<DnsServerEditor?>(null) }
    var removal by remember { mutableStateOf<DnsServerRemoval?>(null) }

    fun refresh() {
        viewModel.loadNameServers()
        viewModel.loadDohUpstream()
        viewModel.loadDotUpstream()
        viewModel.loadDnsIntercept()
        viewModel.loadDnsFilters()
    }

    LaunchedEffect(Unit) {
        viewModel.loadInterfaces()
        refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "DNS",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = KeeneticColors.TextPrimary
                        )
                        Text(
                            "Серверы, перехват запросов и фильтрация",
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
                    IconButton(onClick = { refresh() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Обновить",
                            tint = KeeneticColors.Primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KeeneticColors.Background)
            )
        },
        containerColor = KeeneticColors.Background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "intercept") {
                DnsInterceptCard(
                    enabled = intercept == true,
                    known = intercept != null,
                    onChange = { viewModel.setDnsIntercept(it) }
                )
            }

            item(key = "servers-header") {
                DnsSectionHeader(
                    icon = Icons.Default.Dns,
                    title = "DNS-серверы",
                    subtitle = "ip/name-server · dns-proxy/{https,tls}/upstream"
                )
            }

            item(key = "group-doh") {
                DnsServerGroup(
                    kind = DnsServerKind.Doh,
                    entries = dohServers.map { DnsServerEntry(it.url, it.interfaceName) },
                    onAdd = { editor = DnsServerEditor(kind = DnsServerKind.Doh) },
                    onEdit = {
                        editor = DnsServerEditor(
                            kind = DnsServerKind.Doh,
                            address = it.value,
                            interfaceName = it.interfaceName.orEmpty(),
                            isEdit = true
                        )
                    },
                    onDelete = { removal = DnsServerRemoval(DnsServerKind.Doh, it.value, it.interfaceName) }
                )
            }

            item(key = "group-dot") {
                DnsServerGroup(
                    kind = DnsServerKind.Dot,
                    entries = dotServers.map {
                        DnsServerEntry(
                            value = it.fqdn ?: it.address ?: "—",
                            interfaceName = it.interfaceName,
                            deletable = !it.address.isNullOrBlank()
                        )
                    },
                    onAdd = { editor = DnsServerEditor(kind = DnsServerKind.Dot) },
                    onEdit = { entry ->
                        val server = dotServers.first { (it.fqdn ?: it.address) == entry.value }
                        editor = DnsServerEditor(
                            kind = DnsServerKind.Dot,
                            address = server.address.orEmpty(),
                            fqdn = server.fqdn.orEmpty(),
                            interfaceName = server.interfaceName.orEmpty(),
                            isEdit = true
                        )
                    },
                    onDelete = { entry ->
                        val server = dotServers.first { (it.fqdn ?: it.address) == entry.value }
                        removal = DnsServerRemoval(
                            kind = DnsServerKind.Dot,
                            address = server.address.orEmpty(),
                            interfaceName = server.interfaceName
                        )
                    }
                )
            }

            item(key = "group-plain") {
                DnsServerGroup(
                    kind = DnsServerKind.Plain,
                    entries = nameServers.map { DnsServerEntry(it.address ?: "—", it.interfaceName) },
                    onAdd = { editor = DnsServerEditor(kind = DnsServerKind.Plain) },
                    onEdit = {
                        editor = DnsServerEditor(
                            kind = DnsServerKind.Plain,
                            address = it.value,
                            interfaceName = it.interfaceName.orEmpty(),
                            isEdit = true
                        )
                    },
                    onDelete = { removal = DnsServerRemoval(DnsServerKind.Plain, it.value, it.interfaceName) }
                )
            }

            dnsFilterItems(
                DnsFilterUiState(
                    presets = filterPresets,
                    profiles = filterProfiles,
                    presetsLoading = filterPresetsState is ApiCallState.Loading,
                    profilesLoading = filterProfilesState is ApiCallState.Loading,
                    installed = filterInstalled
                )
            )
        }
    }

    editor?.let { current ->
        DnsServerDialog(
            editor = current,
            interfaces = interfaces,
            onDismiss = { editor = null },
            onSave = { address, fqdn, iface ->
                when (current.kind) {
                    DnsServerKind.Doh ->
                        if (current.isEdit) {
                            viewModel.updateDohServer(
                                oldUrl = current.address,
                                oldInterface = current.interfaceName.ifBlank { null },
                                newUrl = address,
                                newInterface = iface.ifBlank { null }
                            )
                        } else {
                            viewModel.addDohServer(address, iface.ifBlank { null })
                        }
                    DnsServerKind.Dot ->
                        if (current.isEdit) {
                            viewModel.updateDotServer(
                                oldAddress = current.address,
                                oldFqdn = current.fqdn.ifBlank { null },
                                oldInterface = current.interfaceName.ifBlank { null },
                                newAddress = address,
                                newFqdn = fqdn.ifBlank { null },
                                newInterface = iface.ifBlank { null }
                            )
                        } else {
                            viewModel.addDotServer(address, fqdn.ifBlank { null }, iface.ifBlank { null })
                        }
                    DnsServerKind.Plain ->
                        if (current.isEdit) {
                            viewModel.updatePlainDnsServer(
                                oldAddress = current.address,
                                oldInterfaceName = current.interfaceName.ifBlank { null },
                                newAddress = address,
                                newInterfaceName = iface
                            )
                        } else {
                            viewModel.addPlainDnsServer(address, iface)
                        }
                }
                editor = null
            }
        )
    }

    removal?.let { target ->
        AlertDialog(
            onDismissRequest = { removal = null },
            title = { Text("Удалить сервер?") },
            text = { Text("${target.address} будет удалён из списка DNS-серверов роутера.") },
            confirmButton = {
                TextButton(onClick = {
                    when (target.kind) {
                        DnsServerKind.Doh -> viewModel.removeDohServer(target.address, target.interfaceName)
                        DnsServerKind.Dot -> viewModel.removeDotServer(target.address, target.interfaceName)
                        DnsServerKind.Plain -> viewModel.removePlainDnsServer(target.address, target.interfaceName.orEmpty())
                    }
                    removal = null
                }) { Text("Удалить", color = KeeneticColors.Error) }
            },
            dismissButton = {
                TextButton(onClick = { removal = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun DnsInterceptCard(enabled: Boolean, known: Boolean, onChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Перехват DNS-запросов",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = KeeneticColors.TextPrimary
                )
                Text(
                    when {
                        !known -> "Состояние неизвестно"
                        enabled -> "Запросы устройств проходят через DNS-прокси роутера"
                        else -> "Устройства используют DNS-серверы напрямую"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
            }
            Switch(checked = enabled, onCheckedChange = onChange, enabled = known)
        }
    }
}

@Composable
private fun DnsServerGroup(
    kind: DnsServerKind,
    entries: List<DnsServerEntry>,
    onAdd: () -> Unit,
    onEdit: (DnsServerEntry) -> Unit,
    onDelete: (DnsServerEntry) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    kind.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = KeeneticColors.TextPrimary
                )
                OutlinedButton(
                    onClick = onAdd,
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                    modifier = Modifier.heightIn(min = 40.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Добавить", style = MaterialTheme.typography.labelLarge)
                }
            }

            if (entries.isEmpty()) {
                Text(
                    "Не заданы",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            entries.forEachIndexed { index, entry ->
                if (index > 0) {
                    HorizontalDivider(color = KeeneticColors.Divider, modifier = Modifier.padding(vertical = 4.dp))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = KeeneticColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            entry.value,
                            style = MaterialTheme.typography.bodyMedium,
                            color = KeeneticColors.TextPrimary
                        )
                        Text(
                            entry.interfaceName?.takeIf { it.isNotBlank() }
                                ?.let { "интерфейс $it" } ?: "любой интерфейс",
                            style = MaterialTheme.typography.labelSmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                    IconButton(onClick = { onEdit(entry) }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Изменить",
                            tint = KeeneticColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (entry.deletable) {
                        IconButton(onClick = { onDelete(entry) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Удалить",
                                tint = KeeneticColors.Error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DnsServerDialog(
    editor: DnsServerEditor,
    interfaces: List<RouterInterface>,
    onDismiss: () -> Unit,
    onSave: (address: String, fqdn: String, iface: String) -> Unit
) {
    var address by remember(editor) { mutableStateOf(editor.address) }
    var fqdn by remember(editor) { mutableStateOf(editor.fqdn) }
    var iface by remember(editor) { mutableStateOf(editor.interfaceName) }
    var menuExpanded by remember { mutableStateOf(false) }
    val options = interfaces.filter { it.isUp }.sortedBy { it.description.lowercase() }
    val currentLabel = options.firstOrNull { it.id == iface }?.description ?: "Любой интерфейс"

    val valid = when (editor.kind) {
        DnsServerKind.Doh -> address.startsWith("https://")
        DnsServerKind.Dot -> address.isNotBlank() || fqdn.isNotBlank()
        DnsServerKind.Plain -> address.isNotBlank()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editor.isEdit) "Изменить сервер" else editor.kind.addLabel) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = {
                        Text(
                            when (editor.kind) {
                                DnsServerKind.Doh -> "Адрес DoH-сервера"
                                DnsServerKind.Dot -> "IP-адрес сервера"
                                DnsServerKind.Plain -> "IP-адрес сервера"
                            }
                        )
                    },
                    placeholder = {
                        Text(
                            when (editor.kind) {
                                DnsServerKind.Doh -> "https://common.dot.dns.yandex.net/dns-query"
                                else -> "77.88.8.8"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (editor.kind == DnsServerKind.Dot) {
                    OutlinedTextField(
                        value = fqdn,
                        onValueChange = { fqdn = it },
                        label = { Text("Доменное имя (SNI)") },
                        placeholder = { Text("common.dot.dns.yandex.net") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                ExposedDropdownMenuBox(expanded = menuExpanded, onExpandedChange = { menuExpanded = it }) {
                    OutlinedTextField(
                        value = currentLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Интерфейс") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Любой интерфейс", color = KeeneticColors.TextPrimary) },
                            onClick = { iface = ""; menuExpanded = false }
                        )
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text("${option.description} (${option.id})", color = KeeneticColors.TextPrimary)
                                },
                                onClick = { iface = option.id; menuExpanded = false }
                            )
                        }
                    }
                }
                Text(
                    "Пустой интерфейс — серверы используются для всех подключений",
                    style = MaterialTheme.typography.labelSmall,
                    color = KeeneticColors.TextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(address.trim(), fqdn.trim(), iface.trim()) },
                enabled = valid
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
