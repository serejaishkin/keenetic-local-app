package com.keenetic.local.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keenetic.local.api.VpnPeer
import com.keenetic.local.api.VpnServerStatus
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VpnAdvancedScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val vpnStatus by viewModel.vpnServerStatus.collectAsState()
    val raw by viewModel.vpnServerRaw.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadVpnServerStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "VPN-сервер",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = KeeneticColors.TextPrimary
                        )
                        Text(
                            "WireGuard, SSTP, OpenVPN, L2TP/IPsec",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад",
                            tint = KeeneticColors.TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadVpnServerStatus() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Обновить",
                            tint = KeeneticColors.Primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KeeneticColors.Background)
            )
        },
        containerColor = KeeneticColors.Background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Info banner
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = KeeneticColors.Primary.copy(alpha = 0.08f)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(KeeneticColors.Primary.copy(alpha = 0.3f))
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = KeeneticColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "VPN-сервер позволяет подключаться к локальной сети роутера извне через защищённые протоколы.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextPrimary
                        )
                    }
                }
            }

            if (vpnStatus != null) {
                val status = vpnStatus!!

                // Status card
                item {
                    VpnStatusCard(status)
                }

                // Peers section (WireGuard)
                if (status.type == "wireguard" && status.peers.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.People,
                                    contentDescription = null,
                                    tint = KeeneticColors.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "WireGuard пиры",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = KeeneticColors.TextPrimary
                                )
                            }
                            Text(
                                "${status.peers.size} шт.",
                                style = MaterialTheme.typography.bodySmall,
                                color = KeeneticColors.TextSecondary
                            )
                        }
                    }
                    items(status.peers, key = { it.name.ifBlank { it.publicKey } }) { peer ->
                        VpnPeerCard(peer)
                    }
                }
            } else if (raw is com.keenetic.local.ui.screens.common.ApiCallState.Loading) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.VpnKey,
                        title = "Загрузка...",
                        subtitle = "Получение данных с роутера"
                    )
                }
            } else {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.VpnKey,
                        title = "VPN-сервер не активен",
                        subtitle = "VPN-сервер выключен или не настроен"
                    )
                }
            }

            // Raw JSON toggle
            item {
                Spacer(modifier = Modifier.height(8.dp))
                var showRaw by remember { mutableStateOf(false) }

                OutlinedButton(
                    onClick = { showRaw = !showRaw },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = KeeneticColors.TextSecondary)
                ) {
                    Icon(
                        if (showRaw) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (showRaw) "Скрыть Raw JSON" else "Показать Raw JSON (RCI)"
                    )
                }

                if (showRaw) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "RCI: show vpn-server",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = KeeneticColors.TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                raw?.toString() ?: "Нет данных",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = KeeneticColors.TextPrimary,
                                fontSize = 11.sp
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
    val typeColor = when (status.type) {
        "wireguard" -> KeeneticColors.Primary
        "sstp" -> KeeneticColors.Success
        "openvpn" -> KeeneticColors.Warning
        "ipsec" -> KeeneticColors.Secondary
        else -> KeeneticColors.TextSecondary
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (status.enabled) typeColor.copy(alpha = 0.4f) else KeeneticColors.Divider
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(typeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            "VPN-сервер",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = KeeneticColors.TextPrimary
                        )
                        Text(
                            status.interfaceName.ifBlank { "Не интерфейс" },
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                }

                // Status badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (status.enabled) KeeneticColors.Success.copy(alpha = 0.15f) else KeeneticColors.SurfaceElevated
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (status.enabled) KeeneticColors.Success else KeeneticColors.TextSecondary)
                        )
                        Text(
                            if (status.enabled) "Активен" else "Выкл",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (status.enabled) KeeneticColors.Success else KeeneticColors.TextSecondary
                        )
                    }
                }
            }

            // Type badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = typeColor.copy(alpha = 0.12f)
            ) {
                Text(
                    status.type.uppercase(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = typeColor
                )
            }

            // Details row
            HorizontalDivider(color = KeeneticColors.Divider)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (status.port > 0) {
                    DetailItem("Порт", "${status.port}")
                }
                if (status.address.isNotBlank()) {
                    DetailItem("Адрес", status.address)
                }
                DetailItem("Пиры", "${status.connectedClients}")
            }
        }
    }
}

@Composable
private fun VpnPeerCard(peer: VpnPeer) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Computer,
                        contentDescription = null,
                        tint = KeeneticColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        peer.name.ifBlank { "Peer" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = KeeneticColors.TextPrimary
                    )
                }
                if (peer.bytesReceived > 0 || peer.bytesSent > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = KeeneticColors.Success.copy(alpha = 0.12f)
                    ) {
                        Text(
                            "Активен",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = KeeneticColors.Success
                        )
                    }
                }
            }

            if (peer.publicKey.isNotBlank()) {
                Text(
                    "Public key: ${peer.publicKey.take(16)}...",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = KeeneticColors.TextSecondary
                )
            }

            if (peer.allowedIp.isNotBlank()) {
                Text(
                    "Allowed IPs: ${peer.allowedIp}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = KeeneticColors.TextSecondary
                )
            }

            if (peer.bytesReceived > 0 || peer.bytesSent > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DetailItem("RX", formatBytes(peer.bytesReceived))
                    DetailItem("TX", formatBytes(peer.bytesSent))
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = KeeneticColors.TextPrimary
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = KeeneticColors.TextSecondary
        )
    }
}

@Composable
private fun EmptyStateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = KeeneticColors.TextSecondary,
                modifier = Modifier.size(36.dp)
            )
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = KeeneticColors.TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = KeeneticColors.TextSecondary.copy(alpha = 0.7f)
            )
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
