package com.example.goldshop.ui

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.goldshop.GoldPriceHolder
import com.example.goldshop.Jalali

// وزن‌های استاندارد (بانک مرکزی): تمام سکه ۸٫۱۳۳ گرم با عیار ۹۰۰؛ مثقال ۴٫۶۰۸۳ گرم
private const val MITHQAL_G = 4.6083
private const val COIN_FINENESS = 0.900
private const val DAY_MS = 86_400_000.0

private data class Coin(val name: String, val weight: Double)

private val COINS = listOf(
    Coin("تمام سکه", 8.133),
    Coin("نیم سکه", 4.0665),
    Coin("ربع سکه", 2.0333),
    Coin("سکه گرمی", 1.0166)
)

private fun num(s: String): Double? =
    Jalali.normalizeDigits(s).replace(",", "").replace("٬", "").trim().toDoubleOrNull()

private fun g3(v: Double): String = String.format("%.3f", v)

// قیمت هر گرم طلای ۲۴ عیار از قیمت‌های لحظه‌ای (اگر دریافت شده باشد)
private fun price24(): Double? =
    GoldPriceHolder.geram24?.toDouble() ?: GoldPriceHolder.geram18?.let { it * 24.0 / 18.0 }

@Composable
fun CoinScreen() {
    var tab by remember { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("تبدیل سکه و مثقال") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("سود سرمایه‌گذاری") })
        }
        if (tab == 0) ConvertTab() else ProfitTab()
    }
}

