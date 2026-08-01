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
    var browserUrl by remember { mutableStateOf("") }
    var browserTitle by remember { mutableStateOf("") }
    var showDedicatedBrowser by remember { mutableStateOf(false) }
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

    fun escapeForJs(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")

    fun injectCredentials(view: WebView, username: String, password: String) {
        val script = """
            (function() {
              const user = "${escapeForJs(username)}";
              const pass = "${escapeForJs(password)}";
              const textInputs = Array.from(document.querySelectorAll('input[type="text"], input[type="email"], input:not([type]), input[name*="user"], input[name*="login"], input[id*="user"], input[id*="login"]'));
              const passwordInputs = Array.from(document.querySelectorAll('input[type="password"]'));
              if (user && textInputs.length > 0) {
                textInputs[0].value = user;
                textInputs[0].dispatchEvent(new Event('input', { bubbles: true }));
              }
              if (pass && passwordInputs.length > 0) {
                passwordInputs[0].value = pass;
                passwordInputs[0].dispatchEvent(new Event('input', { bubbles: true }));
              }
            })();
        """.trimIndent()
        view.post { view.evaluateJavascript(script, null) }
    }

    fun openService(targetHost: String, targetPort: String, username: String = "", password: String = "") {
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
            browserUrl = builtUrl
            browserTitle = normalizedHost
            serviceUsername = username
            servicePassword = password
            showDedicatedBrowser = true
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
        if (showDedicatedBrowser && browserUrl.isNotBlank()) {
            Box(modifier = Modifier.fillMaxSize()) {
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
                                    injectCredentials(view, serviceUsername, servicePassword)
                                }
                            }
                            webChromeClient = WebChromeClient()
                            loadUrl(browserUrl)
                        }
                    },
                    update = { webView ->
                        webViewRef = webView
                        if (webView.url != browserUrl && browserUrl.isNotBlank()) {
                            webView.loadUrl(browserUrl)
                        }
                        canGoBack = webView.canGoBack()
                        canGoForward = webView.canGoForward()
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { showDedicatedBrowser = false }) {
                        Text("Закрыть окно")
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                        .align(Alignment.BottomCenter),
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
                }
            }
        } else {
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
                                            OutlinedButton(onClick = { openService(service.host, service.port, service.username, service.password) }) {
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
                                Button(onClick = { openService(host, port, serviceUsername, servicePassword) }) {
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
                                Button(onClick = { openService(host, port, serviceUsername, servicePassword) }) {
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
