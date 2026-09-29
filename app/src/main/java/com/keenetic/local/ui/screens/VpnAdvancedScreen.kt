package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keenetic.local.api.VpnPeer
import com.keenetic.local.api.VpnServerStatus
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.EmptyHint
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.RowDivider
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SubGroupHeader
import com.keenetic.local.ui.screens.common.ApiCallState
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun VpnAdvancedScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val vpnStatus by viewModel.vpnServerStatus.collectAsState()
    val raw by viewModel.vpnServerRaw.collectAsState()
    var showRaw by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadVpnServerStatus()
    }

    val status = vpnStatus

    SectionScaffold(
        title = "VPN-сервер",
        subtitle = "WireGuard, SSTP, OpenVPN, L2TP/IPsec",
        onBack = onBack,
        onRefresh = { viewModel.loadVpnServerStatus() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "hint") {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = KeeneticColors.Primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "VPN-сервер позволяет подключаться к локальной сети роутера извне " +
                                "через защищённые протоколы. Настройка протоколов и ключей — " +
                                "в веб-интерфейсе KeeneticOS.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextPrimary
                        )
                    }
                }
            }

            if (status != null) {
                item(key = "status") {
                    VpnStatusCard(status)
                }

                if (status.type == "wireguard" && status.peers.isNotEmpty()) {
                    item(key = "peers-header") {
                        SubGroupHeader("WireGuard пиры", status.peers.size)
                    }
                    items(status.peers, key = { it.name.ifBlank { it.publicKey } }) { peer ->
                        VpnPeerCard(peer)
                    }
                }
            } else {
                item(key = "empty") {
                    SectionCard {
                        EmptyHint(
                            if (raw is ApiCallState.Loading) {
                                "Загрузка данных с роутера…"
                            } else {
                                "VPN-сервер выключен или не настроен"
                            }
                        )
                    }
                }
            }

            item(key = "raw") {
                SectionCard {
                    TextButton(onClick = { showRaw = !showRaw }) {
                        Text(
                            if (showRaw) "Скрыть Raw JSON" else "Показать Raw JSON (RCI)",
                            color = KeeneticColors.TextSecondary
                        )
                    }
                    if (showRaw) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "RCI: show vpn-server",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = KeeneticColors.TextSecondary
                            )
                            Text(
                                raw?.toString() ?: "Нет данных",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = KeeneticColors.TextPrimary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VpnStatusCard(status: VpnServerStatus) {
    SectionCard(
        title = "VPN-сервер",
        subtitle = status.interfaceName.ifBlank { "Не интерфейс" },
        icon = Icons.Default.VpnKey
    ) {
        InfoRow(
            label = "Состояние",
            value = if (status.enabled) "Активен" else "Выключен",
            valueColor = if (status.enabled) KeeneticColors.Success else KeeneticColors.TextSecondary
        )
        InfoRow("Протокол", status.type.uppercase())
        if (status.port > 0) {
            InfoRow("Порт", status.port.toString(), monospace = true)
        }
        if (status.address.isNotBlank()) {
            InfoRow("Адрес", status.address, monospace = true)
        }
        InfoRow("Подключено клиентов", status.connectedClients.toString())
        RowDivider()
        Text(
            "Изменение протокола и ключей выполняется в веб-интерфейсе KeeneticOS.",
            style = MaterialTheme.typography.labelSmall,
            color = KeeneticColors.TextSecondary
        )
    }
}

@Composable
private fun VpnPeerCard(peer: VpnPeer) {
    SectionCard(
        title = peer.name.ifBlank { "Peer" },
        subtitle = if (peer.bytesReceived > 0 || peer.bytesSent > 0) "Активен" else "Ожидает",
        icon = Icons.Default.Computer
    ) {
        if (peer.publicKey.isNotBlank()) {
            InfoRow("Public key", peer.publicKey.take(16) + "…", monospace = true)
        }
        if (peer.allowedIp.isNotBlank()) {
            InfoRow("Allowed IPs", peer.allowedIp, monospace = true)
        }
        if (peer.bytesReceived > 0 || peer.bytesSent > 0) {
            RowDivider()
            InfoRow("Принято", formatBytes(peer.bytesReceived), monospace = true)
            InfoRow("Отправлено", formatBytes(peer.bytesSent), monospace = true)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.2f GB".format(gb)
}
