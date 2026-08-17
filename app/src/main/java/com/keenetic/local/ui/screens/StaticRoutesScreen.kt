package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors
import com.keenetic.local.ui.screens.common.RawJsonCard

/**
 * Раздел "Маршрутизация" (/network/static-routes на сайте).
 *
 * Чтение (show/ip/route) - подтверждено строкой в main-553997B.js.
 * Создание - ПОДТВЕРЖДЕНО ДВАЖДЫ независимо (07.08): класс IpRouteModel в
 * декомпилированном официальном Android-приложении Keenetic (поля auto,
 * default, description, exclusive, gateway, host, interface, mask,
 * network) + строки "gateway"/"network" найдены в /lib/libndmCorePack.so
 * реальной прошивки роутера. Не проверено живым HAR-запросом.
 */
@Composable
fun StaticRoutesScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val routes by viewModel.ipRouteRaw.collectAsState()
    val error by viewModel.error.collectAsState()

    var network by remember { mutableStateOf("") }
    var mask by remember { mutableStateOf("255.255.255.0") }
    var gateway by remember { mutableStateOf("") }
    var interfaceName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(false) }
    var confirmCreate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadStaticRoutes() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Маршрутизация", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Text(
            "Таблица статических маршрутов роутера",
            style = MaterialTheme.typography.bodySmall,
            color = KeeneticColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                RawJsonCard(title = "Маршруты", state = routes, emptyText = "Статических маршрутов не найдено")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Новый маршрут", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = network, onValueChange = { network = it },
                                label = { Text("Сеть назначения") }, placeholder = { Text("10.99.99.0") },
                                singleLine = true, modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = mask, onValueChange = { mask = it },
                                label = { Text("Маска") }, singleLine = true, modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = gateway, onValueChange = { gateway = it },
                            label = { Text("Шлюз") }, placeholder = { Text("192.168.1.1") },
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = interfaceName, onValueChange = { interfaceName = it },
                            label = { Text("Интерфейс (необязательно)") },
                            placeholder = { Text("например GigabitEthernet0/Vlan4") },
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = description, onValueChange = { description = it },
                            label = { Text("Описание") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isDefault, onCheckedChange = { isDefault = it })
                            Text("Маршрут по умолчанию (default)", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (error != null) {
                            Text(error ?: "", color = KeeneticColors.Error, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Button(
                            onClick = { confirmCreate = true },
                            enabled = isDefault || (network.isNotBlank() && gateway.isNotBlank()),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Добавить маршрут")
                        }
                    }
                }
            }
        }
    }

    if (confirmCreate) {
        AlertDialog(
            onDismissRequest = { confirmCreate = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = KeeneticColors.Warning) },
            title = { Text("Добавить маршрут?") },
            text = {
                Text(
                    "Формат подтверждён дважды независимо (официальное приложение + код прошивки), но не проверен живым запросом. Ошибка в маршруте может нарушить связность сети - рекомендуется проверить на некритичной подсети."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createStaticRoute(
                        network = network.takeIf { !isDefault },
                        mask = mask.takeIf { !isDefault },
                        gateway = gateway,
                        interfaceName = interfaceName.takeIf { it.isNotBlank() },
                        description = description,
                        isDefault = isDefault
                    )
                    confirmCreate = false
                }) {
                    Text("Добавить", color = KeeneticColors.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmCreate = false }) { Text("Отмена") }
            }
        )
    }
}
