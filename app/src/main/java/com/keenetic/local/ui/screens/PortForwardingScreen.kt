package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors
import com.keenetic.local.ui.screens.common.RawJsonCard

/**
 * Раздел "Переадресация портов" (/port-forwarding на сайте).
 *
 * СТАТУС: чтение (ip.static) реализовано как сырой JSON - схема ответа
 * (массив или объект-словарь по id, см. предупреждение в API-REFERENCE.md
 * про непостоянство схем /rci/show/...) ещё не подтверждена HAR, поэтому
 * строгий парсер пока не пишем.
 *
 * Форма ниже использует подтверждённые подписи полей из
 * assets/language/locale.ru.json (namespace "portforwarding" ->
 * "editor.fields"), но САМА set-команда создания правила НЕ подтверждена
 * HAR - кнопка "Создать правило" сознательно отключена, чтобы не отправить
 * на роутер угаданный формат. Чтобы включить: сними HAR момента реального
 * добавления правила на веб-морде (Управление -> Переадресация портов ->
 * Добавить правило -> Сохранить) и пришли его.
 */
@Composable
fun PortForwardingScreen(viewModel: RouterViewModel) {
    val rawRules by viewModel.portForwardingRaw.collectAsState()
    val upnpRules by viewModel.upnpRedirectRaw.collectAsState()

    var interfaceName by remember { mutableStateOf("") }
    var portMode by remember { mutableStateOf("single") } // single | range
    var port by remember { mutableStateOf("") }
    var protocol by remember { mutableStateOf("tcp") }
    var toIp by remember { mutableStateOf("") }
    var toPort by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadPortForwardingRules() }
    LaunchedEffect(Unit) { viewModel.loadUpnpRedirect() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Переадресация портов",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Публичные правила проброса портов из интернета в локальную сеть",
            style = MaterialTheme.typography.bodySmall,
            color = KeeneticColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                RawJsonCard(title = "Текущие правила", state = rawRules, emptyText = "Ни одного правила ещё не создано")
            }

            item {
                RawJsonCard(
                    title = "Открытые порты по UPnP",
                    state = upnpRules,
                    emptyText = "Нет открытых портов по UPnP"
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = KeeneticColors.TextSecondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Создание правила пока недоступно - формат подтверждающей команды ещё не снят с реального роутера (нужен HAR). Форма готова заранее.",
                                style = MaterialTheme.typography.labelSmall,
                                color = KeeneticColors.TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = interfaceName,
                            onValueChange = { interfaceName = it },
                            label = { Text("Вход (интерфейс)") },
                            placeholder = { Text("например GigabitEthernet0/Vlan4") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Протокол", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = protocol == "tcp", onClick = { protocol = "tcp" }, label = { Text("TCP") })
                            FilterChip(selected = protocol == "udp", onClick = { protocol = "udp" }, label = { Text("UDP") })
                            FilterChip(selected = protocol == "both", onClick = { protocol = "both" }, label = { Text("TCP/UDP") })
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Тип правила", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = portMode == "single", onClick = { portMode = "single" }, label = { Text("Одиночный порт") })
                            FilterChip(selected = portMode == "range", onClick = { portMode = "range" }, label = { Text("Диапазон портов") })
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text(if (portMode == "range") "Открыть порты (напр. 8000-8010)" else "Открыть порт") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Выход", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = toIp, onValueChange = { toIp = it },
                                label = { Text("IP-адрес") }, singleLine = true, modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = toPort, onValueChange = { toPort = it },
                                label = { Text("Направлять на порт") }, singleLine = true, modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = comment,
                            onValueChange = { comment = it },
                            label = { Text("Описание") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { /* намеренно не подключено - см. комментарий выше файла */ },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Создать правило (ждём HAR)")
                        }
                    }
                }
            }
        }
    }
}
