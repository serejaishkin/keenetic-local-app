package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
 * Раздел "Маршрутизация" (/network/static-routes на сайте). RCI-путь
 * show/ip/route подтверждён строкой в main-553997B.js. Только чтение -
 * добавление/изменение маршрутов не реализовано, формат set-команды
 * ("ip route ...") не подтверждён HAR.
 */
@Composable
fun StaticRoutesScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val routes by viewModel.ipRouteRaw.collectAsState()

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
        RawJsonCard(title = "Маршруты", data = routes, emptyText = "Статических маршрутов не найдено")
    }
}
