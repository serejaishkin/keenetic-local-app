package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
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

/**
 * Раздел "VPN-расширения" - статус встроенного VPN-сервера (L2TP/IKEv2).
 *
 * RCI-путь show/vpn-server подтверждён строкой в main-553997B.js. Ранее
 * (см. ROADMAP.md) команда `show vpn-server` через SSH-терминал вернула
 * пустой ответ - но это отдельный механизм (BusyBox/CLI shell), а не тот
 * же REST-путь, которым пользуется веб-морда. Пробуем REST отдельно.
 *
 * Управление пользователями/сегментами VPN-сервера НЕ реализовано -
 * set-команда не подтверждена HAR.
 */
@Composable
fun VpnAdvancedScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val raw by viewModel.vpnServerRaw.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadVpnServerStatus() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("VPN-расширения", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Text(
            "Статус встроенного VPN-сервера (L2TP/IKEv2)",
            style = MaterialTheme.typography.bodySmall,
            color = KeeneticColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Статус VPN-сервера", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                when {
                    raw == null -> Text(
                        "Загрузка…",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                    raw!!.isJsonNull || (raw!!.isJsonObject && raw!!.asJsonObject.entrySet().isEmpty()) -> Text(
                        "Пусто - VPN-сервер, вероятно, не настроен на этом роутере, либо этот REST-путь не поддерживается прошивкой",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                    else -> Text(
                        text = raw.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = KeeneticColors.TextSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Управление пользователями и сегментами VPN-сервера пока не реализовано - нужен HAR реального изменения на веб-морде.",
                    style = MaterialTheme.typography.labelSmall,
                    color = KeeneticColors.TextSecondary
                )
            }
        }
    }
}
