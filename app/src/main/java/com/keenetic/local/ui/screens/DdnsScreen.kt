package com.keenetic.local.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.ConfirmDialog
import com.keenetic.local.ui.components.DialogForm
import com.keenetic.local.ui.components.EditableRow
import com.keenetic.local.ui.components.EmptyHint
import com.keenetic.local.ui.components.FormDialog
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.OptionPickerDialog
import com.keenetic.local.ui.components.RowDivider
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SubGroupHeader
import com.keenetic.local.ui.components.SwitchRow
import com.keenetic.local.ui.theme.KeeneticColors

private val DDNS_PROVIDERS = listOf(
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

private fun ddnsProviderLabel(key: String): String =
    DDNS_PROVIDERS.firstOrNull { it.first == key }?.second ?: key.ifBlank { "—" }

@Composable
fun DdnsScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val dyndnsStatus by viewModel.dyndnsStatus.collectAsState()
    val updaters by viewModel.dyndnsUpdaters.collectAsState()
    val profiles by viewModel.dyndnsProfiles.collectAsState()
    val interfaces by viewModel.interfaces.collectAsState()

    var showEditor by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    // Настройки формы подхватываем из профиля _WEBADMIN, если он уже настроен.
    val webProfile = profiles.firstOrNull { it.name == "_WEBADMIN" } ?: profiles.firstOrNull()

    var provider by remember(webProfile) {
        mutableStateOf(webProfile?.provider?.takeIf { it.isNotBlank() } ?: "noip")
    }
    var url by remember { mutableStateOf("") }
    var domain by remember(webProfile) {
        mutableStateOf(webProfile?.hostname?.ifBlank { null } ?: dyndnsStatus.hostname)
    }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var autoDetectIp by remember { mutableStateOf(true) }
    var selectedInterfaces by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) {
        viewModel.loadDyndnsStatus()
        if (interfaces.isEmpty()) viewModel.loadInterfaces()
    }

    SectionScaffold(
        title = "Динамический DNS",
        subtitle = ddnsProviderLabel(provider),
        onBack = onBack,
        onRefresh = { viewModel.loadDyndnsStatus() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "status") {
                SectionCard(title = "Статус", icon = Icons.Default.Public) {
                    SwitchRow(
                        label = "Отправлять адрес автоматически",
                        description = "Профиль ${dyndnsStatus.profileName.ifBlank { "_WEBADMIN" }}",
                        checked = dyndnsStatus.sendAddress,
                        onCheckedChange = { viewModel.setDyndnsSendAddress(it) }
                    )
                    RowDivider()
                    InfoRow("Доменное имя", dyndnsStatus.hostname.ifBlank { "—" }, monospace = true)
                    InfoRow("Зарегистрирован", dyndnsStatus.regtime.ifBlank { "—" })
                    if (dyndnsStatus.status.isNotBlank()) {
                        InfoRow(
                            label = "Состояние IPv4",
                            value = dyndnsStatus.status,
                            valueColor = KeeneticColors.Success
                        )
                    }
                    if (dyndnsStatus.status6.isNotBlank()) {
                        InfoRow(
                            label = "Состояние IPv6",
                            value = dyndnsStatus.status6,
                            valueColor = KeeneticColors.Success
                        )
                    }
                    if (dyndnsStatus.message.isNotBlank()) {
                        InfoRow("Сообщение IPv4", dyndnsStatus.message, valueColor = KeeneticColors.Warning)
                    }
                    if (dyndnsStatus.message6.isNotBlank()) {
                        InfoRow("Сообщение IPv6", dyndnsStatus.message6, valueColor = KeeneticColors.Warning)
                    }
                }
            }

            item(key = "settings-header") {
                SubGroupHeader("Настройки сервиса")
            }

            item(key = "settings") {
                SectionCard {
                    EditableRow(
                        label = "Провайдер",
                        value = ddnsProviderLabel(provider),
                        onClick = { showEditor = true }
                    )
                    EditableRow(
                        label = "Доменное имя",
                        value = domain.ifBlank { "не задано" },
                        onClick = { showEditor = true },
                        monospaceValue = true
                    )
                    EditableRow(
                        label = "Имя пользователя",
                        value = username.ifBlank { "не задано" },
                        onClick = { showEditor = true }
                    )
                    EditableRow(
                        label = "Пароль",
                        value = if (password.isBlank()) "не задан" else "••••••",
                        onClick = { showEditor = true }
                    )
                    EditableRow(
                        label = "Адрес сервиса (URL)",
                        value = url.ifBlank { "не задан" },
                        onClick = { showEditor = true },
                        monospaceValue = true
                    )
                    EditableRow(
                        label = "Определять IP автоматически",
                        value = if (autoDetectIp) "включено" else "выключено",
                        onClick = { showEditor = true }
                    )
                    EditableRow(
                        label = "Интерфейсы для DDNS",
                        value = if (selectedInterfaces.isEmpty()) {
                            "не выбраны"
                        } else {
                            selectedInterfaces.joinToString(", ")
                        },
                        onClick = { showEditor = true }
                    )
                    RowDivider()
                    TextButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Удалить провайдера", color = KeeneticColors.Error)
                    }
                }
            }

            if (profiles.isNotEmpty()) {
                item(key = "profiles-header") { SubGroupHeader("Профили", profiles.size) }
                items(profiles, key = { it.name }) { profile ->
                    SectionCard(
                        title = profile.name.ifBlank { "Профиль" },
                        icon = Icons.Default.Public
                    ) {
                        InfoRow("Провайдер", ddnsProviderLabel(profile.provider))
                        InfoRow("Домен", profile.hostname.ifBlank { "—" }, monospace = true)
                    }
                }
            }

            if (updaters.isNotEmpty()) {
                item(key = "updaters-header") {
                    SubGroupHeader("Провайдеры, известные роутеру", updaters.size)
                }
                item(key = "updaters") {
                    SectionCard {
                        updaters.forEach { updater ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    ddnsProviderLabel(updater.type),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = KeeneticColors.TextPrimary
                                )
                                if (updater.url.isNotBlank()) {
                                    Text(
                                        updater.url,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = KeeneticColors.TextSecondary
                                    )
                                }
                                if (updater.api.isNotBlank()) {
                                    Text(
                                        "API: ${updater.api}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = KeeneticColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (profiles.isEmpty()) {
                item(key = "no-updaters") {
                    SectionCard {
                        EmptyHint(
                            "Роутер ещё не сообщил список провайдеров DDNS. Список приходит " +
                                "вместе с обновлением прошивки."
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        DdnsSettingsDialog(
            provider = provider,
            url = url,
            domain = domain,
            username = username,
            password = password,
            autoDetectIp = autoDetectIp,
            selectedInterfaces = selectedInterfaces,
            interfaces = interfaces.map { it.name },
            onProviderChange = { provider = it },
            onUrlChange = { url = it },
            onDomainChange = { domain = it },
            onUsernameChange = { username = it },
            onPasswordChange = { password = it },
            onAutoDetectIpChange = { autoDetectIp = it },
            onInterfacesChange = { selectedInterfaces = it },
            onDismiss = { showEditor = false },
            onSave = {
                viewModel.saveDyndnsProfile(
                    provider = provider,
                    url = url,
                    domain = domain,
                    username = username,
                    password = password,
                    autoDetectIp = autoDetectIp,
                    interfaces = selectedInterfaces.toList()
                )
                showEditor = false
            }
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Удалить профиль DDNS?",
            message = "Профиль с доменным именем ${domain.ifBlank { "—" }} будет удалён с роутера, " +
                "актуальный адрес перестанет обновляться.",
            onConfirm = {
                viewModel.deleteDyndnsProfile()
                confirmDelete = false
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun DdnsSettingsDialog(
    provider: String,
    url: String,
    domain: String,
    username: String,
    password: String,
    autoDetectIp: Boolean,
    selectedInterfaces: Set<String>,
    interfaces: List<String>,
    onProviderChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onDomainChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onAutoDetectIpChange: (Boolean) -> Unit,
    onInterfacesChange: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var draftProvider by remember { mutableStateOf(provider) }
    var draftUrl by remember { mutableStateOf(url) }
    var draftDomain by remember { mutableStateOf(domain) }
    var draftUsername by remember { mutableStateOf(username) }
    var draftPassword by remember { mutableStateOf(password) }
    var draftAutoDetect by remember { mutableStateOf(autoDetectIp) }
    var draftInterfaces by remember { mutableStateOf(selectedInterfaces) }
    var showPassword by remember { mutableStateOf(false) }
    var showProviders by remember { mutableStateOf(false) }
    var showInterfaces by remember { mutableStateOf(false) }

    FormDialog(
        title = "Настройки динамического DNS",
        confirmEnabled = draftDomain.isNotBlank(),
        onDismiss = onDismiss,
        onConfirm = {
            onProviderChange(draftProvider)
            onUrlChange(draftUrl)
            onDomainChange(draftDomain)
            onUsernameChange(draftUsername)
            onPasswordChange(draftPassword)
            onAutoDetectIpChange(draftAutoDetect)
            onInterfacesChange(draftInterfaces)
            onSave()
        }
    ) {
        TextButton(onClick = { showProviders = true }) {
            Text("Провайдер: ${ddnsProviderLabel(draftProvider)}", color = KeeneticColors.Primary)
        }

        OutlinedTextField(
            value = draftDomain,
            onValueChange = { draftDomain = it },
            label = { Text("Доменное имя") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = draftUsername,
            onValueChange = { draftUsername = it },
            label = { Text("Имя пользователя") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = draftPassword,
            onValueChange = { draftPassword = it },
            label = { Text("Пароль") },
            singleLine = true,
            visualTransformation = if (showPassword) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                TextButton(onClick = { showPassword = !showPassword }) {
                    Text(
                        if (showPassword) "Скрыть" else "Показать",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = draftUrl,
            onValueChange = { draftUrl = it },
            label = { Text("Адрес сервиса (URL)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        SwitchRow(
            label = "Определять IP автоматически",
            description = "Взять адрес с выбранных интерфейсов",
            checked = draftAutoDetect,
            onCheckedChange = { draftAutoDetect = it }
        )

        TextButton(onClick = { showInterfaces = true }) {
            Text(
                "Интерфейсы: " + if (draftInterfaces.isEmpty()) {
                    "не выбраны"
                } else {
                    draftInterfaces.joinToString(", ")
                },
                color = KeeneticColors.Primary
            )
        }
    }

    if (showProviders) {
        OptionPickerDialog(
            title = "Провайдер DDNS",
            options = DDNS_PROVIDERS,
            selectedKey = draftProvider,
            onSelect = { draftProvider = it },
            onDismiss = { showProviders = false }
        )
    }

    if (showInterfaces) {
        AlertDialog(
            onDismissRequest = { showInterfaces = false },
            title = {
                Text(
                    "Интерфейсы для DDNS",
                    style = MaterialTheme.typography.titleMedium,
                    color = KeeneticColors.TextPrimary
                )
            },
            text = {
                DialogForm {
                    if (interfaces.isEmpty()) {
                        EmptyHint("Список интерфейсов пуст")
                    } else {
                        interfaces.forEach { name ->
                            val checked = name in draftInterfaces
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable {
                                        draftInterfaces = if (checked) {
                                            draftInterfaces - name
                                        } else {
                                            draftInterfaces + name
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = KeeneticColors.TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (checked) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = KeeneticColors.Primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInterfaces = false }) {
                    Text("Готово", color = KeeneticColors.Primary)
                }
            }
        )
    }
}
