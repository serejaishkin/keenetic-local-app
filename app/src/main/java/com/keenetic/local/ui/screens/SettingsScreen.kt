package com.keenetic.local.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun SettingsScreen(
    viewModel: RouterViewModel,
    onLoggedOut: () -> Unit,
    onOpenApps: () -> Unit = {},
    onOpenSystemSettings: () -> Unit = {}
) {
    val savedIp by viewModel.routerIp.collectAsState()
    val savedLogin by viewModel.routerLogin.collectAsState()
    val savedAutoLogin by viewModel.autoLoginEnabled.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var ip by remember(savedIp) { mutableStateOf(savedIp) }
    var login by remember(savedLogin) { mutableStateOf(savedLogin) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var autoLogin by remember(savedAutoLogin) { mutableStateOf(savedAutoLogin) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Настройки приложения",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Подключение к роутеру, авто-вход и быстрые переходы по настройкам",
            style = MaterialTheme.typography.bodySmall,
            color = KeeneticColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth().clickable { onOpenApps() },
            colors = CardDefaults.cardColors(containerColor = KeeneticColors.Primary.copy(alpha = 0.08f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Apps, contentDescription = null, tint = KeeneticColors.Primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Приложения", fontWeight = FontWeight.Medium, color = KeeneticColors.Primary)
                        Text(
                            "IntelliQoS, opkg, торрент - отдельный раздел",
                            style = MaterialTheme.typography.labelSmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                }
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = KeeneticColors.Primary)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth().clickable { onOpenSystemSettings() },
            colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.SettingsSystemDaydream, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Настройки системы", fontWeight = FontWeight.Medium, color = KeeneticColors.TextPrimary)
                    Text(
                        "VPN, DNS-over-HTTPS, расписания и системные сервисы",
                        style = MaterialTheme.typography.labelSmall,
                        color = KeeneticColors.TextSecondary
                    )
                }
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = KeeneticColors.TextSecondary)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Подключение к роутеру", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP адрес роутера") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = login,
                    onValueChange = { login = it },
                    label = { Text("Логин администратора") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль администратора") },
                    placeholder = { Text("Оставьте пустым, чтобы не менять") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Автовход", fontWeight = FontWeight.Medium)
                        Text(
                            "Пароль хранится зашифрованным (AndroidKeyStore)",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                    Switch(
                        checked = autoLogin,
                        onCheckedChange = { autoLogin = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = KeeneticColors.Accent)
                    )
                }

                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error ?: "", color = KeeneticColors.Error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.saveConnectionSettings(ip, login, password.ifBlank { null }, autoLogin)
                        password = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Сохранить")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                viewModel.logout()
                onLoggedOut()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = KeeneticColors.Error)
        ) {
            Text("Выйти")
        }
    }
}

@Composable
fun SystemSettingsScreen(viewModel: RouterViewModel, onOpenAppSettings: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Настройки системы",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "VPN, DNS-over-HTTPS, расписания и системные сервисы",
            style = MaterialTheme.typography.bodySmall,
            color = KeeneticColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { onOpenAppSettings() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("К настройкам приложения")
        }
        Spacer(modifier = Modifier.height(16.dp))

        VpnServerStatusCard(viewModel)

        Spacer(modifier = Modifier.height(16.dp))
        DohDnsCard(viewModel)

        Spacer(modifier = Modifier.height(16.dp))
        ScheduleCard(viewModel)

        Spacer(modifier = Modifier.height(16.dp))
        ExtraServicesCard(viewModel)
    }
}

@Composable
fun VpnServerStatusCard(viewModel: RouterViewModel) {
    val vpnServer by viewModel.vpnServer.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadVpnServerConfig() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("VPN-сервер (L2TP/IKEv2)", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            if (vpnServer == null) {
                Text("Загрузка...", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
            } else {
                val cfg = vpnServer!!
                Text(
                    if (cfg.enabled) "Включён" else "Выключен",
                    color = if (cfg.enabled) KeeneticColors.Accent else KeeneticColors.TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Сегмент: ${cfg.interfaceName ?: "—"} · Пул: ${cfg.poolStart ?: "—"} (+${cfg.poolSize ?: "?"})",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
                Text(
                    "Команда включения/выключения пока не подтверждена - только просмотр",
                    style = MaterialTheme.typography.labelSmall,
                    color = KeeneticColors.TextSecondary
                )
            }
        }
    }
}

@Composable
fun DohDnsCard(viewModel: RouterViewModel) {
    var expanded by remember { mutableStateOf(false) }
    var dohUrl by remember { mutableStateOf("") }
    var targetInterface by remember { mutableStateOf("") }
    val nameServers by viewModel.nameServers.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadNameServers() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("DNS-over-HTTPS (DoH)", fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Скрыть" else "Настроить")
                }
            }

            if (nameServers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Сейчас настроено:", style = MaterialTheme.typography.labelSmall, color = KeeneticColors.TextSecondary)
                nameServers.forEach {
                    Text(
                        "${it.address ?: "?"} (${it.interfaceName ?: "?"})",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Сейчас DNS-серверы не заданы явно (используются от провайдера)",
                    style = MaterialTheme.typography.labelSmall,
                    color = KeeneticColors.TextSecondary
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Задаёт один сервер DoH, заменяя текущий список. Если у тебя настроено несколько DoH-серверов сразу - эта настройка их перезапишет.",
                    style = MaterialTheme.typography.labelSmall,
                    color = KeeneticColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = dohUrl,
                    onValueChange = { dohUrl = it },
                    label = { Text("DoH URL") },
                    placeholder = { Text("https://common.dot.dns.yandex.net/dns-query") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = targetInterface,
                    onValueChange = { targetInterface = it },
                    label = { Text("Интерфейс (необязательно)") },
                    placeholder = { Text("например GigabitEthernet0/Vlan4") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.setCustomDoh(dohUrl, targetInterface.takeIf { it.isNotBlank() }) },
                    enabled = dohUrl.startsWith("https://"),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Применить")
                }
            }
        }
    }
}

