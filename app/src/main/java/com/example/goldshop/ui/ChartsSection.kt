package com.example.goldshop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.goldshop.Jalali
import com.example.goldshop.data.InvoiceEntity
import com.example.goldshop.data.TransactionEntity
import com.example.goldshop.data.isLedger
import java.util.Calendar

private const val DAY = 86_400_000L

private class Series(val labels: List<String>, val sales: LongArray, val profit: LongArray)

private fun startOfDay(millis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = millis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

// سود تقریبی همان تعریف صفحه گزارش: اجرت + درآمد − هزینه (تراکنش‌های «حساب:» حذف می‌شوند)
private fun profitDelta(t: TransactionEntity): Long = if (t.type == "income") t.amount else -t.amount

private fun dailySeries(invoices: List<InvoiceEntity>, txns: List<TransactionEntity>): Series {
    val n = 30
    val first = startOfDay(System.currentTimeMillis()) - (n - 1) * DAY
    val sales = LongArray(n)
    val profit = LongArray(n)
    for (inv in invoices) {
        if (inv.dateMillis < first) continue
        val i = ((inv.dateMillis - first) / DAY).toInt()
        if (i in 0 until n) { sales[i] += inv.total; profit[i] += inv.feeTotal }
    }
    for (t in txns) {
        if (t.dateMillis < first) continue
        val i = ((t.dateMillis - first) / DAY).toInt()
        if (i in 0 until n) profit[i] += profitDelta(t)
    }
    val labels = (0 until n).map { i ->
        if (i % 3 == 0 || i == n - 1) Jalali.parts(first + i * DAY + DAY / 2).d.toString() else ""
    }
    return Series(labels, sales, profit)
}

private fun monthlySeries(invoices: List<InvoiceEntity>, txns: List<TransactionEntity>): Series {
    val n = 12
    val t = Jalali.today()
    val months = ArrayList<Pair<Int, Int>>()
    var y = t.y
    var m = t.m
    repeat(n) {
        months.add(Pair(y, m))
        val p = Jalali.previousMonth(y, m)
        y = p.first
        m = p.second
    }
    months.reverse()
    val index = HashMap<Int, Int>()
    months.forEachIndexed { i, ym -> index[ym.first * 100 + ym.second] = i }
    val sales = LongArray(n)
    val profit = LongArray(n)
    val limit = Jalali.monthStart(months.first().first, months.first().second)
    for (inv in invoices) {
        if (inv.dateMillis < limit) continue
        val p = Jalali.parts(inv.dateMillis)
        val i = index[p.y * 100 + p.m] ?: continue
        sales[i] += inv.total; profit[i] += inv.feeTotal
    }
    for (tx in txns) {
        if (tx.dateMillis < limit) continue
        val p = Jalali.parts(tx.dateMillis)
        val i = index[p.y * 100 + p.m] ?: continue
        profit[i] += profitDelta(tx)
    }
    return Series(months.map { Jalali.monthNames[it.second - 1] }, sales, profit)
}

private class Agg(val count: Int, val sales: Long, val profit: Long)

private fun aggregate(invoices: List<InvoiceEntity>, txns: List<TransactionEntity>, from: Long, to: Long): Agg {
    val inv = invoices.filter { it.dateMillis in from..to }
    val tx = txns.filter { it.dateMillis in from..to }
    return Agg(inv.size, inv.sumOf { it.total }, inv.sumOf { it.feeTotal } + tx.sumOf { profitDelta(it) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsSection(invoices: List<InvoiceEntity>, allTxns: List<TransactionEntity>) {
    var mode by remember { mutableIntStateOf(0) }
    val txns = allTxns.filter { !it.isLedger() }

    val series = if (mode == 0) dailySeries(invoices, txns) else monthlySeries(invoices, txns)

    // مقایسه «این ماه تا امروز» با «ماه قبل تا همان روز»
    val now = System.currentTimeMillis()
    val today = Jalali.today()
    val thisStart = Jalali.monthStart(today.y, today.m)
    val prev = Jalali.previousMonth(today.y, today.m)
    val prevStart = Jalali.monthStart(prev.first, prev.second)
    val prevEnd = minOf(prevStart + (now - thisStart), Jalali.monthEnd(prev.first, prev.second))
    val cur = aggregate(invoices, txns, thisStart, now)
    val old = aggregate(invoices, txns, prevStart, prevEnd)

    LuxCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("نمودار فروش و سود", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("روزانه (۳۰ روز)") })
                FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("ماهانه (۱۲ ماه)") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(Lux.Gold))
                Text("فروش", style = MaterialTheme.typography.bodySmall)
                Box(Modifier.size(10.dp).background(Lux.Positive))
                Text("سود تقریبی", style = MaterialTheme.typography.bodySmall)
            }
            BarPair(series)
            if (mode == 1) {
                HorizontalDivider()
                for (i in series.labels.indices.reversed()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(series.labels[i], style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Text(
                            "فروش " + formatNum(series.sales[i]) + " | سود " + formatNum(series.profit[i]),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    LuxCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("مقایسه با ماه قبل (تا همین روز از ماه)", fontWeight = FontWeight.Bold)
            CompareRow("جمع فروش", cur.sales, old.sales)
            CompareRow("سود تقریبی", cur.profit, old.profit)
            CompareRow("تعداد فاکتور", cur.count.toLong(), old.count.toLong(), money = false)
        }
    }
}

@Composable
private fun CompareRow(label: String, cur: Long, old: Long, money: Boolean = true) {
    val diff = cur - old
    val pct = if (old != 0L) (diff * 100.0 / Math.abs(old)) else null
    val up = diff >= 0
    val color = if (diff == 0L) MaterialTheme.colorScheme.onSurface else if (up) Lux.Positive else MaterialTheme.colorScheme.error
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(
                (if (diff == 0L) "" else if (up) "▲ " else "▼ ") +
                    (if (pct != null) String.format("%.1f", Math.abs(pct)) + "٪" else "—"),
                color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            "این ماه: " + (if (money) formatNum(cur) + " ت" else cur.toString()) +
                " | ماه قبل: " + (if (money) formatNum(old) + " ت" else old.toString()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BarPair(series: Series) {
    val maxV = maxOf(
        series.sales.maxOrNull() ?: 0L,
        series.profit.maxOrNull() ?: 0L,
        1L
    )
    fun frac(v: Long): Float = (v.coerceAtLeast(0L).toFloat() / maxV.toFloat()).coerceIn(0f, 1f)

    Row(
        modifier = Modifier.fillMaxWidth().height(140.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in series.sales.indices) {
            Row(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Box(Modifier.weight(1f).fillMaxHeight(frac(series.sales[i])).background(Lux.Gold))
                Box(Modifier.weight(1f).fillMaxHeight(frac(series.profit[i])).background(Lux.Positive))
            }
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in series.labels.indices) {
            val label = series.labels[i]
            Text(
                if (series.labels.size > 12) label else label.take(3),
                fontSize = 8.sp,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.weight(1f),
                color = Color.Gray
            )
        }
    }
}
