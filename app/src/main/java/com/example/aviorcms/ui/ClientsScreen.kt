package com.example.aviorcms.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aviorcms.CmsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ClientsScreen(viewModel: CmsViewModel, onBack: () -> Unit) {
    val clients by viewModel.clients.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Группируем клиентов по первой букве для алфавитного индекса
    val groupedClients = remember(clients) {
        clients.sortedBy { it.fullName }
            .groupBy { it.fullName.firstOrNull()?.uppercaseChar() ?: '#' }
    }
    
    val alphabet = remember(groupedClients) { groupedClients.keys.toList().sorted() }

    LaunchedEffect(Unit) { viewModel.loadClients(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Клиенты") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (clients.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Список клиентов пуст", style = MaterialTheme.typography.bodyLarge)
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
                                    leadingContent = { Icon(Icons.Default.Person, contentDescription = null) }
                                )
                                if (index < clientsInGroup.size - 1) {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                                }
                            }
                        }
                    }

                    // Алфавитный индекс сбоку
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
