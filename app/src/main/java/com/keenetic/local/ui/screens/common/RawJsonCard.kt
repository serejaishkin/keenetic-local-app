package com.keenetic.local.ui.screens.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.keenetic.local.ui.theme.KeeneticColors

/**
 * Карточка "как есть" для show-эндпоинтов, у которых RCI-путь подтверждён
 * (см. комментарии в KeeneticRestApi.kt), но точная схема ответа (массив /
 * объект-словарь / вложенность) ещё не оформлена в отдельный parser/DTO.
 * По опыту проекта - лучше показать реальный JSON, чем угадать схему и
 * словить "Expected BEGIN_ARRAY but was BEGIN_OBJECT". Как только придёт
 * реальный пример непустого ответа - меняем конкретный экран на нормальные
 * карточки, эта заглушка отсюда убирается точечно.
 */
@Composable
fun RawJsonCard(title: String, data: JsonElement?, emptyText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            when {
                data == null -> Text(
                    "Загрузка…",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
                isEmpty(data) -> Text(
                    emptyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
                else -> {
                    val text = remember(data) { data.toString() }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
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