@Composable
private fun ConvertTab() {
    var unit by remember { mutableIntStateOf(0) } // 0..3 سکه، 4 گرم، 5 مثقال
    var karat by remember { mutableIntStateOf(18) }
    var amount by remember { mutableStateOf("1") }
    val units = COINS.map { it.name } + listOf("گرم طلا", "مثقال")

    val qty = num(amount) ?: 0.0
    val totalG: Double
    val fineness: Double
    if (unit < COINS.size) {
        totalG = COINS[unit].weight * qty
        fineness = COIN_FINENESS
    } else if (unit == COINS.size) {
        totalG = qty
        fineness = karat / 24.0
    } else {
        totalG = qty * MITHQAL_G
        fineness = karat / 24.0
    }
    val pure = totalG * fineness
    val p24 = price24()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            units.forEachIndexed { i, u -> FilterChip(selected = unit == i, onClick = { unit = i }, label = { Text(u) }) }
        }
        if (unit >= COINS.size) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(18, 21, 24).forEach { k ->
                    FilterChip(selected = karat == k, onClick = { karat = k }, label = { Text("عیار $k") })
                }
            }
        }
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text(if (unit < COINS.size) "تعداد" else if (unit == COINS.size) "وزن (گرم)" else "وزن (مثقال)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        LuxCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("نتیجه", fontWeight = FontWeight.Bold)
                Text("وزن کل: " + g3(totalG) + " گرم  =  " + g3(totalG / MITHQAL_G) + " مثقال")
                Text("طلای خالص (۲۴ عیار): " + g3(pure) + " گرم  =  " + g3(pure / MITHQAL_G) + " مثقال")
                Text("معادل ۱۸ عیار: " + g3(pure / 0.75) + " گرم")
                HorizontalDivider()
                Text("معادل خالص بر حسب سکه", fontWeight = FontWeight.Bold)
                COINS.forEach { c ->
                    Text(c.name + ": " + g3(pure / (c.weight * COIN_FINENESS)) + " عدد", style = MaterialTheme.typography.bodySmall)
                }
                if (p24 != null) {
                    HorizontalDivider()
                    Text(
                        "ارزش ذاتی طلا با قیمت لحظه‌ای: " + formatNum(Math.round(pure * p24)) + " تومان",
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
                    )
                    Text("(بدون حباب سکه و بدون اجرت؛ قیمت از صفحه «قیمت‌ها»)", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("برای ارزش ذاتی، یک بار از صفحه «قیمت‌ها» قیمت لحظه‌ای را دریافت کنید.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Text(
            "مثقال = ۴٫۶۰۸۳ گرم | سکه‌ها با عیار ۹۰۰؛ تمام ۸٫۱۳۳ گرم، نیم و ربع و گرمی به نسبت.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfitTab() {
    val context = LocalContext.current
    var coinMode by remember { mutableStateOf(true) }
    var coinIndex by remember { mutableIntStateOf(0) }
    var karat by remember { mutableIntStateOf(18) }
    var qtyText by remember { mutableStateOf("1") }
    var buyText by remember { mutableStateOf("") }
    var curText by remember { mutableStateOf("") }
    var buyDate by remember { mutableStateOf("") }

    val qty = num(qtyText) ?: 0.0
    val buy = num(buyText) ?: 0.0
    val cur = num(curText) ?: 0.0
    val p24 = price24()

    val pureEach: Double = if (coinMode) COINS[coinIndex].weight * COIN_FINENESS else karat / 24.0 // خالص هر واحد
    val intrinsicEach: Double? = p24?.let { it * pureEach }

    val cost = qty * buy
    val value = qty * cur
    val profit = value - cost
    val pct = if (cost > 0) profit * 100.0 / cost else null
    val buyMillis: Long? = if (buyDate.isBlank()) null else Jalali.parse(buyDate)
    val days = if (buyMillis != null) (System.currentTimeMillis() - buyMillis) / DAY_MS else null
    val annual: Double? = if (days != null && days >= 30 && cost > 0 && value > 0) Math.pow(value / cost, 365.0 / days) - 1.0 else null
    val premium: Double? = if (coinMode && intrinsicEach != null && intrinsicEach > 0 && cur > 0) (cur / intrinsicEach - 1.0) * 100.0 else null

    val unitName = if (coinMode) COINS[coinIndex].name else "گرم طلا ($karat عیار)"
    val resultText = buildString {
        append("محاسبه سود سرمایه‌گذاری\n")
        append(unitName + " — مقدار: " + qtyText + "\n")
        append("هزینه خرید: " + formatNum(Math.round(cost)) + " تومان\n")
        append("ارزش فعلی: " + formatNum(Math.round(value)) + " تومان\n")
        append((if (profit >= 0) "سود: " else "زیان: ") + formatNum(Math.round(Math.abs(profit))) + " تومان")
        if (pct != null) append(" (" + String.format("%.1f", pct) + "٪)")
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = coinMode, onClick = { coinMode = true; curText = "" }, label = { Text("سکه") })
            FilterChip(selected = !coinMode, onClick = { coinMode = false; curText = "" }, label = { Text("طلا (گرم)") })
        }
        if (coinMode) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                COINS.forEachIndexed { i, c -> FilterChip(selected = coinIndex == i, onClick = { coinIndex = i }, label = { Text(c.name) }) }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(18, 21, 24).forEach { k -> FilterChip(selected = karat == k, onClick = { karat = k }, label = { Text("عیار $k") }) }
            }
        }
        OutlinedTextField(
            value = qtyText, onValueChange = { qtyText = it },
            label = { Text(if (coinMode) "تعداد سکه" else "وزن (گرم)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = buyText, onValueChange = { buyText = it },
            label = { Text(if (coinMode) "قیمت خرید هر سکه (تومان)" else "قیمت خرید هر گرم (تومان)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = curText, onValueChange = { curText = it },
            label = { Text(if (coinMode) "قیمت فعلی هر سکه (بازار)" else "قیمت فعلی هر گرم") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
        )
        if (intrinsicEach != null) {
            TextButton(onClick = { curText = Math.round(intrinsicEach).toString() }) {
                Text(
                    if (coinMode) "استفاده از ارزش ذاتی هر سکه (" + formatNum(Math.round(intrinsicEach)) + ")"
                    else "استفاده از قیمت لحظه‌ای این عیار (" + formatNum(Math.round(intrinsicEach)) + ")"
                )
            }
        }
        OutlinedTextField(
            value = buyDate, onValueChange = { buyDate = it },
            label = { Text("تاریخ خرید شمسی (1405/01/15) — اختیاری، برای بازده سالانه") }, singleLine = true,
            isError = buyDate.isNotBlank() && buyMillis == null, modifier = Modifier.fillMaxWidth()
        )

        LuxCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("نتیجه", fontWeight = FontWeight.Bold)
                Text("هزینه خرید: " + formatNum(Math.round(cost)) + " تومان")
                Text("ارزش فعلی: " + formatNum(Math.round(value)) + " تومان")
                Text(
                    (if (profit >= 0) "سود: " else "زیان: ") + formatNum(Math.round(Math.abs(profit))) + " تومان" +
                        if (pct != null) "  (" + String.format("%.1f", pct) + "٪)" else "",
                    fontWeight = FontWeight.Bold,
                    color = if (profit >= 0) Lux.Positive else MaterialTheme.colorScheme.error
                )
                if (days != null) {
                    Text("مدت نگهداری: " + days.toInt() + " روز", style = MaterialTheme.typography.bodySmall)
                    Text(
                        if (annual != null) "بازده سالانه (مرکب): " + String.format("%.1f", annual * 100) + "٪"
                        else "کمتر از ۳۰ روز؛ بازده سالانه محاسبه نمی‌شود",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (premium != null) {
                    Text(
                        "اختلاف قیمت بازار با ارزش ذاتی (حباب): " + String.format("%.1f", premium) + "٪",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        TextButton(onClick = {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, resultText)
            }
            context.startActivity(Intent.createChooser(send, "ارسال نتیجه"))
        }) { Text("اشتراک‌گذاری نتیجه") }
        Text(
            "این محاسبه فقط یک برآورد ریاضی است و پیش‌بینی یا توصیه سرمایه‌گذاری نیست.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
