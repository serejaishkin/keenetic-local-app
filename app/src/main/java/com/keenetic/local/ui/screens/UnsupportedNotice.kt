package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.theme.KeeneticColors

/**
 * Notice shown when the connected router does not support the requested RCI path
 * (component not installed on the running firmware), so the section no longer
 * renders as a silently empty page.
 */
@Composable
fun UnsupportedNotice(sectionTitle: String, description: String? = null) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface.copy(alpha = 0.6f))
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = KeeneticColors.Primary)
            Text(
                buildString {
                    append("Раздел «$sectionTitle» не поддерживается данным роутером.")
                    if (description != null) {
                        append("\n$description")
                    } else {
                        append("\nКомпонент не установлен на прошивке (маршрут show/* не найден).")
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = KeeneticColors.TextPrimary
            )
        }
    }
}