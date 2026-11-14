package com.example.goldshop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.goldshop.GoldPriceApi
import com.example.goldshop.GoldPriceHolder
import com.example.goldshop.Jalali
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
private fun RowScope.PriceTile(title: String, value: Long?) {
    Column(
        modifier = Modifier
            .weight(1f)
            .background(Lux.Black.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            if (value != null) formatNum(value) else "—",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Lux.GoldLight,
            maxLines = 1
        )
        Text("تومان", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun PricesScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var p18 by remember { mutableStateOf(GoldPriceHolder.geram18) }
    var p24 by remember { mutableStateOf(GoldPriceHolder.geram24) }
    var pM by remember { mutableStateOf(GoldPriceHolder.mithqal) }
    var updated by remember { mutableStateOf(GoldPriceHolder.updatedMillis) }

    var manual by remember { mutableStateOf(GoldPriceHolder.geram18?.toString() ?: "") }
    var showApi by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf("https://api.tgju.org/v1/data/market") }
    var status by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    fun publish() {
        GoldPriceHolder.geram18 = p18
        GoldPriceHolder.geram24 = p24
        GoldPriceHolder.mithqal = pM
        GoldPriceHolder.save(context)
        updated = GoldPriceHolder.updatedMillis
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        LuxCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("قیمت هر گرم طلا", fontWeight = FontWeight.Bold, color = Lux.Gold, fontSize = 17.sp)
                    GemLogo(size = 26.dp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriceTile("۱۸ عیار", p18)
                    PriceTile("۲۴ عیار", p24)
                    PriceTile("مثقال", pM)
                }
                Text(
                    if (updated > 0L)
                        "آخرین به‌روزرسانی: " + Jalali.format(updated) + " ساعت " +
                            SimpleDateFormat("HH:mm", Locale.US).format(Date(updated))
                    else "هنوز قیمتی ثبت نشده است",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LuxCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ثبت دستی قیمت", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = manual,
                    onValueChange = { manual = it },
                    label = { Text("قیمت هر گرم ۱۸ عیار (تومان)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                val v = Jalali.normalizeDigits(manual).replace(",", "").toLongOrNull() ?: 0L
                if (v > 0) {
                    Text(
                        "۲۴ عیار: " + formatNum(v * 24 / 18) + " تومان (محاسبه خودکار)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                GoldButton(
                    onClick = {
                        p18 = v
                        p24 = v * 24 / 18
                        publish()
                        status = "قیمت ثبت شد و در ماشین‌حساب، فاکتور و کهنه استفاده می‌شود"
                    },
                    enabled = v > 0,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("ثبت قیمت", fontWeight = FontWeight.Bold) }
            }
        }

        LuxCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("دریافت خودکار از اینترنت", fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showApi = !showApi }) { Text(if (showApi) "بستن" else "تنظیم") }
                }
                if (showApi) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("آدرس API قیمت‌ها (JSON)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        enabled = !loading,
                        onClick = {
                            scope.launch {
                                loading = true
                                status = "در حال دریافت..."
                                val result = withContext(Dispatchers.IO) { GoldPriceApi.fetch(url) }
                                if (result.isEmpty()) {
                                    status = "دریافت ناموفق بود؛ آدرس یا اینترنت را بررسی کنید یا قیمت را دستی ثبت کنید"
                                } else {
                                    GoldPriceApi.pick(result, "geram18", "gram18", "gold18", "gol18")
                                        ?.let { p18 = it.toLong() }
                                    GoldPriceApi.pick(result, "geram24", "gram24", "gold24")
                                        ?.let { p24 = it.toLong() }
                                    GoldPriceApi.pick(result, "mithqal", "mesghal", "mesqhal")
                                        ?.let { pM = it.toLong() }
                                    publish()
                                    manual = p18?.toString() ?: manual
                                    status = "قیمت‌ها به‌روز شد"
                                }
                                loading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (loading) "در حال دریافت…" else "دریافت قیمت‌ها") }
                    Text(
                        "هر API که JSON برگرداند و کلیدهایی مثل geram18 / geram24 / mithqal داشته باشد پشتیبانی می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (status.isNotBlank()) {
            Text(status, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}
