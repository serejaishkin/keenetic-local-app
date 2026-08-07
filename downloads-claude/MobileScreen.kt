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
 * Раздел "Mobile" - статус LTE-модема и SIM-карты. Актуально конкретно для
 * Hero 4G+ (в отличие от большинства других новых разделов). RCI-пути
 * подтверждены строками в main-553997B.js: show/mobile, show/sim.
 * SMS/USSD НЕ подключены - в бандле нет строки "show.sms"/"show.ussd",
 * только UI-подписи; похоже, что это не простой GET, нужен HAR.
 */
@Composable
fun MobileScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val mobile by viewModel.mobileRaw.collectAsState()
    val sim by viewModel.simRaw.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadMobileStatus() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Мобильный интернет", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Text(
            "Статус LTE-модема и SIM-карты",
            style = MaterialTheme.typography.bodySmall,
            color = KeeneticColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        RawJsonCard(title = "Модем", state = mobile, emptyText = "Модем не обнаружен или не активен")
        Spacer(modifier = Modifier.height(12.dp))
        RawJsonCard(title = "SIM-карта", state = sim, emptyText = "SIM не обнаружена")
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "SMS и USSD в этом разделе пока нет - точный RCI-путь для них не подтверждён (в отличие от статуса модема/SIM выше), нужен HAR открытия соответствующей вкладки на веб-морде.",
            style = MaterialTheme.typography.labelSmall,
            color = KeeneticColors.TextSecondary
        )
    }
}
