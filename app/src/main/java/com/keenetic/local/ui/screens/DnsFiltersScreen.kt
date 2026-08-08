package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
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
 * Раздел "DNS-фильтры" (родительский контроль / блокировка категорий сайтов).
 *
 * RCI-пути подтверждены строками в main-553997B.js:
 * show.dns-proxy.filter.presets, show.dns-proxy.filter.profiles - это
 * ЧТЕНИЕ, подключено. Назначение профиля конкретному клиенту/сети
 * (dns-proxy.filter.assign) - НЕ подключено, формат set-команды не
 * подтверждён HAR.
 */
@Composable
fun DnsFiltersScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val presets by viewModel.dnsFilterPresets.collectAsState()
    val profiles by viewModel.dnsFilterProfiles.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadDnsFilters() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("DNS-фильтры", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Text(
            "Готовые пресеты и профили фильтрации доменов",
            style = MaterialTheme.typography.bodySmall,
            color = KeeneticColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                DnsFilterEngineCard(viewModel)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = KeeneticColors.TextSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Назначение профиля устройству/сети пока недоступно - нужен HAR момента реального переключения на веб-морде.",
                            style = MaterialTheme.typography.labelSmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                }
            }

            item {
                RawJsonCard(title = "Пресеты фильтрации", state = presets, emptyText = "Пусто или не поддерживается на этой прошивке")
            }
            item {
                RawJsonCard(title = "Профили", state = profiles, emptyText = "Пусто или не поддерживается на этой прошивке")
            }
        }
    }
}

/**
 * ПОДТВЕРЖДЕНО HAR (07.08, найдено при повторном полном проходе по всем
 * 8 файлам архива - в первый раз проверял только route.har, поэтому
 * пропустил). Реальная команда: {"dns-proxy":{"filter":{"engine":{"no":true}}}}
 * + save, ответ роутера успешный.
 *
 * Кнопки, а не Switch - потому что НЕТ подтверждённого способа прочитать
 * текущее состояние (в архиве не встретился show-запрос именно этого поля),
 * поэтому нечестно рисовать переключатель, как будто мы знаем состояние.
 */
@Composable
private fun DnsFilterEngineCard(viewModel: RouterViewModel) {
    var lastAction by remember { mutableStateOf<Boolean?>(null) }
    val error by viewModel.error.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Движок DNS-фильтрации", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Текущее состояние неизвестно приложению - нет подтверждённого способа его прочитать, только переключить.",
                style = MaterialTheme.typography.labelSmall,
                color = KeeneticColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { lastAction = true; viewModel.setDnsFilterEngine(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Accent)
                ) {
                    Text("Включить")
                }
                OutlinedButton(onClick = { lastAction = false; viewModel.setDnsFilterEngine(false) }) {
                    Text("Выключить")
                }
            }
            if (lastAction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (error != null) "Ошибка: $error" else if (lastAction == true) "Отправлено: включить" else "Отправлено: выключить",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (error != null) KeeneticColors.Error else KeeneticColors.Accent
                )
            }
        }
    }
}
