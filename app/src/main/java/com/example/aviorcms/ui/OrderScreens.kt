@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.aviorcms.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.aviorcms.CmsViewModel
import com.example.aviorcms.api.InlineNewClient

@Composable
fun OrderListScreen(
    viewModel: CmsViewModel,
    onAddOrder: () -> Unit,
    onOrderClick: (Int) -> Unit,
    onBack: () -> Unit
) {
    val orders by viewModel.orders.collectAsState()
    val meta by viewModel.meta.collectAsState()
    var selectedStatus by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Заказы") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddOrder, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Добавить заказ")
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = viewModel.isLoading.collectAsState().value,
            onRefresh = { viewModel.refreshData() },
            modifier = Modifier.padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedStatus == null,
                        onClick = {
                            selectedStatus = null
                            viewModel.loadOrders(null)
                        },
                        label = { Text("Все") }
                    )
                    meta?.statuses?.forEach { status ->
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = {
                                selectedStatus = status
                                viewModel.loadOrders(status)
                            },
                            label = { Text(status) }
                        )
                    }
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    if (orders.isEmpty() && !viewModel.isLoading.value) {
                        item { EmptyState("Заказов пока нет") }
                    }

                    items(orders) { order ->
                        ListItem(
                            headlineContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(getDeviceIcon(order.deviceType))
                                    Spacer(Modifier.width(8.dp))
                                    Text(order.deviceType + " " + (order.deviceModel ?: ""), style = MaterialTheme.typography.titleMedium)
                                }
                            },
                            supportingContent = {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(order.clientName)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = getStatusColor(order.status))
                                        Spacer(Modifier.width(4.dp))
                                        AnimatedContent(targetState = order.status, label = "status") { status ->
                                            Text(status, color = getStatusColor(status))
                                        }
                                    }
                                }
                            },
                            trailingContent = {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(order.orderNo, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            },
                            modifier = Modifier.clickable { onOrderClick(order.id) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}

@Composable
fun AddOrderScreen(
    viewModel: CmsViewModel,
    onOrderAdded: (Int?, InlineNewClient?, String, String?, String, Double?) -> Unit,
    onBack: () -> Unit
) {
    val meta by viewModel.meta.collectAsState()

    // Упрощено 19.08: приложение — только для оперативного занесения
    // заказа с выезда/на месте, без выбора "существующий клиент" (это
    // теперь делается на десктопе). Каждый раз — новое ФИО+телефон;
    // дедупликация по телефону происходит на сервере автоматически
    // (find_or_create_client() в functions.php) — если клиент с таким
    // номером уже есть, сервер сам подставит существующую запись, не
    // будет дублей, даже если ввели одного и того же клиента дважды.
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var clientSource by remember { mutableStateOf<String?>(null) }
    var sourceDropdownExpanded by remember { mutableStateOf(false) }
    val foundClient by viewModel.foundClientByPhone.collectAsState()
    var suggestionDismissed by remember { mutableStateOf(false) }

    var deviceType by remember { mutableStateOf("") }
    var deviceTypeDropdownExpanded by remember { mutableStateOf(false) }

    var deviceModel by remember { mutableStateOf("") }
    var modelSuggestionsExpanded by remember { mutableStateOf(false) }
    val modelSuggestions = remember(deviceModel, meta) {
        val all = meta?.deviceModels ?: emptyList()
        if (deviceModel.isBlank()) emptyList()
        else all.filter { it.contains(deviceModel, ignoreCase = true) }.take(8)
    }

    var description by remember { mutableStateOf("") }
    var priceEstimate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadMeta() }
    DisposableEffect(Unit) { onDispose { viewModel.clearFoundClientByPhone() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новый заказ") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = clientName,
                onValueChange = { clientName = it },
                label = { Text("ФИО клиента") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = clientPhone,
                onValueChange = {
                    clientPhone = it
                    suggestionDismissed = false
                    viewModel.searchClientByPhone(it)
                },
                label = { Text("Телефон") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Подсказка «Найден: ...» — если введённый телефон совпал с
            // уже существующим клиентом, предлагаем не перепечатывать имя
            // (сервер и так не создаст дубль по телефону, но так быстрее).
            if (foundClient != null && !suggestionDismissed && clientName.isBlank()) {
                Card(
                    onClick = {
                        clientName = foundClient!!.fullName
                        suggestionDismissed = true
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Найден: ${foundClient!!.fullName}", fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                            Text("Нажмите, чтобы подставить имя", style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = { suggestionDismissed = true }) {
                            Icon(Icons.Default.Close, contentDescription = "Скрыть")
                        }
                    }
                }
            }

            Box {
                OutlinedTextField(
                    value = meta?.clientSources?.get(clientSource) ?: "Источник (необязательно)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Источник") },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = sourceDropdownExpanded,
                    onDismissRequest = { sourceDropdownExpanded = false }
                ) {
                    meta?.clientSources?.forEach { (key, value) ->
                        DropdownMenuItem(
                            text = { Text(value) },
                            onClick = {
                                clientSource = key
                                sourceDropdownExpanded = false
                            }
                        )
                    }
                }
                Box(Modifier.matchParentSize().clickable { sourceDropdownExpanded = true })
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            // Тип устройства — только выбор из списка, что реально
            // ремонтируем, без ручного ввода (список приходит с сервера,
            // тот же самый, что в веб-CRM — device_type_options()).
            Box {
                OutlinedTextField(
                    value = deviceType.ifBlank { "Тип устройства" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Тип устройства") },
                    leadingIcon = { Icon(Icons.Default.Build, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = deviceTypeDropdownExpanded,
                    onDismissRequest = { deviceTypeDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    meta?.deviceTypes?.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                deviceType = type
                                deviceTypeDropdownExpanded = false
                            }
                        )
                    }
                }
                Box(Modifier.matchParentSize().clickable { deviceTypeDropdownExpanded = true })
            }

            // Модель — свободный ввод, но с подсказками из каталога
            // моделей веб-CRM (~200 популярных) — фильтр на лету по мере
            // набора, без отдельного запроса на сервер.
            Box {
                OutlinedTextField(
                    value = deviceModel,
                    onValueChange = {
                        deviceModel = it
                        modelSuggestionsExpanded = it.isNotBlank()
                    },
                    label = { Text("Модель") },
                    leadingIcon = { Icon(Icons.Default.Devices, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = modelSuggestionsExpanded && modelSuggestions.isNotEmpty(),
                    onDismissRequest = { modelSuggestionsExpanded = false },
                    properties = androidx.compose.ui.window.PopupProperties(focusable = false)
                ) {
                    modelSuggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = {
                                deviceModel = suggestion
                                modelSuggestionsExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание проблемы") },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = priceEstimate,
                onValueChange = { if (it.all { char -> char.isDigit() }) priceEstimate = it },
                label = { Text("Оценка стоимости, ₽ (необязательно)") },
                leadingIcon = { Icon(Icons.Default.MonetizationOn, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val inlineClient = InlineNewClient(fullName = clientName, phone = clientPhone, source = clientSource)
                    onOrderAdded(
                        null,
                        inlineClient,
                        deviceType,
                        deviceModel.ifBlank { null },
                        description,
                        priceEstimate.toDoubleOrNull()
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp).pressScale(),
                enabled = clientName.isNotBlank() && clientPhone.isNotBlank() &&
                        deviceType.isNotBlank() && description.isNotBlank()
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("СОЗДАТЬ ЗАКАЗ")
            }
        }
    }
}

@Composable
fun OrderDetailScreen(
    orderId: Int,
    viewModel: CmsViewModel,
    onBack: () -> Unit
) {
    val orders by viewModel.orders.collectAsState()
    val order = orders.find { it.id == orderId }
    val meta by viewModel.meta.collectAsState()
    val context = LocalContext.current

    var showShareMenu by remember { mutableStateOf(false) }
    var selectedUrlForDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    fun shareUrl(url: String, title: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "$title: $url")
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, null))
    }

    fun sendToWhatsApp(phone: String, url: String, title: String) {
        try {
            val cleanPhone = phone.replace(Regex("[^0-9]"), "")
            val message = "$title: $url"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${android.net.Uri.encode(message)}".toUri()
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "WhatsApp не установлен", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendToEmail(email: String, url: String, title: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:".toUri()
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Здравствуйте! Ссылка на документ: $url")
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Почтовое приложение не найдено", Toast.LENGTH_SHORT).show()
        }
    }

    // Быстрая связь с клиентом (иконки рядом с карточкой заказа) — не
    // про пересылку документов, просто открыть звонок/чат с его номером,
    // чтобы не искать контакт отдельно.
    fun callClient(phone: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$phone".toUri()))
        } catch (_: Exception) {
            Toast.makeText(context, "Не удалось открыть набор номера", Toast.LENGTH_SHORT).show()
        }
    }
    fun smsClient(phone: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_SENDTO, "smsto:$phone".toUri()))
        } catch (_: Exception) {
            Toast.makeText(context, "Не удалось открыть SMS", Toast.LENGTH_SHORT).show()
        }
    }
    fun openWhatsAppChat(phone: String) {
        try {
            val cleanPhone = phone.replace(Regex("[^0-9]"), "")
            context.startActivity(Intent(Intent.ACTION_VIEW, "https://api.whatsapp.com/send?phone=$cleanPhone".toUri()))
        } catch (_: Exception) {
            Toast.makeText(context, "WhatsApp не установлен", Toast.LENGTH_SHORT).show()
        }
    }
    fun openTelegramChat(phone: String) {
        try {
            val cleanPhone = phone.replace(Regex("[^0-9]"), "")
            context.startActivity(Intent(Intent.ACTION_VIEW, "tg://resolve?phone=$cleanPhone".toUri()))
        } catch (_: Exception) {
            Toast.makeText(context, "Telegram не установлен, либо номер не привязан к аккаунту", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(orderId) {
        if (order == null) viewModel.loadOrders(null)
    }

    if (selectedUrlForDialog != null && order != null) {
        val (url, title) = selectedUrlForDialog!!
        var customContact by remember { mutableStateOf("") }
        var isEditingContact by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { selectedUrlForDialog = null },
            title = { Text("Отправить документ") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, style = MaterialTheme.typography.bodyMedium)
                    if (isEditingContact) {
                        OutlinedTextField(
                            value = customContact,
                            onValueChange = { customContact = it },
                            label = { Text("Введите номер или Email") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Клиент: ${order.clientName}", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text("Телефон: ${order.clientPhone}")
                        order.clientEmail?.let { Text("Email: $it") }
                    }
                    TextButton(onClick = {
                        isEditingContact = !isEditingContact
                        if (isEditingContact) customContact = ""
                    }) {
                        Text(if (isEditingContact) "Использовать данные из заказа" else "Ввести контакт вручную")
                    }
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val target = if (isEditingContact) customContact else order.clientPhone
                            sendToWhatsApp(target, url, title)
                            selectedUrlForDialog = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("В WhatsApp")
                    }

                    Button(
                        onClick = {
                            val target = if (isEditingContact) customContact else (order.clientEmail ?: "")
                            sendToEmail(target, url, title)
                            selectedUrlForDialog = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isEditingContact || !order.clientEmail.isNullOrBlank()
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("На Почту")
                    }

                    OutlinedButton(
                        onClick = {
                            shareUrl(url, title)
                            selectedUrlForDialog = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Другие способы")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedUrlForDialog = null }) { Text("Отмена") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Заказ №${order?.orderNo ?: ""}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (order?.receiptUrl != null || order?.reportUrl != null) {
                        IconButton(onClick = { showShareMenu = true }) {
                            Icon(Icons.Default.Share, contentDescription = "Поделиться")
                        }
                        DropdownMenu(
                            expanded = showShareMenu,
                            onDismissRequest = { showShareMenu = false }
                        ) {
                            order.receiptUrl?.let { url ->
                                DropdownMenuItem(
                                    text = { Text("Приёмная квитанция") },
                                    onClick = {
                                        selectedUrlForDialog = url to "Приёмная квитанция по заказу ${order.orderNo}"
                                        showShareMenu = false
                                    }
                                )
                            }
                            order.reportUrl?.let { url ->
                                DropdownMenuItem(
                                    text = { Text("Акт выполненных работ") },
                                    onClick = {
                                        selectedUrlForDialog = url to "Акт выполненных работ по заказу ${order.orderNo}"
                                        showShareMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (order == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("Заказ не найден") }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(text = "${order.deviceType} ${order.deviceModel ?: ""}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(text = "Клиент: ${order.clientName}")
                        }
                        Spacer(Modifier.height(10.dp))
                        // Быстрая связь — не про пересылку документов
                        // (то ниже, в блоке «Документы»), а чтобы сразу
                        // позвонить/написать клиенту, не выходя из заказа.
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalIconButton(onClick = { callClient(order.clientPhone) }) {
                                Icon(Icons.Default.Phone, contentDescription = "Позвонить")
                            }
                            FilledTonalIconButton(onClick = { smsClient(order.clientPhone) }) {
                                Icon(Icons.Default.Sms, contentDescription = "SMS")
                            }
                            FilledTonalIconButton(onClick = { openWhatsAppChat(order.clientPhone) }) {
                                Icon(Icons.Default.Chat, contentDescription = "WhatsApp")
                            }
                            FilledTonalIconButton(onClick = { openTelegramChat(order.clientPhone) }) {
                                Icon(Icons.Default.Send, contentDescription = "Telegram")
                            }
                        }
                    }
                }

                Text(text = "Описание проблемы:", style = MaterialTheme.typography.titleMedium)
                Text(text = order.problemDescription ?: "Не указана", style = MaterialTheme.typography.bodyLarge)

                HorizontalDivider()

                Text(text = "Документы:", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            order.receiptUrl?.let { url ->
                                selectedUrlForDialog = url to "Приёмная квитанция по заказу ${order.orderNo}"
                            }
                        },
                        enabled = order.receiptUrl != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Квитанция")
                    }

                    OutlinedButton(
                        onClick = {
                            order.reportUrl?.let { url ->
                                selectedUrlForDialog = url to "Акт выполненных работ по заказу ${order.orderNo}"
                            }
                        },
                        enabled = order.reportUrl != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Assignment, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Акт")
                    }
                }

                HorizontalDivider()

                Text(text = "История изменений:", style = MaterialTheme.typography.titleMedium)
                order.statusLog?.let { log ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        log.forEach { entry ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(12.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(getStatusColor(entry.status))
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(entry.status, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    Text(entry.changedAt, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    entry.comment?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(text = "Текущий статус: ${order.status}", style = MaterialTheme.typography.titleMedium)
                }

                Text(text = "Сменить статус:")
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    meta?.statuses?.forEach { status ->
                        FilterChip(
                            selected = order.status == status,
                            onClick = { viewModel.updateOrderStatus(order.id, status) },
                            label = { Text(status) }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
