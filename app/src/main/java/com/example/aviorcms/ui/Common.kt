package com.example.aviorcms.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.aviorcms.ui.theme.StatusBlue
import com.example.aviorcms.ui.theme.StatusGreen
import com.example.aviorcms.ui.theme.StatusGrey
import com.example.aviorcms.ui.theme.StatusRed
import com.example.aviorcms.ui.theme.StatusYellow

/** Иконки по типу устройства — те же смысловые значки, что в веб-CRM. */
fun getDeviceIcon(deviceType: String): String = when {
    deviceType.contains("моноблок", true) -> "🖥"
    deviceType.contains("настольн", true) || deviceType.contains(" пк", true) -> "🖥️"
    deviceType.contains("ноутбук", true) -> "💻"
    deviceType.contains("смартфон", true) || deviceType.contains("телефон", true) -> "📱"
    deviceType.contains("планшет", true) -> "🔲"
    deviceType.contains("консол", true) -> "🎮"
    deviceType.contains("аксессуар", true) -> "🔌"
    else -> "🔧"
}

/** Цвет статуса — совпадает с палитрой .status-badge в веб-CRM. */
fun getStatusColor(status: String): Color = when (status) {
    "принят" -> StatusBlue
    "диагностика", "согласование" -> StatusYellow
    "в ремонте" -> StatusBlue
    "готов" -> StatusGreen
    "выдан" -> StatusGrey
    "отказ" -> StatusRed
    else -> StatusGrey
}

@Composable
fun EmptyState(message: String) {
    Column(
        modifier = Modifier.padding(vertical = 48.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "📭", style = MaterialTheme.typography.displayMedium)
        Text(text = message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
