package com.example.goldshop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.goldshop.GoldPriceApi
import com.example.goldshop.GoldPriceHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PricesScreen() {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("https://api.tgju.org/v1/data/market") }
    var status by remember { mutableStateOf("آدرس API را وارد کنید و دکمه دریافت را بزنید") }
    var prices by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("آدرس API قیمت‌ها (JSON)") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                scope.launch {
                    status = "در حال دریافت..."
                    val result = withContext(Dispatchers.IO) { GoldPriceApi.fetch(url) }
                    prices = result
                    if (result.isEmpty()) {
                        status = "دریافت ناموفق بود؛ آدرس یا اینترنت را بررسی کنید و در صورت نیاز قیمت را دستی وارد کنید"
                    } else {
                        status = "قیمت‌ها به‌روز شد و در ماشین‌حساب اعمال شد"
                        GoldPriceApi.pick(result, "geram18", "gram18", "gold18", "gol18")
                            ?.let { GoldPriceHolder.geram18 = it.toLong() }
                        GoldPriceApi.pick(result, "geram24", "gram24", "gold24")
                            ?.let { GoldPriceHolder.geram24 = it.toLong() }
                        GoldPriceApi.pick(result, "mithqal", "mesghal", "mesqhal")
                            ?.let { GoldPriceHolder.mithqal = it.toLong() }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("دریافت قیمت‌ها") }

        Text(status, style = MaterialTheme.typography.bodySmall)

        if (prices.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("نتیجه دریافت:", fontWeight = FontWeight.Bold)
                    prices.entries.take(12).forEach { (k, v) ->
                        Text(k + " : " + formatNum(v.toLong()))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "نکته: قیمت‌های دریافتی به‌صورت خودکار در ماشین‌حساب استفاده می‌شوند. " +
                "کلیدهای geram18 / geram24 / mithqal شناسایی می‌شوند.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
