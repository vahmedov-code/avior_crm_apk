package com.example.aviorcms.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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

/**
 * Модификатор для кнопок/карточек — лёгкое сжатие при нажатии (поверх
 * стандартной ряби Material3, которая и так уже есть на любой Button/
 * Card «из коробки» — это дополнительный, более заметный отклик).
 * Использование: Button(modifier = Modifier.pressScale(), ...) { ... }
 */
@Composable
fun Modifier.pressScale(pressedScale: Float = 0.94f): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 800f),
        label = "pressScale"
    )
    return this.scale(scale)
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
