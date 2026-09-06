package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.*
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun VpnServersScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val wireguardServer by viewModel.wireguardServer.collectAsState()
    val l2tpServer by viewModel.l2tpServer.collectAsState()
    val ikev2Server by viewModel.ikev2Server.collectAsState()
    val sstpServer by viewModel.sstpServer.collectAsState()
    val ipsecStatus by viewModel.ipsecStatus.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadWireguardServer()
        viewModel.loadL2tpServer()
        viewModel.loadIkev2Server()
        viewModel.loadSstpServer()
        viewModel.loadIpsecStatus()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                Icon(Icons.Default.VpnLock, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "VPN Серверы",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = KeeneticColors.TextPrimary
                )
            }
        }

        // WireGuard
        item {
            WireGuardCard(wireguardServer)
        }

        // Peers
        if (wireguardServer.peers.isNotEmpty()) {
            item {
                Text("WireGuard пиры", style = MaterialTheme.typography.titleSmall, color = KeeneticColors.TextPrimary, modifier = Modifier.padding(horizontal = 4.dp))
            }
            items(wireguardServer.peers) { peer ->
                WireGuardPeerCard(peer)
            }
        }

        // L2TP
        item { L2tpCard(l2tpServer) }

        // IKEv2
        item { Ikev2Card(ikev2Server) }

        // SSTP
        item { SstpCard(sstpServer) }

        // IPsec
        item { IpsecCard(ipsecStatus) }

        if (ipsecStatus.connections.isNotEmpty()) {
            items(ipsecStatus.connections) { conn ->
                IpsecConnectionCard(conn)
            }
        }
    }
}

@Composable
private fun WireGuardCard(server: WireguardServerStatus) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.VpnLock, contentDescription = null, tint = KeeneticColors.Primary)
                Text("WireGuard", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Text(if (server.enabled) "ВКЛ" else "ВЫКЛ", color = if (server.enabled) KeeneticColors.Primary else KeeneticColors.TextSecondary, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = KeeneticColors.Divider)
            InfoRow("Порт", "${server.listenPort}")
            InfoRow("Публичный ключ", server.publicKey)
            InfoRow("Адрес", server.address)
            InfoRow("Пиров", "${server.peers.size}")
        }
    }
}

@Composable
private fun WireGuardPeerCard(peer: WireguardPeerFull) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(peer.name.ifEmpty { "Peer" }, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
            Text("Публичный ключ: ${peer.publicKey}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
            Text("Конечная точка: ${peer.endpoint}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
            Text("Разрешённый IP: ${peer.allowedIp}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
            Text("RX: ${peer.rxBytes} | TX: ${peer.txBytes}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
        }
    }
}

@Composable
private fun L2tpCard(server: L2tpServer) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = KeeneticColors.Primary)
                Text("L2TP/IPsec", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Text(if (server.enabled) "ВКЛ" else "ВЫКЛ", color = if (server.enabled) KeeneticColors.Primary else KeeneticColors.TextSecondary, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = KeeneticColors.Divider)
            InfoRow("Интерфейс", server.interfaceName)
            InfoRow("Пул (начало)", server.poolStart)
            InfoRow("Размер пула", server.poolSize)
            InfoRow("NAT", if (server.nat) "Да" else "Нет")
            InfoRow("Шифрование", if (server.encryption) "Да" else "Нет")
        }
    }
}

@Composable
private fun Ikev2Card(server: Ikev2Server) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = KeeneticColors.Primary)
                Text("IKEv2", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Text(if (server.enabled) "ВКЛ" else "ВЫКЛ", color = if (server.enabled) KeeneticColors.Primary else KeeneticColors.TextSecondary, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = KeeneticColors.Divider)
            InfoRow("Интерфейс", server.interfaceName)
            InfoRow("Пул (начало)", server.poolStart)
            InfoRow("Размер пула", server.poolSize)
        }
    }
}

@Composable
private fun SstpCard(server: SstpServerFull) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.VpnLock, contentDescription = null, tint = KeeneticColors.Primary)
                Text("SSTP", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Text(if (server.enabled) "ВКЛ" else "ВЫКЛ", color = if (server.enabled) KeeneticColors.Primary else KeeneticColors.TextSecondary, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = KeeneticColors.Divider)
            InfoRow("Интерфейс", server.interfaceName)
            InfoRow("Пул (начало)", server.poolStart)
            InfoRow("Размер пула", server.poolSize)
            InfoRow("Камуфляж", if (server.camouflage) "Да" else "Нет")
        }
    }
}

@Composable
private fun IpsecCard(status: IpsecStatus) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Security, contentDescription = null, tint = KeeneticColors.Primary)
                Text("IPsec", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Text(if (status.enabled) "ВКЛ" else "ВЫКЛ", color = if (status.enabled) KeeneticColors.Primary else KeeneticColors.TextSecondary, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = KeeneticColors.Divider)
            InfoRow("Подключения", "${status.connections.size}")
        }
    }
}

@Composable
private fun IpsecConnectionCard(conn: IpsecConnection) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(conn.name, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
            Text("Статус: ${conn.status}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
            Text("Локальный: ${conn.localAddress}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
            Text("Удалённый: ${conn.remoteAddress}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
        }
    }
}

private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Medium)
    }
}
