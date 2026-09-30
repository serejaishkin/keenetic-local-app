package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.ConfirmDialog
import com.keenetic.local.ui.components.EditableRow
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.OptionPickerDialog
import com.keenetic.local.ui.components.RowDivider
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.theme.KeeneticColors

private val REBOOT_METHODS = listOf(
    RouterViewModel.RebootMethod.RCI.name to "RCI REST API (основной)",
    RouterViewModel.RebootMethod.SSH.name to "SSH JSch (резервный)"
)

@Composable
fun FirmwareScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val status by viewModel.firmwareStatus.collectAsState()
    val isRebooting by viewModel.isRebooting.collectAsState()
    val rebootMessage by viewModel.rebootMessage.collectAsState()
    val selectedRebootMethod by viewModel.selectedRebootMethod.collectAsState()
    val sshPort by viewModel.sshPort.collectAsState()
    val savedIp by viewModel.savedIp.collectAsState()
    val savedUsername by viewModel.savedUsername.collectAsState()
    var showRebootDialog by remember { mutableStateOf(false) }
    var methodPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadFirmwareStatus()
    }

    SectionScaffold(
        title = "KeeneticOS и система",
        subtitle = "Прошивка и перезагрузка",
        onBack = onBack,
        onRefresh = { viewModel.loadFirmwareStatus() }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rebootMessage?.let { msg ->
                item(key = "reboot-message") {
                    SectionCard(
                        trailing = {
                            TextButton(onClick = { viewModel.clearRebootMessage() }) {
                                Text("OK", color = KeeneticColors.Primary)
                            }
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = KeeneticColors.Primary)
                            Text(
                                msg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = KeeneticColors.TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item(key = "firmware") {
                SectionCard(
                    title = status?.title ?: "KeeneticOS",
                    subtitle = status?.channel ?: "Release",
                    icon = Icons.Default.SystemUpdate
                ) {
                    if (status?.model?.isNotBlank() == true) {
                        InfoRow("Модель", status?.model ?: "—")
                    }
                    if (status?.updateAvailable == true) {
                        InfoRow(
                            label = "Доступно обновление",
                            value = status?.availableVersion ?: "—",
                            valueColor = KeeneticColors.Success
                        )
                        if (status?.changelog?.isNotBlank() == true) {
                            Text(
                                status?.changelog ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = KeeneticColors.TextSecondary
                            )
                        }
                        // Установка прошивки - это загрузка файла в веб-интерфейс роутера,
                        // а не команда RCI: рабочий эндпоинт не документирован, поэтому
                        // кнопка не имитирует установку и не меняет локальное состояние.
                        Text(
                            "Установить обновление из этого приложения нельзя: прошивка " +
                                "загружается файлом через веб-интерфейс роутера " +
                                "(Обновления → KeeneticOS).",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = KeeneticColors.Success)
                            Text(
                                "Установлена актуальная версия ПО",
                                color = KeeneticColors.Success,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.loadFirmwareStatus() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = KeeneticColors.Primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Проверить обновления", color = KeeneticColors.Primary)
                    }
                }
            }

            item(key = "reboot") {
                SectionCard(
                    title = "Перезагрузка интернет-центра",
                    subtitle = "Все соединения будут кратковременно прерваны",
                    icon = Icons.Default.PowerSettingsNew
                ) {
                    EditableRow(
                        label = "Метод перезагрузки",
                        value = if (selectedRebootMethod == RouterViewModel.RebootMethod.SSH) {
                            "SSH (JSch)"
                        } else {
                            "RCI REST API"
                        },
                        onClick = { methodPicker = true }
                    )

                    if (selectedRebootMethod == RouterViewModel.RebootMethod.SSH) {
                        Text(
                            "Отправляет CLI-команду 'system reboot' через зашифрованный SSH2-сеанс " +
                                "(библиотека JSch). Незаменим, если веб-сервер роутера (порт 80/443) " +
                                "недоступен.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary
                        )
                        OutlinedTextField(
                            value = sshPort,
                            onValueChange = { viewModel.setSshPort(it.filter { ch -> ch.isDigit() }) },
                            label = { Text("SSH порт интернет-центра") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Lan,
                                contentDescription = null,
                                tint = KeeneticColors.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Отправка команды {'system': {'reboot': {}}} через REST API " +
                                    "локального интерфейса KeeneticOS.",
                                style = MaterialTheme.typography.bodySmall,
                                color = KeeneticColors.TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    RowDivider()

                    Button(
                        onClick = { showRebootDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isRebooting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KeeneticColors.Error.copy(alpha = 0.85f),
                            contentColor = KeeneticColors.Background
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isRebooting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = KeeneticColors.Background,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Отправка команды...")
                        } else {
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (selectedRebootMethod == RouterViewModel.RebootMethod.SSH) {
                                    "Перезагрузить через SSH (JSch)"
                                } else {
                                    "Перезагрузить через RCI API"
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (methodPicker) {
        OptionPickerDialog(
            title = "Метод перезагрузки",
            options = REBOOT_METHODS,
            selectedKey = selectedRebootMethod.name,
            onSelect = {
                viewModel.setSelectedRebootMethod(
                    if (it == RouterViewModel.RebootMethod.SSH.name) {
                        RouterViewModel.RebootMethod.SSH
                    } else {
                        RouterViewModel.RebootMethod.RCI
                    }
                )
            },
            onDismiss = { methodPicker = false }
        )
    }

    if (showRebootDialog) {
        ConfirmDialog(
            title = "Подтверждение перезагрузки",
            message = "Вы действительно хотите перезагрузить интернет-центр Keenetic? " +
                "Все активные сетевые сессии клиентов будут кратковременно прерваны.\n\n" +
                "Метод: " +
                (if (selectedRebootMethod == RouterViewModel.RebootMethod.SSH) "SSH (JSch)" else "RCI REST API") +
                "\nЦель: $savedIp:" +
                (if (selectedRebootMethod == RouterViewModel.RebootMethod.SSH) sshPort else "80/443") +
                "\nПользователь: $savedUsername",
            confirmLabel = "Да, перезагрузить",
            onConfirm = {
                viewModel.rebootRouter(selectedRebootMethod) { _, _ ->
                    showRebootDialog = false
                }
            },
            onDismiss = { if (!isRebooting) showRebootDialog = false }
        )
    }
}
