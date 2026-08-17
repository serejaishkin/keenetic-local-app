package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Usb
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
 * Раздел "Накопители и устройства" (USB). Чтение (show/usb) - подтверждено
 * строкой в JS-бандле веб-морды и в списке путей APK/прошивки. Извлечение
 * (system/eject) - путь подтверждён, точный формат тела запроса не
 * проверен HAR, отправляется как предположение "name" - проверить на
 * некритичном накопителе.
 */
@Composable
fun UsbDevicesScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val devices by viewModel.usbDevicesRaw.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadUsbDevices() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Накопители и устройства", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Text(
            "USB-накопители, подключённые к роутеру",
            style = MaterialTheme.typography.bodySmall,
            color = KeeneticColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Usb, contentDescription = null, tint = KeeneticColors.TextSecondary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Если накопитель не отображается - вероятно, к роутеру ничего не подключено",
                style = MaterialTheme.typography.labelSmall,
                color = KeeneticColors.TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        RawJsonCard(title = "Устройства", state = devices, emptyText = "USB-накопители не обнаружены")
    }
}
