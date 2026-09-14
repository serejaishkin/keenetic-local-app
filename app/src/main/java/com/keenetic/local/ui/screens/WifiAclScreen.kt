package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

private val ACL_MODES = listOf(
    "none" to "Отключен (все разрешены)",
    "permit" to "Белый список (только из списка)",
    "deny" to "Черный список (все кроме списка)"
)

@Composable
fun WifiAclScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val interfaces by viewModel.interfaces.collectAsState()
    val unsupported by viewModel.unsupportedFeatures.collectAsState()
    val segments = remember(interfaces) { interfaces.filter { it.type == "Bridge" } }

    LaunchedEffect(Unit) {
        viewModel.checkFeatureSupport("wifi_acl", "sc/interface/mac.access-list")
        viewModel.loadInterfaces()
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Security, contentDescription = null, tint = KeeneticColors.Primary)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    "Контроль доступа Wi-Fi",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = KeeneticColors.TextPrimary
                )
                Text(
                    "Черные и белые списки для сегментов",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { viewModel.loadInterfaces() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = KeeneticColors.Primary)
            }
        }

        if (unsupported.contains("wifi_acl")) {
            UnsupportedNotice("Контроль доступа Wi-Fi")
        }

        if (segments.isEmpty()) {
            Text("Сегменты сети не найдены", color = KeeneticColors.TextSecondary)
        }

        segments.forEach { seg ->
            AclSegmentCard(seg.id, seg.name, seg.macAccessMode) { mode ->
                viewModel.setWifiAclMode(seg.id, mode)
            }
        }
        
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface.copy(alpha = 0.5f))
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Info, contentDescription = null, tint = KeeneticColors.TextSecondary)
                Text(
                    "Для добавления устройств в списки используйте веб-интерфейс. В данном приложении доступно только переключение режимов.",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun AclSegmentCard(id: String, name: String, currentMode: String, onModeChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
            
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val modeText = ACL_MODES.find { it.first == currentMode }?.second ?: currentMode
                    Text(modeText, color = KeeneticColors.TextPrimary)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = KeeneticColors.TextSecondary)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    ACL_MODES.forEach { (mode, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                expanded = false
                                onModeChange(mode)
                            }
                        )
                    }
                }
            }
        }
    }
}