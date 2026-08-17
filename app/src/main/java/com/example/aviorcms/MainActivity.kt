package com.example.aviorcms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.aviorcms.ui.AddOrderScreen
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

                NavHost(
                    navController = navController,
                    startDestination = if (isLoggedIn) "dashboard" else "login"
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
                        ClientsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
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
