package com.example.goldshop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.goldshop.GoldPriceHolder
import com.example.goldshop.data.AppDatabase
import com.example.goldshop.data.OldGoldEntity
import kotlinx.coroutines.launch

@Composable
fun OldGoldScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val records by db.oldGoldDao().all().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.size(6.dp))
            Text("ثبت خرید کهنه")
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(records, key = { it.id }) { r ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(r.customerName, fontWeight = FontWeight.Bold)
                            Text(
                                "وزن: " + r.weight + " گرم | عیار: " + r.karat +
                                    " | افت: " + r.lossPercent + "٪ | خالص: " +
                                    String.format("%.2f", r.pureWeight) + " گرم",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (r.phone.isNotBlank()) {
                                Text(r.phone, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text(
                            formatNum(r.total) + " ت",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { scope.launch { db.oldGoldDao().delete(r) } }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف")
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddOldGoldDialog(
            onDismiss = { showAdd = false },
            onSave = { entity ->
                scope.launch { db.oldGoldDao().insert(entity) }
                showAdd = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddOldGoldDialog(onDismiss: () -> Unit, onSave: (OldGoldEntity) -> Unit) {
    val karats = listOf(24, 21, 18)
    var karatIndex by remember { mutableIntStateOf(2) }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var lossPercent by remember { mutableStateOf("0") }
    var gramPriceText by remember {
        mutableStateOf(defaultGramPrice(karats[karatIndex]).toString())
    }

    // با تغییر عیار، قیمت هر گرم به‌روز می‌شود (قابل ویرایش دستی)
    LaunchedEffect(karatIndex) {
        gramPriceText = defaultGramPrice(karats[karatIndex]).toString()
    }

    val w = weight.toDoubleOrNull() ?: 0.0
    val loss = lossPercent.toDoubleOrNull() ?: 0.0
    val gram = gramPriceText.toLongOrNull() ?: 0L
    val pureWeight = w * (1 - loss / 100.0)
    val total = (pureWeight * gram).toLong()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("خرید طلای کهنه") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("نام فروشنده") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it },
                    label = { Text("شماره تماس") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = weight, onValueChange = { weight = it },
                    label = { Text("وزن کل (گرم)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    karats.forEachIndexed { i, k ->
                        FilterChip(
                            selected = karatIndex == i,
                            onClick = { karatIndex = i },
                            label = { Text("عیار " + k) }
                        )
                    }
                }
                OutlinedTextField(
                    value = lossPercent, onValueChange = { lossPercent = it },
                    label = { Text("درصد افت (سنگ، رنگ و...)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = gramPriceText, onValueChange = { gramPriceText = it },
                    label = { Text("قیمت هر گرم (تومان)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider()
                Text("وزن خالص: " + String.format("%.2f", pureWeight) + " گرم")
                Text(
                    "مبلغ پرداختی: " + formatNum(total) + " تومان",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        OldGoldEntity(
                            customerName = name,
                            phone = phone,
                            weight = w,
                            karat = karats[karatIndex],
                            lossPercent = loss,
                            gramPrice = gram,
                            pureWeight = pureWeight,
                            total = total,
                            dateMillis = System.currentTimeMillis()
                        )
                    )
                },
                enabled = name.isNotBlank() && w > 0.0
            ) { Text("ثبت خرید") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

private fun defaultGramPrice(karat: Int): Long {
    val base = GoldPriceHolder.geram18 ?: 45000000L
    return (base * karat / 18.0).toLong()
}
