package com.example.aviorcms.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.aviorcms.R
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

/** Быстрая связь с клиентом (звонок). */
fun callClient(context: Context, phone: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$phone".toUri()))
    } catch (_: Exception) {
        Toast.makeText(context, "Не удалось открыть набор номера", Toast.LENGTH_SHORT).show()
    }
}

/** Быстрая связь с клиентом (SMS). */
fun smsClient(context: Context, phone: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_SENDTO, "smsto:$phone".toUri()))
    } catch (_: Exception) {
        Toast.makeText(context, "Не удалось открыть SMS", Toast.LENGTH_SHORT).show()
    }
}

/** Быстрая связь с клиентом (WhatsApp). */
fun openWhatsAppChat(context: Context, phone: String) {
    try {
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        context.startActivity(Intent(Intent.ACTION_VIEW, "https://api.whatsapp.com/send?phone=$cleanPhone".toUri()))
    } catch (_: Exception) {
        Toast.makeText(context, "WhatsApp не установлен", Toast.LENGTH_SHORT).show()
    }
}

/** Быстрая связь с клиентом (Telegram). */
fun openTelegramChat(context: Context, phone: String) {
    try {
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        context.startActivity(Intent(Intent.ACTION_VIEW, "tg://resolve?phone=$cleanPhone".toUri()))
    } catch (_: Exception) {
        Toast.makeText(context, "Telegram не установлен, либо номер не привязан к аккаунту", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Ряд из 4 иконок быстрой связи — готовый компонент поверх функций
 * выше, чтобы не собирать Row из 4 FilledTonalIconButton вручную в
 * каждом месте (используется в ClientDetailScreen). Настоящие логотипы
 * WhatsApp/Telegram — res/drawable/ic_whatsapp.xml, ic_telegram.xml
 * (Bootstrap Icons, MIT).
 */
@Composable
fun QuickContactRow(phone: String) {
    val context = LocalContext.current
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilledTonalIconButton(onClick = { callClient(context, phone) }) {
            Icon(Icons.Default.Phone, contentDescription = "Позвонить")
        }
        FilledTonalIconButton(onClick = { smsClient(context, phone) }) {
            Icon(Icons.Default.Sms, contentDescription = "SMS")
        }
        FilledTonalIconButton(onClick = { openWhatsAppChat(context, phone) }) {
            Icon(painterResource(id = R.drawable.ic_whatsapp), contentDescription = "WhatsApp")
        }
        FilledTonalIconButton(onClick = { openTelegramChat(context, phone) }) {
            Icon(painterResource(id = R.drawable.ic_telegram), contentDescription = "Telegram")
        }
    }
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
