package com.example.aviorcms.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aviorcms.CmsViewModel
import com.example.aviorcms.api.TokenStore
import androidx.compose.ui.platform.LocalContext
import com.example.aviorcms.BuildConfig

private data class PipelineCard(val label: String, val statuses: List<String>, val color: androidx.compose.ui.graphics.Color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: CmsViewModel,
    onOpenOrders: (String?) -> Unit,
    onAddOrder: () -> Unit,
    onOpenClients: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val context = LocalContext.current
    val orders by viewModel.orders.collectAsState()
    val fullName = remember { TokenStore.getFullName(context) ?: "" }

    LaunchedEffect(Unit) {
        viewModel.loadMeta()
        viewModel.loadOrders(null)
        viewModel.loadClients(null)
    }

    val cards = remember {
        listOf(
            PipelineCard("Новые", listOf("принят"), com.example.aviorcms.ui.theme.StatusBlue),
            PipelineCard("В работе", listOf("диагностика", "в ремонте"), com.example.aviorcms.ui.theme.StatusYellow),
            PipelineCard("Отложены", listOf("согласование"), com.example.aviorcms.ui.theme.StatusYellow),
            PipelineCard("Готовы", listOf("готов"), com.example.aviorcms.ui.theme.StatusGreen),
            PipelineCard("Выданы", listOf("выдан"), com.example.aviorcms.ui.theme.StatusGrey),
            PipelineCard("Отказы", listOf("отказ"), com.example.aviorcms.ui.theme.StatusRed)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("АВИОР"); Text(fullName, style = MaterialTheme.typography.labelSmall) } },
                actions = {
                    IconButton(onClick = {
                        viewModel.logout()
                        onLoggedOut()
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Выйти")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddOrder) {
                Icon(Icons.Default.Add, contentDescription = "Новый заказ")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Card(
                onClick = onOpenClients,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.People, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Клиенты", fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Заказы по статусу", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(cards) { card ->
                    val count = orders.count { it.status in card.statuses }
                    Card(
                        onClick = { onOpenOrders(if (card.statuses.size == 1) card.statuses.first() else null) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(card.color))
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(count.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text(card.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}
