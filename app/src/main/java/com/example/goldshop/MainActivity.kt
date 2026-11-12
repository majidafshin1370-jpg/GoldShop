package com.example.goldshop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.goldshop.ui.CalculatorScreen
import com.example.goldshop.ui.CashScreen
import com.example.goldshop.ui.InvoicesScreen
import com.example.goldshop.ui.LedgerScreen
import com.example.goldshop.ui.LockScreen
import com.example.goldshop.ui.OldGoldScreen
import com.example.goldshop.ui.PricesScreen
import com.example.goldshop.ui.ProductsScreen
import com.example.goldshop.ui.ReportsScreen
import com.example.goldshop.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // زمان‌بندی پشتیبان ابری و گزارش ماهانه بر اساس تنظیمات ذخیره‌شده
        AutoScheduler.apply(applicationContext)
        setContent {
            val context = LocalContext.current
            var unlocked by remember { mutableStateOf(!PinManager.hasPin(context)) }
            MaterialTheme {
                if (!unlocked) {
                    LockScreen(onSuccess = { unlocked = true })
                } else {
                    App()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val navController = rememberNavController()
    var selected by remember { mutableStateOf(0) }
    var title by remember { mutableStateOf("ماشین‌حساب طلا") }

    fun go(index: Int, newTitle: String, route: String) {
        selected = index
        title = newTitle
        navController.navigate(route) { launchSingleTop = true }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        selected = -1
                        title = "چک و طلب/بدهی"
                        navController.navigate("ledger") { launchSingleTop = true }
                    }) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = "چک و طلب")
                    }
                    IconButton(onClick = {
                        selected = -1
                        title = "تنظیمات"
                        navController.navigate("settings")
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "تنظیمات")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selected == 0,
                    onClick = { go(0, "ماشین‌حساب طلا", "calc") },
                    icon = { Icon(Icons.Default.Calculate, contentDescription = null) },
                    label = { Text("ماشین‌حساب") }
                )
                NavigationBarItem(
                    selected = selected == 1,
                    onClick = { go(1, "قیمت لحظه‌ای", "prices") },
                    icon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                    label = { Text("قیمت‌ها") }
                )
                NavigationBarItem(
                    selected = selected == 2,
                    onClick = { go(2, "محصولات", "products") },
                    icon = { Icon(Icons.Default.Inventory, contentDescription = null) },
                    label = { Text("محصولات") }
                )
                NavigationBarItem(
                    selected = selected == 3,
                    onClick = { go(3, "خرید طلای کهنه", "oldgold") },
                    icon = { Icon(Icons.Default.Recycling, contentDescription = null) },
                    label = { Text("کهنه") }
                )
                NavigationBarItem(
                    selected = selected == 4,
                    onClick = { go(4, "فاکتورها", "invoices") },
                    icon = { Icon(Icons.Default.Receipt, contentDescription = null) },
                    label = { Text("فاکتور") }
                )
                NavigationBarItem(
                    selected = selected == 5,
                    onClick = { go(5, "صندوق (هزینه و درآمد)", "cash") },
                    icon = { Icon(Icons.Default.PointOfSale, contentDescription = null) },
                    label = { Text("صندوق") }
                )
                NavigationBarItem(
                    selected = selected == 6,
                    onClick = { go(6, "گزارش فروش", "reports") },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                    label = { Text("گزارش") }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "calc",
            modifier = Modifier.padding(padding)
        ) {
            composable("calc") { CalculatorScreen() }
            composable("prices") { PricesScreen() }
            composable("products") { ProductsScreen() }
            composable("oldgold") { OldGoldScreen() }
            composable("invoices") { InvoicesScreen() }
            composable("cash") { CashScreen() }
            composable("reports") { ReportsScreen() }
            composable("ledger") { LedgerScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}
