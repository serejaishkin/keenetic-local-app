package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.RciCapture
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.theme.KeeneticColors

/**
 * «Снимок ответов RCI»: прогоняет read-only show-пути и сохраняет ответы одним
 * JSON-файлом в Download/Keenetic. Секреты по умолчанию заменяются на ***.
 */
@Composable
fun RciCaptureScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val progress by viewModel.rciCaptureProgress.collectAsState()
    val items by viewModel.rciCaptureItems.collectAsState()
    val running by viewModel.rciCaptureRunning.collectAsState()

    val selected = remember {
        mutableStateMapOf<String, Boolean>().also { m -> RciCapture.DEFAULT_PATHS.forEach { m[it] = true } }
    }
    var custom by remember { mutableStateOf("") }
    var maskSecrets by remember { mutableStateOf(true) }

    SectionScaffold(
        title = "Снимок ответов RCI",
        subtitle = "Только чтение. Ничего на роутере не меняется",
        onBack = onBack
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = maskSecrets, onCheckedChange = { maskSecrets = it }, enabled = !running)
                Spacer(Modifier.width(8.dp))
                Text("Скрыть пароли и ключи (***)", color = KeeneticColors.TextPrimary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { RciCapture.DEFAULT_PATHS.forEach { selected[it] = true } }, enabled = !running) { Text("Выбрать все") }
                OutlinedButton(onClick = { RciCapture.DEFAULT_PATHS.forEach { selected[it] = false } }, enabled = !running) { Text("Снять все") }
            }
            OutlinedTextField(
                value = custom,
                onValueChange = { custom = it },
                label = { Text("Свои пути show (по одному в строке)") },
                placeholder = { Text("interface/Wireguard0") },
                enabled = !running,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Button(
                onClick = {
                    val paths = RciCapture.DEFAULT_PATHS.filter { selected[it] == true } +
                        custom.lines().map { it.trim() }.filter { it.isNotEmpty() }
                    viewModel.runRciCapture(paths, maskSecrets)
                },
                enabled = !running,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (running) "Идёт сбор…" else "Собрать снимок") }

            if (progress.isNotEmpty()) Text(progress, color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            if (running) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            if (items.isNotEmpty() && !running) {
                Button(onClick = { viewModel.saveRciCapture(context) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Сохранить в Download/Keenetic")
                }
                Text("Результат", fontWeight = FontWeight.SemiBold, color = KeeneticColors.TextPrimary)
                items.forEach {
                    Text(
                        "${if (it.ok) "✓" else "✗"} show/${it.path} — ${it.bytes} Б",
                        color = if (it.ok) KeeneticColors.Success else KeeneticColors.Warning,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            HorizontalDivider(color = KeeneticColors.Divider)
            Text("Пути", fontWeight = FontWeight.SemiBold, color = KeeneticColors.TextPrimary)
            RciCapture.DEFAULT_PATHS.forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = selected[p] == true, onCheckedChange = { selected[p] = it }, enabled = !running)
                    Text("show/$p", color = KeeneticColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
