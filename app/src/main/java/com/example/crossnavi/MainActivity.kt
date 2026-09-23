package com.example.crossnavi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crossnavi.data.FavoritesRepository
import com.example.crossnavi.data.HoldingsRepository
import com.example.crossnavi.data.StockMasterRepository
import com.example.crossnavi.theme.CrossNaviTheme
import com.example.crossnavi.ui.calculator.CalculatorViewModel
import com.example.crossnavi.ui.calculator.CostCalculatorSheet
import com.example.crossnavi.ui.calendar.CalendarScreen
import com.example.crossnavi.ui.favorites.FavoritesScreen
import com.example.crossnavi.ui.favorites.FavoritesViewModel
import com.example.crossnavi.ui.holdings.HoldingsScreen
import com.example.crossnavi.ui.holdings.HoldingsViewModel
import com.example.crossnavi.ui.search.StockSearchScreen

enum class NavigationTab(val title: String) {
    SEARCH("銘柄検索"),
    FAVORITES("お気に入り"),
    HOLDINGS("保有管理"),
    CALENDAR("カレンダー")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrossNaviTheme {
                CrossNaviApp()
            }
        }
    }
}

@Composable
fun CrossNaviApp() {
    val context = LocalContext.current
    val repository = remember { HoldingsRepository(context.applicationContext) }
    val stockMasterRepository = remember { StockMasterRepository(context.applicationContext) }
    val favoritesRepository = remember { FavoritesRepository(context.applicationContext) }

    val calculatorViewModel = remember { CalculatorViewModel(repository, stockMasterRepository, favoritesRepository) }
    val holdingsViewModel = remember { HoldingsViewModel(repository) }
    val favoritesViewModel = remember { FavoritesViewModel(favoritesRepository, stockMasterRepository) }

    val calculatorUiState by calculatorViewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(NavigationTab.SEARCH) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                calculatorViewModel.onSyncInventory(force = false)
                favoritesViewModel.onSyncInventory(force = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // コスト試算シート（銘柄検索やお気に入りから銘柄をタップした時に展開）
    if (calculatorUiState.isCalculationSheetOpen) {
        CostCalculatorSheet(
            viewModel = calculatorViewModel,
            onDismissRequest = calculatorViewModel::onCloseCalculationSheet
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.SEARCH,
                    onClick = { selectedTab = NavigationTab.SEARCH },
                    icon = { Icon(Icons.Default.Search, contentDescription = "銘柄検索") },
                    label = { Text(NavigationTab.SEARCH.title) }
                )
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.FAVORITES,
                    onClick = { selectedTab = NavigationTab.FAVORITES },
                    icon = { Icon(Icons.Default.Star, contentDescription = "お気に入り") },
                    label = { Text(NavigationTab.FAVORITES.title) }
                )
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.HOLDINGS,
                    onClick = { selectedTab = NavigationTab.HOLDINGS },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "保有管理") },
                    label = { Text(NavigationTab.HOLDINGS.title) }
                )
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.CALENDAR,
                    onClick = { selectedTab = NavigationTab.CALENDAR },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "カレンダー") },
                    label = { Text(NavigationTab.CALENDAR.title) }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            NavigationTab.SEARCH -> {
                StockSearchScreen(
                    viewModel = calculatorViewModel,
                    onStockSelected = { stock ->
                        calculatorViewModel.onOpenCalculationForStock(stock)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavigationTab.FAVORITES -> {
                FavoritesScreen(
                    viewModel = favoritesViewModel,
                    onNavigateToCalculate = { stock ->
                        calculatorViewModel.onOpenCalculationForStock(stock)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavigationTab.HOLDINGS -> {
                HoldingsScreen(
                    viewModel = holdingsViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavigationTab.CALENDAR -> {
                CalendarScreen(
                    onNavigateToMonth = { month ->
                        calculatorViewModel.onMonthSelected(month)
                        selectedTab = NavigationTab.SEARCH
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
