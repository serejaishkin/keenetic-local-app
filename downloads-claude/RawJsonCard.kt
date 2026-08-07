package com.keenetic.local.ui.screens.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.keenetic.local.ui.theme.KeeneticColors

/**
 * Состояние одного read-only запроса к роутеру. Раньше вместо этого
 * использовался просто JsonElement? - из-за чего ошибка запроса была
 * неотличима от "ещё грузится": UI застревал на "Загрузка..." навсегда
 * (реальный баг, замечен на VPN-сервере). Теперь ошибка - отдельная ветка.
 */
sealed class ApiCallState {
    object Loading : ApiCallState()
    data class Error(val message: String) : ApiCallState()
    data class Success(val data: JsonElement) : ApiCallState()
}

/**
 * Карточка для show-эндпоинтов, у которых RCI-путь подтверждён, но под
 * ответ ещё не написан отдельный parser/DTO с полями конкретной фичи.
 * Вместо одной нечитаемой строки JSON (как было раньше) - раскладывает
 * объект/массив в дерево с отступами, это уже прилично смотрится на
 * телефоне для большинства show-ответов Keenetic.
 */
@Composable
fun RawJsonCard(title: String, state: ApiCallState, emptyText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            when (state) {
                is ApiCallState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.height(14.dp).width(14.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Загрузка…", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                }
                is ApiCallState.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = KeeneticColors.Error, modifier = Modifier.height(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Не удалось загрузить: ${state.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.Error
                    )
                }
                is ApiCallState.Success -> {
                    if (isEmpty(state.data)) {
                        Text(emptyText, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                    } else {
                        JsonTree(state.data, depth = 0)
                    }
                }
            }
        }
    }
}

private fun isEmpty(el: JsonElement?): Boolean {
    if (el == null) return true
    return when {
        el.isJsonNull -> true
        el.isJsonArray -> el.asJsonArray.size() == 0
        el.isJsonObject -> el.asJsonObject.entrySet().isEmpty()
        else -> false
    }
}

/**
 * Простой рендер JSON деревом: ключ жирным, примитивные значения - сразу
 * рядом, вложенные объекты/массивы - с отступом на следующей строке. Не
 * претендует на красоту финального UI конкретной фичи - это временная
 * читаемая замена voidной строке toString(), пока не написан parser/DTO
 * под конкретный экран.
 */
@Composable
fun JsonTree(el: JsonElement, depth: Int) {
    val indent = (depth * 12).dp
    when {
        el.isJsonObject -> {
            val obj = el.asJsonObject
            Column {
                for ((key, value) in obj.entrySet()) {
                    if (value.isJsonPrimitive || value.isJsonNull) {
                        Row(modifier = Modifier.padding(start = indent, top = 2.dp, bottom = 2.dp)) {
                            Text(
                                "$key: ",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = KeeneticColors.TextPrimary
                            )
                            Text(
                                primitiveText(value),
                                style = MaterialTheme.typography.bodySmall,
                                color = KeeneticColors.TextSecondary
                            )
                        }
                    } else {
                        Text(
                            "$key:",
                            modifier = Modifier.padding(start = indent, top = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = KeeneticColors.TextPrimary
                        )
                        JsonTree(value, depth + 1)
                    }
                }
            }
        }
        el.isJsonArray -> {
            val arr = el.asJsonArray
            Column {
                arr.forEachIndexed { index, item ->
                    if (item.isJsonPrimitive || item.isJsonNull) {
                        Text(
                            "• ${primitiveText(item)}",
                            modifier = Modifier.padding(start = indent, top = 2.dp, bottom = 2.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary
                        )
                    } else {
                        Text(
                            "#$index",
                            modifier = Modifier.padding(start = indent, top = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = KeeneticColors.TextSecondary
                        )
                        JsonTree(item, depth + 1)
                    }
                }
            }
        }
        else -> Text(
            primitiveText(el),
            modifier = Modifier.padding(start = indent),
            style = MaterialTheme.typography.bodySmall,
            color = KeeneticColors.TextSecondary
        )
    }
}

private fun primitiveText(el: JsonElement): String {
    if (el.isJsonNull) return "—"
    val prim = el.asJsonPrimitive
    return if (prim.isString) prim.asString else prim.toString()
}
