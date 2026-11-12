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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.goldshop.GoldPriceHolder
import java.text.DecimalFormat

fun formatNum(n: Long): String = DecimalFormat("#,###").format(n)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen() {
    val karats = listOf(24, 21, 18)
    var karat by remember { mutableStateOf(18) }
    var base18 by remember { mutableStateOf(GoldPriceHolder.geram18?.toString() ?: "45000000") }
    var weight by remember { mutableStateOf("") }
    var feePercentMode by remember { mutableStateOf(true) }
    var feePercent by remember { mutableStateOf("7") }
    var feeToman by remember { mutableStateOf("") }
    var taxPercent by remember { mutableStateOf("9") }

    val base = base18.toLongOrNull() ?: 0L
    val w = weight.toDoubleOrNull() ?: 0.0
    val gramPrice = base * karat / 18.0
    val subtotal = (w * gramPrice).toLong()
    val fee = if (feePercentMode) {
        (subtotal * (feePercent.toDoubleOrNull() ?: 0.0) / 100).toLong()
    } else {
        (w * (feeToman.toLongOrNull() ?: 0L)).toLong()
    }
    val tax = ((subtotal + fee) * (taxPercent.toDoubleOrNull() ?: 0.0) / 100).toLong()
    val total = subtotal + fee + tax

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
            karats.forEach { k ->
                FilterChip(
                    selected = karat == k,
                    onClick = { karat = k },
                    label = { Text("عیار $k") }
                )
            }
        }

        OutlinedTextField(
            value = base18,
            onValueChange = { base18 = it },
            label = { Text("قیمت هر گرم ۱۸ عیار (تومان)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it },
            label = { Text("وزن طلا (گرم)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = feePercentMode,
                onClick = { feePercentMode = true },
                label = { Text("اجرت درصد") }
            )
            FilterChip(
                selected = !feePercentMode,
                onClick = { feePercentMode = false },
                label = { Text("اجرت تومان/گرم") }
            )
        }

        if (feePercentMode) {
            OutlinedTextField(
                value = feePercent,
                onValueChange = { feePercent = it },
                label = { Text("اجرت ساخت (٪)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            OutlinedTextField(
                value = feeToman,
                onValueChange = { feeToman = it },
                label = { Text("اجرت (تومان به ازای هر گرم)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            value = taxPercent,
            onValueChange = { taxPercent = it },
            label = { Text("مالیات بر ارزش افزوده (٪)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("قیمت هر گرم $karat عیار: " + formatNum(gramPrice.toLong()) + " تومان")
                HorizontalDivider()
                Text("جمع اقلام: " + formatNum(subtotal) + " تومان")
                Text("اجرت ساخت: " + formatNum(fee) + " تومان")
                Text("مالیات: " + formatNum(tax) + " تومان")
                HorizontalDivider()
                Text(
                    "مبلغ نهایی: " + formatNum(total) + " تومان",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