@Composable
fun ExtraServicesCard(viewModel: RouterViewModel) {
    // Реальное текущее состояние этих переключателей роутер не отдаёт нам
    // сейчас отдельным запросом (мы не парсим show/sc/ntce и show/sc/opkg),
    // поэтому тумблеры "локальные" - отражают то, что ты сам переключил в
    // этой сессии, а не факт с роутера. Понадёт - подключим show-запрос,
    // чтобы подтягивать реальное состояние при открытии экрана.
    var qosEnabled by remember { mutableStateOf(false) }
    var opkgEnabled by remember { mutableStateOf(false) }
    var torrentEnabled by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Дополнительные сервисы", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Состояние переключателей не подгружается с роутера - отражает только твои действия в этой сессии",
                style = MaterialTheme.typography.labelSmall,
                color = KeeneticColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("IntelliQoS", fontWeight = FontWeight.Medium)
                    Text(
                        "Приоритезация трафика (звонки/игры/стриминг)",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                }
                Switch(
                    checked = qosEnabled,
                    onCheckedChange = { qosEnabled = it; viewModel.setIntelliQos(it) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Менеджер пакетов (opkg)", fontWeight = FontWeight.Medium)
                    Text(
                        "Установка дополнительного ПО на роутер",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                }
                Switch(
                    checked = opkgEnabled,
                    onCheckedChange = { opkgEnabled = it; viewModel.setOpkgManager(it) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Торрент-клиент", fontWeight = FontWeight.Medium)
                    Text(
                        "Встроенный торрент-клиент роутера",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                }
                Switch(
                    checked = torrentEnabled,
                    onCheckedChange = { torrentEnabled = it; viewModel.setTorrentClient(it) }
                )
            }
        }
    }
}

@Composable
fun ScheduleCard(viewModel: RouterViewModel) {
    val dayNames = listOf("Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб")
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val selectedDays = remember { mutableStateListOf<Int>() }
    var startHour by remember { mutableStateOf("22") }
    var startMin by remember { mutableStateOf("0") }
    var stopHour by remember { mutableStateOf("7") }
    var stopMin by remember { mutableStateOf("0") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Расписание доступа", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Создаёт именованное расписание. Привязка к конкретному устройству - в разделе Устройства (структурная аналогия с назначением политики, отдельно не проверена реальным действием на роутере)",
                style = MaterialTheme.typography.labelSmall,
                color = KeeneticColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Имя расписания") },
                placeholder = { Text("например schedule0") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text("Дни недели", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                dayNames.forEachIndexed { index, label ->
                    FilterChip(
                        selected = index in selectedDays,
                        onClick = {
                            if (index in selectedDays) selectedDays.remove(index) else selectedDays.add(index)
                        },
                        label = { Text(label) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startHour, onValueChange = { startHour = it },
                    label = { Text("Начало, час") }, singleLine = true, modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = startMin, onValueChange = { startMin = it },
                    label = { Text("мин") }, singleLine = true, modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = stopHour, onValueChange = { stopHour = it },
                    label = { Text("Окончание, час") }, singleLine = true, modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = stopMin, onValueChange = { stopMin = it },
                    label = { Text("мин") }, singleLine = true, modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    viewModel.createSchedule(
                        name = name,
                        description = description,
                        daysOfWeek = selectedDays.toList(),
                        startHour = startHour.toIntOrNull() ?: 0,
                        startMin = startMin.toIntOrNull() ?: 0,
                        stopHour = stopHour.toIntOrNull() ?: 0,
                        stopMin = stopMin.toIntOrNull() ?: 0
                    )
                },
                enabled = name.isNotBlank() && selectedDays.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Создать расписание")
            }
        }
    }
}
