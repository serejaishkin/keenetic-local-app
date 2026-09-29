package com.keenetic.local.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.theme.KeeneticColors

/**
 * Общие каркасы и строки для всех разделов (этап G, правило G0).
 *
 * Здесь держим то, что должно выглядеть одинаково везде: одна шапка с одной
 * кнопкой «назад», карточки-секции с заголовочком, вертикальные строки
 * «подпись — значение» и кликабельные строки, открывающие диалог правки.
 * Экраны не должны рисовать своя шапка/карточки руками — иначе разъезжаются
 * отступы, минимальные тап-зоны и подписи.
 */
object SectionUi {
    const val MinTapTarget = 48
}

/** Одна шапка экрана: заголовок, подзаголовок, «назад» и «обновить». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionScaffold(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = KeeneticColors.TextPrimary
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = KeeneticColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                tint = KeeneticColors.TextPrimary
                            )
                        }
                    }
                },
                actions = {
                    if (onRefresh != null) {
                        IconButton(onClick = onRefresh) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Обновить",
                                tint = KeeneticColors.Primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KeeneticColors.Background)
            )
        },
        containerColor = KeeneticColors.Background,
        content = content
    )
}

/** Карточка-секция: соответствует одному блоку веб-морды. */
@Composable
fun SectionCard(
    title: String? = null,
    icon: ImageVector? = null,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = KeeneticColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = KeeneticColors.TextPrimary
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = KeeneticColors.TextSecondary
                            )
                        }
                    }
                    trailing?.invoke()
                }
            }
            content()
        }
    }
}

/** Строка «подпись — значение», только чтение. */
@Composable
fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = KeeneticColors.TextPrimary,
    monospace: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = KeeneticColors.TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (monospace) FontWeight.Medium else FontWeight.SemiBold,
            color = valueColor,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Строка параметра, открывающая диалог правки по тапу — как на сайте:
 * значение показано, меняется через форму с Сохранить/Отмена.
 */
@Composable
fun EditableRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    hint: String? = null,
    valueColor: Color = KeeneticColors.TextPrimary,
    monospaceValue: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SectionUi.MinTapTarget.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) KeeneticColors.TextSecondary else KeeneticColors.TextSecondary.copy(alpha = 0.6f)
            )
            if (!hint.isNullOrBlank()) {
                Text(
                    hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = KeeneticColors.TextSecondary.copy(alpha = 0.7f)
                )
            }
        }
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (monospaceValue) FontWeight.Medium else FontWeight.SemiBold,
            fontFamily = if (monospaceValue) FontFamily.Monospace else null,
            color = if (enabled) valueColor else KeeneticColors.TextSecondary,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = if (enabled) KeeneticColors.TextSecondary else KeeneticColors.Divider
        )
    }
}

/** Строка с переключателем справа — для булевых параметров. */
@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
    enabled: Boolean = true,
    unknown: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SectionUi.MinTapTarget.dp)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = KeeneticColors.TextPrimary
            )
            val status = description ?: if (unknown) "Состояние неизвестно" else null
            if (!status.isNullOrBlank()) {
                Text(
                    status,
                    style = MaterialTheme.typography.labelSmall,
                    color = KeeneticColors.TextSecondary
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled && !unknown
        )
    }
}

/** Заголовок подгруппы внутри карточки. */
@Composable
fun SubGroupHeader(title: String, count: Int? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = KeeneticColors.TextSecondary
        )
        if (count != null) {
            Text(
                " · $count",
                style = MaterialTheme.typography.labelLarge,
                color = KeeneticColors.TextSecondary.copy(alpha = 0.7f)
            )
        }
    }
}

/** Разделитель между строками карточки. */
@Composable
fun RowDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(color = KeeneticColors.Divider, modifier = modifier)
}

/** Заглушка вместо пустого списка или отсутствующих данных. */
@Composable
fun EmptyHint(
    text: String,
    modifier: Modifier = Modifier,
    centered: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = KeeneticColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/** Диалог подтверждения удаления/действия. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Удалить",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = KeeneticColors.Error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

/** Прокручиваемое тело диалога, если полей много. */
@Composable
fun DialogForm(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) { content() }
}

/**
 * Форма правки параметра: прокручиваемое тело и кнопки «Сохранить»/«Отмена»
 * с фиолетовой кнопкой подтверждения, как на сайте.
 */
@Composable
fun FormDialog(
    title: String,
    confirmLabel: String = "Сохранить",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        },
        text = { DialogForm { content() } },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) {
                Text(confirmLabel, color = KeeneticColors.Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

/** Диалог выбора одного варианта из списка — как переключатели на сайте. */
@Composable
fun OptionPickerDialog(
    title: String,
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
        text = {
            DialogForm {
                options.forEach { (key, name) ->
                    val isSelected = key == selectedKey
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(key)
                                onDismiss()
                            }
                            .heightIn(min = SectionUi.MinTapTarget.dp)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) KeeneticColors.Primary else KeeneticColors.TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = KeeneticColors.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
