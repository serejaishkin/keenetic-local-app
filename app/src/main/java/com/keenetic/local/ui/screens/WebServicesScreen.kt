package com.keenetic.local.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.keenetic.local.data.SavedService
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebServicesScreen(viewModel: RouterViewModel) {
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("80") }
    var url by remember { mutableStateOf("") }
    var showBrowser by remember { mutableStateOf(false) }
    var browserMode by remember { mutableStateOf("inline") }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var serviceName by remember { mutableStateOf("") }
    var serviceUsername by remember { mutableStateOf("") }
    var servicePassword by remember { mutableStateOf("") }
    var editingService by remember { mutableStateOf<SavedService?>(null) }
    val networkHint by viewModel.networkHint.collectAsState()
    val savedServices by viewModel.savedServices.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) { viewModel.refreshNetworkHint() }

    val candidateHosts = remember(networkHint.suggestedRouterIps) {
        networkHint.suggestedRouterIps.take(3)
    }

    fun openService(targetHost: String, targetPort: String, mode: String) {
        val normalizedHost = targetHost.trim()
        val normalizedPort = targetPort.trim().ifBlank { "80" }
        val builtUrl = if (normalizedHost.startsWith("http://") || normalizedHost.startsWith("https://")) {
            normalizedHost
        } else {
            "http://$normalizedHost:$normalizedPort"
        }
        if (builtUrl.isNotBlank()) {
            host = normalizedHost
            port = normalizedPort
            url = builtUrl
            showBrowser = true
            browserMode = mode
        }
    }

    fun saveCurrentService() {
        val normalizedHost = host.trim()
        if (normalizedHost.isEmpty()) return
        val normalizedPort = port.trim().ifBlank { "80" }
        val finalName = serviceName.trim().ifBlank { if (normalizedHost.contains(".")) normalizedHost else "Сервис" }
        viewModel.saveService(
            SavedService(
                name = finalName,
                host = normalizedHost,
                port = normalizedPort,
                username = serviceUsername.trim(),
                password = servicePassword.trim()
            )
        )
        serviceName = ""
        serviceUsername = ""
        servicePassword = ""
        host = ""
        port = "80"
        editingService = null
    }

    fun deleteService(service: SavedService) {
        viewModel.deleteService(service)
    }

    fun editService(service: SavedService) {
        editingService = service
        serviceName = service.name
        serviceUsername = service.username
        servicePassword = service.password
        host = service.host
        port = service.port
    }

    fun cancelEditing() {
        editingService = null
        serviceName = ""
        serviceUsername = ""
        servicePassword = ""
        host = ""
        port = "80"
    }

    fun fillSuggestedHost(suggestedHost: String) {
        host = suggestedHost
        if (port.isBlank()) {
            port = "80"
        }
    }

    fun applyCurrentNetworkHint() {
        val preferred = networkHint.gateway ?: networkHint.currentIp ?: networkHint.suggestedRouterIps.firstOrNull().orEmpty()
        if (preferred.isNotBlank()) {
            host = preferred
            if (port.isBlank()) port = "80"
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "Веб-сервисы",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Мини-браузер для AWG Manager, Nfqws2 и других сервисов внутри приложения",
                style = MaterialTheme.typography.bodySmall,
                color = KeeneticColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Быстрый доступ", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (candidateHosts.isNotEmpty()) {
                        Text("Подсказки по локальной сети", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            candidateHosts.forEach { suggested ->
                                OutlinedButton(onClick = { fillSuggestedHost(suggested) }) {
                                    Text(suggested)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (savedServices.isNotEmpty()) {
                        Text("Сохранённые сервисы", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            savedServices.forEach { service ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(service.name, fontWeight = FontWeight.Medium)
                                        Text("${service.host}:${service.port}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(onClick = { openService(service.host, service.port, "inline") }) {
                                            Text("Открыть")
                                        }
                                        OutlinedButton(onClick = { editService(service) }) {
                                            Text("Изменить")
                                        }
                                        OutlinedButton(onClick = { deleteService(service) }) {
                                            Text("Удалить")
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    OutlinedTextField(
                        value = serviceName,
                        onValueChange = { serviceName = it },
                        label = { Text("Название сервиса") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("IP или hostname") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = serviceUsername,
                        onValueChange = { serviceUsername = it },
                        label = { Text("Логин") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = servicePassword,
                        onValueChange = { servicePassword = it },
                        label = { Text("Пароль") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it.filter(Char::isDigit) },
                        label = { Text("Порт") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { openService(host, port, "separate") }) {
                                Icon(Icons.Default.Public, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Открыть")
                            }
                            Button(onClick = { saveCurrentService() }) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (editingService != null) "Обновить" else "Сохранить")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { openService(host, port, "separate") }) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Открыть в окне")
                            }
                            if (editingService != null) {
                                OutlinedButton(onClick = { cancelEditing() }) {
                                    Text("Отмена")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Подсказка: сохраните сервис вручную, а затем открывайте его одним тапом.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                    Button(onClick = {
                        viewModel.refreshNetworkHint()
                        applyCurrentNetworkHint()
                    }) {
                        Text("Спросить телефон о текущем IP")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (showBrowser && url.isNotBlank()) {
                if (browserMode == "separate") {
                    Card(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 420.dp, max = 900.dp),
                        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(onClick = { showBrowser = false }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Назад")
                                }
                                Button(onClick = { webViewRef?.goBack() }, enabled = canGoBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Назад по странице")
                                }
                                Button(onClick = { webViewRef?.goForward() }, enabled = canGoForward) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Вперёд")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            AndroidView(
                                factory = { context ->
                                    WebView(context).apply {
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        settings.allowFileAccess = true
                                        settings.setSupportZoom(true)
                                        settings.builtInZoomControls = true
                                        webViewClient = object : WebViewClient() {
                                            override fun onPageFinished(view: WebView, url: String) {
                                                canGoBack = view.canGoBack()
                                                canGoForward = view.canGoForward()
                                            }
                                        }
                                        webChromeClient = WebChromeClient()
                                        loadUrl(url)
                                    }
                                },
                                update = { webView ->
                                    webViewRef = webView
                                    if (webView.url != url && url.isNotBlank()) {
                                        webView.loadUrl(url)
                                    }
                                    canGoBack = webView.canGoBack()
                                    canGoForward = webView.canGoForward()
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 360.dp, max = 640.dp),
                        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(onClick = { webViewRef?.goBack() }, enabled = canGoBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Назад")
                                }
                                Button(onClick = { webViewRef?.goForward() }, enabled = canGoForward) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Вперёд")
                                }
                                Text(
                                    text = "встроенный режим",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KeeneticColors.TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            AndroidView(
                                factory = { context ->
                                    WebView(context).apply {
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        settings.allowFileAccess = true
                                        settings.setSupportZoom(true)
                                        settings.builtInZoomControls = true
                                        webViewClient = object : WebViewClient() {
                                            override fun onPageFinished(view: WebView, url: String) {
                                                canGoBack = view.canGoBack()
                                                canGoForward = view.canGoForward()
                                            }
                                        }
                                        webChromeClient = WebChromeClient()
                                        loadUrl(url)
                                    }
                                },
                                update = { webView ->
                                    webViewRef = webView
                                    if (webView.url != url && url.isNotBlank()) {
                                        webView.loadUrl(url)
                                    }
                                    canGoBack = webView.canGoBack()
                                    canGoForward = webView.canGoForward()
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Структура без API", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Сервис пока работает как простая внутренняя ссылка: укажите IP/hostname и порт, и приложение откроет страницу внутри WebView.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}
