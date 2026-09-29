package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.UpnpPinhole
import com.keenetic.local.api.UpnpRedirect
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.EmptyHint
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SubGroupHeader

@Composable
fun UpnpScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val redirects by viewModel.upnpRedirects.collectAsState()
    val pinholes by viewModel.upnpPinholes.collectAsState()
    val unsupported by viewModel.unsupportedFeatures.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkFeatureSupport("upnp", "upnp/redirect")
        viewModel.loadUpnpStatus()
    }

    SectionScaffold(
        title = "UPnP / NAT-PMP",
        subtitle = "Проброс портов приложениями",
        onBack = onBack,
        onRefresh = { viewModel.loadUpnpStatus() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (unsupported.contains("upnp")) {
                item(key = "unsupported") {
                    UnsupportedNotice(
                        "UPnP / NAT-PMP",
                        "RCI-путь show/upnp/redirect не поддерживается на данной прошивке KN-2311 (fw 5.01.C.4.0-1). " +
                            "UPnP-переадресация управляется через веб-интерфейс, но не через REST API."
                    )
                }
            }

            item(key = "redirects-header") {
                SubGroupHeader("Переадресации UPnP", redirects.size)
            }

            if (redirects.isEmpty()) {
                item(key = "redirects-empty") {
                    SectionCard {
                        EmptyHint(
                            "Приложения локальной сети не создавали переадресаций. " +
                                "Они появляются автоматически, когда программа пробрасывает порт через UPnP."
                        )
                    }
                }
            } else {
                items(redirects, key = { it.name + it.externalPort + it.internalIp }) { redirect ->
                    UpnpRedirectCard(redirect)
                }
            }

            item(key = "pinholes-header") {
                SubGroupHeader("Дыры в NAT (pinhole)", pinholes.size)
            }

            if (pinholes.isEmpty()) {
                item(key = "pinholes-empty") {
                    SectionCard {
                        EmptyHint(
                            "Дыр в NAT нет. Их создают приложения при работе с NAT-PMP, " +
                                "в отличие от обычной переадресации портов они не привязаны к протоколу."
                        )
                    }
                }
            } else {
                items(pinholes, key = { it.name + it.port + it.internalIp }) { pinhole ->
                    UpnpPinholeCard(pinhole)
                }
            }
        }
    }
}

@Composable
private fun UpnpRedirectCard(redirect: UpnpRedirect) {
    SectionCard(
        title = redirect.name,
        subtitle = if (redirect.enabled) "Активна" else "Отключена",
        icon = Icons.Default.Router
    ) {
        InfoRow("Протокол", redirect.proto)
        InfoRow("Внешний порт", redirect.externalPort, monospace = true)
        InfoRow(
            "Внутренний адрес",
            "${redirect.internalIp}:${redirect.internalPort}",
            monospace = true
        )
    }
}

@Composable
private fun UpnpPinholeCard(pinhole: UpnpPinhole) {
    SectionCard(
        title = pinhole.name,
        subtitle = if (pinhole.enabled) "Активна" else "Отключена",
        icon = Icons.Default.Security
    ) {
        InfoRow("Протокол", pinhole.proto)
        InfoRow("Порт", pinhole.port, monospace = true)
        InfoRow("Внутренний адрес", pinhole.internalIp, monospace = true)
    }
}
