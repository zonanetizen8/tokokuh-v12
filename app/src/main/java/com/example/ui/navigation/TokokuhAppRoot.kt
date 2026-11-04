package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import com.example.ui.transactions.TransactionHistoryScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.about.AboutScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.employees.EmployeeScreen
import com.example.ui.expenses.ExpenseScreen
import com.example.ui.help.HelpScreen
import com.example.ui.pos.PosScreen
import com.example.ui.printer.PrinterScreen
import com.example.ui.products.ProductListScreen
import com.example.ui.reports.ReportsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.shifts.ShiftScreen
import com.example.ui.theme.TokokuhAccent
import com.example.ui.theme.TokokuhBackground
import com.example.ui.theme.TokokuhCard
import com.example.ui.theme.TokokuhSecondary
import com.example.ui.theme.TokokuhTextPrimary
import com.example.ui.theme.TokokuhTextSecondary
import com.example.viewmodel.TokokuhViewModel

enum class Screen {
    DASHBOARD,
    POS,
    PRODUCTS,
    TRANSACTIONS,
    REPORTS,
    SETTINGS,
    PRINTER,
    EMPLOYEES,
    SHIFTS,
    EXPENSES,
    HELP,
    ABOUT
}

data class NavItem(
    val screen: Screen,
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun TokokuhAppRoot(
    viewModel: TokokuhViewModel
) {
    val navStack = remember { mutableStateListOf(Screen.DASHBOARD) }
    val currentScreen = navStack.lastOrNull() ?: Screen.DASHBOARD

    val userMessage by viewModel.userMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var preFillBarcodeForProduct by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userMessage) {
        val msg = userMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // Handle Back Press
    BackHandler(enabled = navStack.size > 1) {
        navStack.removeAt(navStack.lastIndex)
    }

    fun navigateTo(screen: Screen) {
        if (screen in listOf(Screen.DASHBOARD, Screen.POS, Screen.PRODUCTS, Screen.TRANSACTIONS, Screen.REPORTS, Screen.SETTINGS)) {
            navStack.clear()
            navStack.add(screen)
        } else {
            navStack.add(screen)
        }
    }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    val primaryNavItems = listOf(
        NavItem(Screen.DASHBOARD, "Beranda", Icons.Default.Dashboard, "nav_dashboard"),
        NavItem(Screen.POS, "Kasir", Icons.Default.PointOfSale, "nav_pos"),
        NavItem(Screen.PRODUCTS, "Produk", Icons.Default.Inventory2, "nav_products"),
        NavItem(Screen.TRANSACTIONS, "Transaksi", Icons.Default.ReceiptLong, "nav_transactions"),
        NavItem(Screen.REPORTS, "Laporan", Icons.Default.Assessment, "nav_reports"),
        NavItem(Screen.SETTINGS, "Pengaturan", Icons.Default.Settings, "nav_settings")
    )

    val isPrimaryScreen = currentScreen in primaryNavItems.map { it.screen }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = TokokuhBackground,
        bottomBar = {
            if (!isTablet && isPrimaryScreen) {
                NavigationBar(
                    containerColor = TokokuhCard,
                    contentColor = TokokuhTextPrimary
                ) {
                    primaryNavItems.forEach { item ->
                        val selected = currentScreen == item.screen
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateTo(item.screen) },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = if (selected) Color(0xFF1E1E1E) else TokokuhTextSecondary
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) TokokuhAccent else TokokuhTextSecondary
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = TokokuhAccent,
                                selectedIconColor = Color(0xFF1E1E1E),
                                unselectedIconColor = TokokuhTextSecondary
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // NavigationRail for Tablets
            if (isTablet && isPrimaryScreen) {
                NavigationRail(
                    containerColor = TokokuhCard,
                    contentColor = TokokuhTextPrimary,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    primaryNavItems.forEach { item ->
                        val selected = currentScreen == item.screen
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTo(item.screen) },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = if (selected) Color(0xFF1E1E1E) else TokokuhTextSecondary
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontSize = 11.sp,
                                    color = if (selected) TokokuhAccent else TokokuhTextSecondary
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                indicatorColor = TokokuhAccent
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (currentScreen) {
                    Screen.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToPos = { navigateTo(Screen.POS) },
                        onNavigateToProducts = { navigateTo(Screen.PRODUCTS) },
                        onNavigateToTransactions = { navigateTo(Screen.TRANSACTIONS) },
                        onNavigateToShifts = { navigateTo(Screen.SHIFTS) },
                        onNavigateToExpenses = { navigateTo(Screen.EXPENSES) },
                        onNavigateToReports = { navigateTo(Screen.REPORTS) }
                    )

                    Screen.POS -> PosScreen(
                        viewModel = viewModel,
                        onNavigateToAddProductWithBarcode = { barcode ->
                            preFillBarcodeForProduct = barcode
                            navigateTo(Screen.PRODUCTS)
                        }
                    )

                    Screen.PRODUCTS -> ProductListScreen(
                        viewModel = viewModel,
                        initialPreFillBarcode = preFillBarcodeForProduct
                    ).also {
                        preFillBarcodeForProduct = null
                    }

                    Screen.TRANSACTIONS -> TransactionHistoryScreen(
                        viewModel = viewModel
                    )

                    Screen.REPORTS -> ReportsScreen(
                        viewModel = viewModel
                    )

                    Screen.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToPrinter = { navigateTo(Screen.PRINTER) },
                        onNavigateToEmployees = { navigateTo(Screen.EMPLOYEES) },
                        onNavigateToShifts = { navigateTo(Screen.SHIFTS) },
                        onNavigateToExpenses = { navigateTo(Screen.EXPENSES) },
                        onNavigateToHelp = { navigateTo(Screen.HELP) },
                        onNavigateToAbout = { navigateTo(Screen.ABOUT) }
                    )

                    Screen.PRINTER -> PrinterScreen(
                        viewModel = viewModel
                    )

                    Screen.EMPLOYEES -> EmployeeScreen(
                        viewModel = viewModel
                    )

                    Screen.SHIFTS -> ShiftScreen(
                        viewModel = viewModel
                    )

                    Screen.EXPENSES -> ExpenseScreen(
                        viewModel = viewModel
                    )

                    Screen.HELP -> HelpScreen(
                        onBack = { navStack.removeAt(navStack.lastIndex) }
                    )

                    Screen.ABOUT -> AboutScreen(
                        onBack = { navStack.removeAt(navStack.lastIndex) }
                    )
                }
            }
        }
    }
}
