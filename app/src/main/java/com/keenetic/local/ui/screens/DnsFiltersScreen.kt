package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
            }

            item {
                DnsFilterSection(title = "Пресеты фильтрации", data = presets)
            }
            item {
                DnsFilterSection(title = "Профили", data = profiles)
            }
        }
    }
}

@Composable
private fun DnsFilterSection(title: String, data: JsonElement?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            when {
                data == null -> Text(
                    "Загрузка…",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
                isEmptyDns(data) -> Text(
                    "Пусто или не поддерживается на этой прошивке",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
                else -> Text(
                    // Временно "как есть" - schema ответа ещё не оформлена в
                    // отдельный parser/DTO, ждём подтверждения реальными
                    // данными с роутера (пресеты у части прошивок пустые).
                    text = data.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
            }
        }
    }
}

private fun isEmptyDns(el: JsonElement?): Boolean {
    if (el == null) return true
    return when {
        el.isJsonNull -> true
        el.isJsonArray -> el.asJsonArray.size() == 0
        el.isJsonObject -> el.asJsonObject.entrySet().isEmpty()
        else -> false
    }
}
