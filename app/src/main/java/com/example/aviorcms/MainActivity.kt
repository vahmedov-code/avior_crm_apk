package com.example.aviorcms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.aviorcms.ui.AddOrderScreen
import com.example.aviorcms.ui.ClientDetailScreen
import com.example.aviorcms.ui.ClientsScreen
import com.example.aviorcms.ui.DashboardScreen
import com.example.aviorcms.ui.LoginScreen
import com.example.aviorcms.ui.OrderDetailScreen
import com.example.aviorcms.ui.OrderListScreen
import com.example.aviorcms.ui.theme.AviorCmsTheme

class MainActivity : ComponentActivity() {

    private val viewModel: CmsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AviorCmsTheme {
                val navController = rememberNavController()
                val isLoggedIn by viewModel.isLoggedIn.collectAsState()

                // Анимации переходов — заданы один раз на весь NavHost,
                // применяются ко всем экранам автоматически (не нужно
                // прописывать на каждом composable() отдельно). "Вперёд"
                // (открыли новый экран) — новый выезжает справа, старый
                // чуть уезжает влево. "Назад" (popBackStack/системная
                // кнопка назад) — зеркально, старый выезжает справа.
                NavHost(
                    navController = navController,
                    startDestination = if (isLoggedIn) "dashboard" else "login",
                    enterTransition = {
                        slideInHorizontally(initialOffsetX = { it }) + fadeIn()
                    },
                    exitTransition = {
                        slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut()
                    },
                    popEnterTransition = {
                        slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn()
                    },
                    popExitTransition = {
                        slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                    }
                ) {
                    composable("login") {
                        LoginScreen(viewModel) {
                            navController.navigate("dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }

                    composable("dashboard") {
                        DashboardScreen(
                            viewModel = viewModel,
                            onOpenOrders = { status ->
                                navController.navigate("orders/${status ?: "all"}")
                            },
                            onAddOrder = { navController.navigate("add_order") },
                            onOpenClients = { navController.navigate("clients") },
                            onLoggedOut = {
                                navController.navigate("login") {
                                    popUpTo(0)
                                }
                            }
                        )
                    }

                    composable(
                        "orders/{status}",
                        arguments = listOf(navArgument("status") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val statusArg = backStackEntry.arguments?.getString("status")
                        androidx.compose.runtime.LaunchedEffect(statusArg) {
                            viewModel.loadOrders(if (statusArg == "all") null else statusArg)
                        }
                        OrderListScreen(
                            viewModel = viewModel,
                            onAddOrder = { navController.navigate("add_order") },
                            onOrderClick = { orderId -> navController.navigate("order_detail/$orderId") },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("clients") {
                        ClientsScreen(
                            viewModel = viewModel,
                            onClientClick = { clientId -> navController.navigate("client_detail/$clientId") },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        "client_detail/{clientId}",
                        arguments = listOf(navArgument("clientId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val clientId = backStackEntry.arguments?.getInt("clientId") ?: return@composable
                        ClientDetailScreen(
                            clientId = clientId,
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("add_order") {
                        AddOrderScreen(
                            viewModel = viewModel,
                            onOrderAdded = { clientId, newClient, deviceType, deviceModel, description, price ->
                                viewModel.createOrder(clientId, newClient, deviceType, deviceModel, description, price) { success ->
                                    if (success) navController.popBackStack()
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        "order_detail/{orderId}",
                        arguments = listOf(navArgument("orderId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val orderId = backStackEntry.arguments?.getInt("orderId") ?: return@composable
                        OrderDetailScreen(
                            orderId = orderId,
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
