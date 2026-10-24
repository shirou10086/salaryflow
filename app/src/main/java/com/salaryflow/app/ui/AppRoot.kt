package com.salaryflow.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.salaryflow.app.core.SalaryClock
import com.salaryflow.app.data.TxType
import com.salaryflow.app.ui.screens.HomeScreen
import com.salaryflow.app.ui.screens.LedgerScreen
import com.salaryflow.app.ui.screens.QuickAddSheet
import com.salaryflow.app.ui.screens.SettingsScreen
import com.salaryflow.app.ui.screens.StatsScreen
import java.time.LocalDate

private data class NavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val navItems = listOf(
    NavItem("home", "首页", Icons.Rounded.Home),
    NavItem("ledger", "账本", Icons.AutoMirrored.Rounded.ReceiptLong),
    NavItem("stats", "统计", Icons.Rounded.BarChart),
    NavItem("settings", "设置", Icons.Rounded.Settings),
)

@Composable
fun AppRoot(
    viewModel: SalaryFlowViewModel,
    quickAddRequest: Boolean,
    celebrationRequest: Boolean,
    onQuickAddConsumed: () -> Unit,
    onCelebrationConsumed: () -> Unit,
) {
    val navController = rememberNavController()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var quickAddType by remember { mutableStateOf<TxType?>(null) }
    var forceCelebration by remember { mutableStateOf(false) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "home"

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { },
    )

    LaunchedEffect(Unit) { viewModel.settleIfDue() }

    LaunchedEffect(quickAddRequest) {
        if (quickAddRequest) {
            quickAddType = TxType.EXPENSE
            onQuickAddConsumed()
        }
    }

    LaunchedEffect(celebrationRequest) {
        if (celebrationRequest) {
            forceCelebration = true
            onCelebrationConsumed()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    navItems.forEach { item ->
                        NavigationBarItem(
                            selected = backStackEntry?.destination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == "ledger") {
                FloatingActionButton(
                    onClick = { quickAddType = TxType.EXPENSE },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "记一笔")
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenLedger = { navController.navigate("ledger") },
                    onQuickAdd = { type -> quickAddType = type },
                    onToggleService = {
                        if (settings.serviceEnabled) {
                            viewModel.stopTickerService()
                        } else {
                            val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS,
                                ) != PackageManager.PERMISSION_GRANTED
                            if (needsPermission) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            viewModel.startTickerService()
                        }
                    },
                )
            }
            composable("ledger") {
                LedgerScreen(
                    viewModel = viewModel,
                    onQuickAdd = { type -> quickAddType = type },
                )
            }
            composable("stats") { StatsScreen(viewModel = viewModel) }
            composable("settings") { SettingsScreen(viewModel = viewModel) }
        }
    }

    quickAddType?.let { type ->
        QuickAddSheet(
            initialType = type,
            onDismiss = { quickAddType = null },
            onSubmit = { selectedType, amountCents, category, note ->
                when (selectedType) {
                    TxType.EXPENSE -> viewModel.addExpense(amountCents, category, note)
                    TxType.INCOME -> viewModel.addIncome(amountCents, category, note)
                    TxType.SAVING_IN -> viewModel.addSaving(amountCents, note)
                    TxType.SAVING_OUT -> viewModel.addSaving(amountCents, note, withdraw = true)
                }
            },
        )
    }

    val pendingKey = settings.pendingCelebrationKey
    if (pendingKey != null || forceCelebration) {
        val periodLabel = pendingKey
            ?.takeUnless { it.startsWith("demo:") || it.startsWith("manual:") }
            ?.let { key ->
                runCatching {
                    SalaryClock(settings.toSalaryConfig())
                        .periodEndingAt(LocalDate.parse(key))
                        .label
                }.getOrNull()
            }
        PaydayCelebrationOverlay(
            amountCents = settings.lastSettlementAmountCents
                .takeIf { it > 0 }
                ?: com.salaryflow.app.core.Money.ratioToCents(
                    settings.savingsRate,
                    settings.monthlySalaryCents,
                ),
            periodLabel = periodLabel,
            onDismiss = {
                forceCelebration = false
                viewModel.dismissCelebration()
            },
            onOpenLedger = {
                forceCelebration = false
                viewModel.dismissCelebration()
                navController.navigate("ledger")
            },
        )
    }
}
