package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.VpnConnection
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun OtherConnectionsScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val connections by viewModel.vpnConnections.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadVpnConnections()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Link, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Другие подключения",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = KeeneticColors.TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.loadVpnConnections() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = KeeneticColors.Primary)
                }
            }
        }

        item {
            Text(
                "VPN и туннельные подключения: WireGuard, OpenVPN, L2TP, PPTP, SSTP, ZeroTier, Proxy и другие. Включение/отключение отправляется на роутер.",
                style = MaterialTheme.typography.bodySmall,
                color = KeeneticColors.TextSecondary
            )
        }

        if (connections.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Подключений нет", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary)
                        Text(
                            "Создайте подключение в веб-интерфейсе роутера (раздел «Другие подключения») или обновите список.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                }
            }
        } else {
            items(connections.size) { index ->
                OtherConnectionCard(conn = connections[index], viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun OtherConnectionCard(conn: VpnConnection, viewModel: RouterViewModel) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(typeIcon(conn.type), contentDescription = null, tint = KeeneticColors.Primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(conn.name.ifBlank { conn.id }, style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary)
                    Text(typeLabel(conn.type, conn.protocol), style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                }
                Surface(
                    color = if (conn.isUp) KeeneticColors.Success.copy(alpha = 0.15f) else KeeneticColors.Error.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if (conn.isUp) "UP" else "DOWN",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (conn.isUp) KeeneticColors.Success else KeeneticColors.Error,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (conn.ip != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("IP-адрес", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                    Text(conn.ip, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
            if (conn.upstream != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Сервер", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                    Text(conn.upstream, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextPrimary)
                }
            }

            HorizontalDivider(color = KeeneticColors.Divider)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Подключение включено", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary)
                Switch(
                    checked = conn.isUp,
                    onCheckedChange = { up ->
                        viewModel.toggleInterface(conn.id, up)
                    }
                )
            }
        }
    }
}

private fun typeIcon(type: String): ImageVector = when (type.lowercase()) {
    "proxy" -> Icons.Default.Dns
    "zerotier" -> Icons.Default.Share
    "gre", "ipip", "eoip", "ipv6to4", "6to4", "tunnel" -> Icons.Default.Link
    "ppp", "pppoe", "pptp" -> Icons.Default.Call
    else -> Icons.Default.Lock
}

private fun typeLabel(type: String, protocol: String?): String {
    val typeName = when (type.lowercase()) {
        "proxy" -> "Proxy"
        "wireguard" -> "WireGuard"
        "openvpn" -> "OpenVPN"
        "pptp" -> "PPTP"
        "ppp" -> "PPP"
        "pppoe" -> "PPPoE"
        "l2tp" -> "L2TP"
        "sstp" -> "SSTP"
        "ike" -> "IPsec IKE"
        "openconnect" -> "OpenConnect"
        "zerotier" -> "ZeroTier"
        "gre" -> "GRE"
        "ipip" -> "IPIP"
        "eoip" -> "EoIP"
        "ipv6to4", "6to4" -> "6to4-туннель"
        else -> type
    }
    return if (protocol != null) "$typeName ($protocol)" else typeName
}