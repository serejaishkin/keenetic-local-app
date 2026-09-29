package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Router
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.EmptyHint
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SubGroupHeader

/**
 * IPv6 на телефоне — только чтение: адреса, префиксы, подсети, DHCPv6-привязки
 * и маршруты. Правки IPv6 живут в «Маршрутизации» (вкладка IPv6) и в настройках
 * интерфейсов, здесь дублировать их не нужно.
 */
@Composable
fun Ipv6Screen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val addresses by viewModel.ipv6Addresses.collectAsState()
    val prefixes by viewModel.ipv6Prefixes.collectAsState()
    val routes by viewModel.ipv6Routes.collectAsState()
    val subnets by viewModel.ipv6Subnets.collectAsState()
    val dhcpBindings by viewModel.ipv6DhcpBindings.collectAsState()

    fun loadAll() {
        viewModel.loadIpv6Addresses()
        viewModel.loadIpv6Prefixes()
        viewModel.loadIpv6Routes()
        viewModel.loadIpv6Subnets()
        viewModel.loadIpv6DhcpBindings()
    }

    LaunchedEffect(Unit) { loadAll() }

    val isEmpty = addresses.isEmpty() && prefixes.isEmpty() && routes.isEmpty() &&
        subnets.isEmpty() && dhcpBindings.isEmpty()

    SectionScaffold(
        title = "IPv6",
        subtitle = "Адреса, префиксы и подсети",
        onBack = onBack,
        onRefresh = { loadAll() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isEmpty) {
                item(key = "empty") {
                    SectionCard {
                        EmptyHint(
                            "IPv6 не настроен: адреса, префиксы и маршруты отсутствуют. " +
                                "IPv6 включается в настройках WAN-интерфейса, если его " +
                                "предоставляет провайдер."
                        )
                    }
                }
            }

            if (addresses.isNotEmpty()) {
                item(key = "addresses-header") { SubGroupHeader("Адреса", addresses.size) }
                items(addresses, key = { it.address + it.interfaceName }) { address ->
                    SectionCard(title = address.address, icon = Icons.Default.Language) {
                        InfoRow("Интерфейс", address.interfaceName)
                        InfoRow("Префикс", "/${address.prefix}", monospace = true)
                    }
                }
            }

            if (prefixes.isNotEmpty()) {
                item(key = "prefixes-header") { SubGroupHeader("Префиксы", prefixes.size) }
                items(prefixes, key = { it.prefix + it.interfaceName }) { prefix ->
                    SectionCard(title = prefix.prefix, icon = Icons.Default.Language) {
                        InfoRow("Интерфейс", prefix.interfaceName)
                    }
                }
            }

            if (subnets.isNotEmpty()) {
                item(key = "subnets-header") { SubGroupHeader("Подсети", subnets.size) }
                items(subnets, key = { it.network + it.interfaceName }) { subnet ->
                    SectionCard(title = "${subnet.network}/${subnet.prefix}", icon = Icons.Default.Router) {
                        InfoRow("Интерфейс", subnet.interfaceName)
                    }
                }
            }

            if (dhcpBindings.isNotEmpty()) {
                item(key = "dhcp-header") { SubGroupHeader("Привязки DHCPv6", dhcpBindings.size) }
                items(dhcpBindings, key = { it.address + it.hostname }) { binding ->
                    SectionCard(
                        title = binding.hostname.ifBlank { binding.address },
                        icon = Icons.Default.Router,
                        subtitle = binding.address
                    ) {
                        InfoRow("DUID", binding.duid, monospace = true)
                        InfoRow("IA ID", binding.iaId, monospace = true)
                    }
                }
            }

            if (routes.isNotEmpty()) {
                item(key = "routes-header") { SubGroupHeader("Маршруты", routes.size) }
                items(routes, key = { it.network + it.gateway + it.interfaceName }) { route ->
                    SectionCard(
                        title = "${route.network}/${route.prefix}",
                        icon = Icons.Default.Route
                    ) {
                        InfoRow("Шлюз", route.gateway.ifBlank { "авто" }, monospace = true)
                        InfoRow("Интерфейс", route.interfaceName)
                    }
                }
            }
        }
    }
}
