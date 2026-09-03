package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors
import com.keenetic.local.ui.screens.common.RawJsonCard

@Composable
fun VpnAdvancedScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val raw by viewModel.vpnServerRaw.collectAsState()

    LaunchedEffect(Unit) { 
        viewModel.loadVpnServerStatus() 
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.VpnKey, contentDescription = null, tint = KeeneticColors.Primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("VPN-сервер и клиенты", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Text(
            "Статус встроенных VPN-серверов (WireGuard, SSTP, OpenVPN, L2TP/IPsec)",
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
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = KeeneticColors.Primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Отображает текущее состояние и статус активных подключений VPN-сервера роутера.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextPrimary
                        )
                    }
                }
            }

            item {
                RawJsonCard(
                    title = "Статус VPN-сервера (show vpn-server)",
                    state = raw,
                    emptyText = "VPN-сервер выключен или не настроен на роутере"
                )
            }
        }
    }
}
