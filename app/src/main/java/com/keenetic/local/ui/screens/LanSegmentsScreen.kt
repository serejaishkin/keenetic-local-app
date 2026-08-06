package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.InterfaceInfo
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

/**
 * Раздел "LAN-сегменты". СОЗНАТЕЛЬНО только чтение: в Keenetic сегмент сети -
 * это Bridge-интерфейс (Home, Guest и т.д.), а не отдельная сущность RCI, и
 * данные по ним уже есть в подтверждённом /rci/show/interface (тот же вызов,
 * что использует Dashboard/Wi-Fi экраны) - новых запросов к роутеру не
 * потребовалось.
 *
 * Создание НОВОГО сегмента - явно отложено (см. ROADMAP.md, "Задача 4" и
 * связанное решение про "высокий риск создания интерфейсов"): по опыту
 * happ-keenetic такие операции не делаются простым set-запросом и легко
 * приводят к полурабочему состоянию сети. Нужен отдельный HAR именно
 * момента создания нового сегмента на веб-морде, прежде чем это писать.
 */
@Composable
fun LanSegmentsScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val interfaces by viewModel.interfaces.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadInterfaces() }

    val segments = interfaces.filter { it.type.equals("Bridge", ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("LAN-сегменты", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Text(
            "Существующие сетевые сегменты (домашняя сеть, гостевая и др.)",
            style = MaterialTheme.typography.bodySmall,
            color = KeeneticColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = KeeneticColors.TextSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Создание нового сегмента здесь не реализовано намеренно - высокий риск сломать сеть без HAR-подтверждения формата команды.",
                    style = MaterialTheme.typography.labelSmall,
                    color = KeeneticColors.TextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (segments.isEmpty()) {
            Text(
                "Сегменты не найдены (или ещё загружаются)",
                style = MaterialTheme.typography.bodySmall,
                color = KeeneticColors.TextSecondary
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(segments) { seg -> SegmentCard(seg) }
            }
        }
    }
}

@Composable
private fun SegmentCard(seg: InterfaceInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Circle,
                    contentDescription = null,
                    tint = if (seg.up) KeeneticColors.Accent else KeeneticColors.TextSecondary,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(seg.displayName, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "ID: ${seg.id}" + (seg.address?.let { "  ·  IP: $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = KeeneticColors.TextSecondary
            )
            Text(
                if (seg.up) "Активен" else "Отключён",
                style = MaterialTheme.typography.labelSmall,
                color = if (seg.up) KeeneticColors.Accent else KeeneticColors.Error
            )
        }
    }
}
