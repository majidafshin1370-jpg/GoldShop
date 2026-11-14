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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.goldshop.Audit
import com.example.goldshop.CustomerStore
import com.example.goldshop.data.AppDatabase
import com.example.goldshop.data.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val txns by db.transactionDao().all().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var isIncome by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val incomeSum = txns.filter { it.type == "income" }.sumOf { it.amount }
    val expenseSum = txns.filter { it.type == "expense" }.sumOf { it.amount }
    val balance = incomeSum - expenseSum

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LuxCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("موجودی صندوق", style = MaterialTheme.typography.bodySmall)
                    Text(
                        formatNum(balance) + " تومان",
                        fontWeight = FontWeight.Bold,
                        color = if (balance >= 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "درآمد: " + formatNum(incomeSum),
                        color = Lux.Positive,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "هزینه: " + formatNum(expenseSum),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = isIncome,
                onClick = { isIncome = true },
                label = { Text("درآمد") }
            )
            FilterChip(
                selected = !isIncome,
                onClick = { isIncome = false },
                label = { Text("هزینه") }
            )
        }
        OutlinedTextField(
            value = category, onValueChange = { category = it },
            label = { Text(if (isIncome) "عنوان درآمد" else "عنوان هزینه (مثلاً اجاره)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = amount, onValueChange = { amount = it },
            label = { Text("مبلغ (تومان)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = note, onValueChange = { note = it },
            label = { Text("توضیح (اختیاری)") },
            modifier = Modifier.fillMaxWidth()
        )
        TextButton(
            onClick = {
                scope.launch {
                    db.transactionDao().insert(
                        TransactionEntity(
                            type = if (isIncome) "income" else "expense",
                            category = category,
                            amount = amount.toLongOrNull() ?: 0L,
                            note = note,
                            dateMillis = System.currentTimeMillis()
                        )
                    )
                    Audit.log(
                        context, "صندوق", if (isIncome) "ثبت درآمد" else "ثبت هزینه",
                        category + " — " + (amount.toLongOrNull() ?: 0L)
                    )
                }
                category = ""; amount = ""; note = ""
            },
            enabled = category.isNotBlank() && (amount.toLongOrNull() ?: 0L) > 0L,
            modifier = Modifier.fillMaxWidth()
        ) { Text("ثبت تراکنش") }

        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(txns, key = { it.id }) { t ->
                LuxCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (t.type == "income") Icons.Default.ArrowUpward
                            else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (t.type == "income") Lux.Positive
                            else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.category, fontWeight = FontWeight.Bold)
                            Text(
                                formatDateShort(t.dateMillis) +
                                    if (t.note.isNotBlank()) " | " + t.note else "",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            (if (t.type == "income") "+ " else "- ") + formatNum(t.amount) + " ت",
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = {
                            scope.launch {
                                db.transactionDao().delete(t)
                                Audit.log(context, "صندوق", "حذف تراکنش", t.category + " — " + t.amount)
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف")
                        }
                    }
                }
            }
        }
    }
}

private fun formatDateShort(millis: Long): String =
    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(millis))
