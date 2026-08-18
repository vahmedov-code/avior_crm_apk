@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.aviorcms.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aviorcms.CmsViewModel
import com.example.aviorcms.R
import com.example.aviorcms.api.Client
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClientsScreen(
    viewModel: CmsViewModel,
    onClientClick: (Int) -> Unit,
    onBack: () -> Unit
) {
    val clients by viewModel.clients.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val filteredClients = remember(clients, searchQuery) {
        if (searchQuery.isBlank()) clients
        else clients.filter { 
            it.fullName.contains(searchQuery, ignoreCase = true) || 
            it.phone.contains(searchQuery) 
        }
    }

    // Группируем клиентов по первой букве для алфавитного индекса
    val groupedClients = remember(filteredClients) {
        filteredClients.sortedBy { it.fullName }
            .groupBy { it.fullName.firstOrNull()?.uppercaseChar() ?: '#' }
    }
    
    val alphabet = remember(groupedClients) { groupedClients.keys.toList().sorted() }

    LaunchedEffect(Unit) { viewModel.loadClients(null) }
    
    LaunchedEffect(isSearching) {
        if (isSearching) focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Поиск...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                        )
                    } else {
                        Text("Клиенты")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = if (isSearching) { { isSearching = false; searchQuery = "" } } else onBack) {
                        Icon(if (isSearching) Icons.Default.Close else Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (!isSearching) {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Поиск")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (filteredClients.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (searchQuery.isEmpty()) "Список клиентов пуст" else "Ничего не найдено",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Основной список
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        groupedClients.forEach { (letter, clientsInGroup) ->
                            stickyHeader {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = letter.toString(),
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            itemsIndexed(clientsInGroup) { index, client ->
                                ListItem(
                                    headlineContent = { Text(client.fullName) },
                                    supportingContent = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(client.phone)
                                        }
                                    },
                                    leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
                                    modifier = Modifier.clickable { onClientClick(client.id) }
                                )
                                if (index < clientsInGroup.size - 1) {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                                }
                            }
                        }
                    }

                    // Алфавитный индекс сбоку (скрываем при поиске, если список короткий)
                    if (alphabet.size > 5) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(32.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            alphabet.forEach { letter ->
                                Text(
                                    text = letter.toString(),
                                    modifier = Modifier
                                        .clickable {
                                            scope.launch {
                                                var targetIndex = 0
                                                for (key in alphabet) {
                                                    if (key == letter) break
                                                    targetIndex += 1 // Заголовок
                                                    targetIndex += groupedClients[key]?.size ?: 0
                                                }
                                                listState.animateScrollToItem(targetIndex)
                                            }
                                        }
                                        .padding(vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClientDetailScreen(
    clientId: Int,
    viewModel: CmsViewModel,
    onBack: () -> Unit
) {
    val clients by viewModel.clients.collectAsState()
    val client = clients.find { it.id == clientId }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Карточка клиента") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        if (client == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Клиент не найден")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = client.fullName.firstOrNull()?.toString() ?: "?",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                Text(
                    text = client.fullName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = client.phone,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                client.email?.let { email ->
                    if (email.isNotBlank()) {
                        Text(text = email, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Spacer(Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ContactActionItem(
                        icon = Icons.Default.Phone,
                        label = "Звонок",
                        onClick = { callClient(context, client.phone) }
                    )
                    ContactActionItem(
                        icon = Icons.Default.Sms,
                        label = "SMS",
                        onClick = { smsClient(context, client.phone) }
                    )
                    ContactActionItem(
                        painter = painterResource(id = R.drawable.ic_whatsapp),
                        label = "WhatsApp",
                        onClick = { openWhatsAppChat(context, client.phone) }
                    )
                    ContactActionItem(
                        painter = painterResource(id = R.drawable.ic_telegram),
                        label = "Telegram",
                        onClick = { openTelegramChat(context, client.phone) }
                    )
                }
                
                Spacer(Modifier.height(32.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!client.address.isNullOrBlank()) {
                        InfoRow(label = "Адрес", value = client.address)
                    }
                    if (!client.source.isNullOrBlank()) {
                        InfoRow(label = "Источник", value = client.source)
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    painter: androidx.compose.ui.graphics.painter.Painter? = null,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp)
    ) {
        FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(56.dp)) {
            if (icon != null) Icon(icon, contentDescription = label)
            else if (painter != null) Icon(painter, contentDescription = label, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
