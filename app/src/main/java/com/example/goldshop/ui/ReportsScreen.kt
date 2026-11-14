package com.example.goldshop.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.goldshop.data.AppDatabase
import com.example.goldshop.data.isLedger
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val invoices by db.invoiceDao().all().collectAsState(initial = emptyList())
    val oldGold by db.oldGoldDao().all().collectAsState(initial = emptyList())
    val txns by db.transactionDao().all().collectAsState(initial = emptyList())

    var rangeMode by remember { mutableIntStateOf(0) }
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }

    val now = System.currentTimeMillis()
    val start: Long
    val end: Long
    when (rangeMode) {
        0 -> { start = startOfDay(now); end = Long.MAX_VALUE }
        1 -> {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            start = startOfDay(cal.timeInMillis)
            end = Long.MAX_VALUE
        }
        2 -> { start = 0L; end = Long.MAX_VALUE }
        else -> {
            start = parseDate(from) ?: 0L
            end = (parseDate(to) ?: Long.MAX_VALUE) + 86399999L
        }
    }

    val fInv = invoices.filter { it.dateMillis in start..end }
    val fOld = oldGold.filter { it.dateMillis in start..end }
    val fTxn = txns.filter { it.dateMillis in start..end }

    val salesCount = fInv.size
    val salesTotal = fInv.sumOf { it.total }
    val feeTotal = fInv.sumOf { it.feeTotal }
    val taxTotal = fInv.sumOf { it.taxTotal }
    val oldGoldTotal = fOld.sumOf { it.total }
    val oldGoldCount = fOld.size
    // تراکنش‌های ناشی از وصول چک/دریافت طلب در سود دوباره شمرده نمی‌شوند
    val opTxn = fTxn.filter { !it.isLedger() }
    val incomeSum = opTxn.filter { it.type == "income" }.sumOf { it.amount }
    val expenseSum = opTxn.filter { it.type == "expense" }.sumOf { it.amount }
    val profit = feeTotal + incomeSum - expenseSum

    val dailySales = fInv
        .groupBy { dayKey(it.dateMillis) }
        .map { (day, list) -> day to list.sumOf { it.total } }
        .sortedByDescending { it.first }
        .take(31)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            FilterChip(selected = rangeMode == 0, onClick = { rangeMode = 0 }, label = { Text("امروز") })
            FilterChip(selected = rangeMode == 1, onClick = { rangeMode = 1 }, label = { Text("این ماه") })
            FilterChip(selected = rangeMode == 2, onClick = { rangeMode = 2 }, label = { Text("همه") })
            FilterChip(selected = rangeMode == 3, onClick = { rangeMode = 3 }, label = { Text("بازه دلخواه") })
        }

        if (rangeMode == 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = from, onValueChange = { from = it },
                    label = { Text("از تاریخ (1403/01/01)") },
                    modifier = Modifier.weight(1f), singleLine = true
                )
                OutlinedTextField(
                    value = to, onValueChange = { to = it },
                    label = { Text("تا تاریخ") },
                    modifier = Modifier.weight(1f), singleLine = true
                )
            }
            Text("فرمت تاریخ: سال/ماه/روز", style = MaterialTheme.typography.bodySmall)
        }

        LuxCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("فروش", fontWeight = FontWeight.Bold)
                Text("تعداد فاکتور: " + salesCount)
                Text("جمع فروش: " + formatNum(salesTotal) + " تومان")
                Text("جمع اجرت ساخت: " + formatNum(feeTotal) + " تومان")
                Text("جمع مالیات: " + formatNum(taxTotal) + " تومان")
                HorizontalDivider()
                Text("خرید کهنه", fontWeight = FontWeight.Bold)
                Text("تعداد خرید: " + oldGoldCount + " | جمع: " + formatNum(oldGoldTotal) + " تومان")
                HorizontalDivider()
                Text("صندوق", fontWeight = FontWeight.Bold)
                Text("درآمد: " + formatNum(incomeSum) + " | هزینه: " + formatNum(expenseSum))
                HorizontalDivider()
                Text(
                    "سود تقریبی (اجرت + درآمد - هزینه): " + formatNum(profit) + " تومان",
                    fontWeight = FontWeight.Bold,
                    color = if (profit >= 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            }
        }

        if (dailySales.isNotEmpty()) {
            LuxCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("فروش روزانه (تا ۳۱ روز)", fontWeight = FontWeight.Bold)
                    dailySales.forEach { (day, total) ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(day, style = MaterialTheme.typography.bodySmall)
                            Text(
                                formatNum(total) + " ت",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
        ChartsSection(invoices, txns)
        MonthlyReportSection()
        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun startOfDay(millis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = millis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun parseDate(s: String): Long? = runCatching {
    SimpleDateFormat("yyyy/MM/dd", Locale.US).apply { isLenient = false }.parse(s)?.time
}.getOrNull()

private fun dayKey(millis: Long): String =
    SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(millis))
