package com.example.goldshop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.example.goldshop.Jalali
import com.example.goldshop.data.AppDatabase
import com.example.goldshop.data.ChequeEntity
import com.example.goldshop.data.DebtEntity
import com.example.goldshop.data.DebtPaymentEntity
import com.example.goldshop.data.LEDGER_PREFIX
import com.example.goldshop.data.TransactionEntity
import kotlinx.coroutines.launch

private val Green = Color(0xFF2E7D32)
private const val DAY = 86_400_000L

@Composable
fun LedgerScreen() {
    var tab by remember { mutableStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("چک‌ها") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("طلب و بدهی مشتریان") })
        }
        if (tab == 0) ChequesTab() else DebtsTab()
    }
}

// ───────────────────────────── چک‌ها ─────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChequesTab() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val cheques by db.chequeDao().all().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var filter by remember { mutableStateOf("pending") }
    var showAdd by remember { mutableStateOf(false) }
    var cashTarget by remember { mutableStateOf<ChequeEntity?>(null) }

    val now = System.currentTimeMillis()
    val pending = cheques.filter { it.status == "pending" }
    val recvPending = pending.filter { it.direction == "receivable" }.sumOf { it.amount }
    val payPending = pending.filter { it.direction == "payable" }.sumOf { it.amount }
    val overdue = pending.count { it.dueMillis < now }
    val dueSoon = pending.count { it.dueMillis in now..(now + 7 * DAY) }

    val shown = cheques.filter { filter == "all" || it.status == filter }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("چک‌های دریافتی در انتظار: " + formatNum(recvPending) + " تومان", color = Green, fontWeight = FontWeight.Bold)
                Text("چک‌های پرداختی در انتظار: " + formatNum(payPending) + " تومان", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                Text(
                    "سررسید گذشته: $overdue | سررسید تا ۷ روز آینده: $dueSoon",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip(selected = filter == "pending", onClick = { filter = "pending" }, label = { Text("در انتظار") })
            FilterChip(selected = filter == "cashed", onClick = { filter = "cashed" }, label = { Text("وصول/پاس شده") })
            FilterChip(selected = filter == "bounced", onClick = { filter = "bounced" }, label = { Text("برگشتی") })
            FilterChip(selected = filter == "all", onClick = { filter = "all" }, label = { Text("همه") })
        }

        Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) { Text("ثبت چک جدید") }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(shown, key = { it.id }) { c ->
                val isRecv = c.direction == "receivable"
                val late = c.status == "pending" && c.dueMillis < now
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                (if (isRecv) "دریافتی از " else "پرداختی به ") + c.partyName,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                formatNum(c.amount) + " ت",
                                fontWeight = FontWeight.Bold,
                                color = if (isRecv) Green else MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            "سررسید: " + Jalali.format(c.dueMillis) + statusText(c.status) + if (late) " (سررسید گذشته!)" else "",
                            color = if (late) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodySmall
                        )
                        val meta = listOf(
                            if (c.chequeNumber.isNotBlank()) "شماره: " + c.chequeNumber else "",
                            c.bank,
                            c.phone,
                            c.note
                        ).filter { it.isNotBlank() }.joinToString(" | ")
                        if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall)
                        Row {
                            if (c.status == "pending") {
                                TextButton(onClick = { cashTarget = c }) { Text(if (isRecv) "وصول شد" else "پاس شد") }
                                TextButton(onClick = {
                                    scope.launch { db.chequeDao().update(c.copy(status = "bounced", settledMillis = System.currentTimeMillis())) }
                                }) { Text("برگشت خورد") }
                            } else {
                                TextButton(onClick = {
                                    scope.launch { db.chequeDao().update(c.copy(status = "pending", settledMillis = 0L)) }
                                }) { Text("بازگشت به در انتظار") }
                            }
                            TextButton(onClick = { scope.launch { db.chequeDao().delete(c) } }) { Text("حذف") }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddChequeDialog(
            onDismiss = { showAdd = false },
            onSave = { c ->
                scope.launch { db.chequeDao().insert(c) }
                showAdd = false
            }
        )
    }

    cashTarget?.let { c ->
        val isRecv = c.direction == "receivable"
        AlertDialog(
            onDismissRequest = { cashTarget = null },
            title = { Text(if (isRecv) "وصول چک" else "پاس شدن چک") },
            text = { Text("مبلغ " + formatNum(c.amount) + " تومان در صندوق هم ثبت شود؟") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        db.chequeDao().update(c.copy(status = "cashed", settledMillis = System.currentTimeMillis()))
                        db.transactionDao().insert(
                            TransactionEntity(
                                type = if (isRecv) "income" else "expense",
                                category = LEDGER_PREFIX + (if (isRecv) "وصول چک " else "پرداخت چک ") + c.partyName,
                                amount = c.amount,
                                note = if (c.chequeNumber.isNotBlank()) "چک شماره " + c.chequeNumber else "",
                                dateMillis = System.currentTimeMillis()
                            )
                        )
                    }
                    cashTarget = null
                }) { Text("بله، ثبت در صندوق") }
            },
            dismissButton = {
                TextButton(onClick = {
                    scope.launch { db.chequeDao().update(c.copy(status = "cashed", settledMillis = System.currentTimeMillis())) }
                    cashTarget = null
                }) { Text("خیر، فقط وضعیت") }
            }
        )
    }
}

private fun statusText(s: String) = when (s) {
    "cashed" -> " | وصول/پاس شده"
    "bounced" -> " | برگشتی"
    else -> " | در انتظار"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddChequeDialog(onDismiss: () -> Unit, onSave: (ChequeEntity) -> Unit) {
    var receivable by remember { mutableStateOf(true) }
    var party by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var due by remember { mutableStateOf(Jalali.format(System.currentTimeMillis() + 30 * DAY)) }
    var note by remember { mutableStateOf("") }

    val dueMillis = Jalali.parse(due)
    val amountVal = Jalali.normalizeDigits(amount).toLongOrNull() ?: 0L
    val valid = party.isNotBlank() && amountVal > 0 && dueMillis != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت چک") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.verticalScrollCompat()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = receivable, onClick = { receivable = true }, label = { Text("دریافتی (طلب)") })
                    FilterChip(selected = !receivable, onClick = { receivable = false }, label = { Text("پرداختی") })
                }
                OutlinedTextField(party, { party = it }, label = { Text(if (receivable) "نام صادرکننده" else "نام گیرنده") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(amount, { amount = it }, label = { Text("مبلغ (تومان)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(due, { due = it }, label = { Text("تاریخ سررسید شمسی (1405/08/21)") }, singleLine = true,
                    isError = dueMillis == null, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(number, { number = it }, label = { Text("شماره چک") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(bank, { bank = it }, label = { Text("بانک") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(phone, { phone = it }, label = { Text("تلفن") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(note, { note = it }, label = { Text("توضیح") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(
                    ChequeEntity(
                        direction = if (receivable) "receivable" else "payable",
                        partyName = party.trim(), phone = phone.trim(),
                        chequeNumber = number.trim(), bank = bank.trim(),
                        amount = amountVal, dueMillis = dueMillis!!,
                        note = note.trim(), createdMillis = System.currentTimeMillis()
                    )
                )
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

// ─────────────────────── طلب و بدهی مشتریان ───────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtsTab() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val debts by db.debtDao().all().collectAsState(initial = emptyList())
    val payments by db.debtDao().allPayments().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var dir by remember { mutableStateOf("receivable") }
    var showOpen by remember { mutableStateOf(true) }
    var showAdd by remember { mutableStateOf(false) }
    var payTarget by remember { mutableStateOf<DebtEntity?>(null) }

    val paidBy = payments.groupBy { it.debtId }.mapValues { (_, l) -> l.sumOf { it.amount } }
    fun remaining(d: DebtEntity) = (d.amount - (paidBy[d.id] ?: 0L)).coerceAtLeast(0L)

    val recvTotal = debts.filter { it.direction == "receivable" }.sumOf { remaining(it) }
    val payTotal = debts.filter { it.direction == "payable" }.sumOf { remaining(it) }

    val shown = debts.filter { it.direction == dir && (!showOpen || remaining(it) > 0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("جمع طلب از مشتریان: " + formatNum(recvTotal) + " تومان", color = Green, fontWeight = FontWeight.Bold)
                Text("جمع بدهی به مشتریان: " + formatNum(payTotal) + " تومان", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip(selected = dir == "receivable", onClick = { dir = "receivable" }, label = { Text("طلب من (بدهکاران)") })
            FilterChip(selected = dir == "payable", onClick = { dir = "payable" }, label = { Text("بدهی من") })
            FilterChip(selected = showOpen, onClick = { showOpen = !showOpen }, label = { Text("فقط تسویه‌نشده") })
        }
        Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) { Text("ثبت طلب / بدهی جدید") }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(shown, key = { it.id }) { d ->
                val paid = paidBy[d.id] ?: 0L
                val rem = remaining(d)
                val isRecv = d.direction == "receivable"
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(d.partyName, fontWeight = FontWeight.Bold)
                            Text(Jalali.format(d.dateMillis), style = MaterialTheme.typography.bodySmall)
                        }
                        Text("مبلغ کل: " + formatNum(d.amount) + " | " + (if (isRecv) "دریافت‌شده: " else "پرداخت‌شده: ") + formatNum(paid))
                        Text(
                            if (rem == 0L) "تسویه شد ✓" else "مانده: " + formatNum(rem) + " تومان",
                            fontWeight = FontWeight.Bold,
                            color = if (rem == 0L) Green else if (isRecv) Green else MaterialTheme.colorScheme.error
                        )
                        LinearProgressIndicator(
                            progress = { if (d.amount > 0) (paid.toFloat() / d.amount).coerceIn(0f, 1f) else 1f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        val meta = listOf(d.phone, d.note).filter { it.isNotBlank() }.joinToString(" | ")
                        if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall)
                        Row {
                            if (rem > 0) {
                                TextButton(onClick = { payTarget = d }) { Text(if (isRecv) "دریافت قسط" else "پرداخت قسط") }
                            }
                            TextButton(onClick = {
                                scope.launch {
                                    db.debtDao().deletePayments(d.id)
                                    db.debtDao().delete(d)
                                }
                            }) { Text("حذف") }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddDebtDialog(
            initialReceivable = dir == "receivable",
            onDismiss = { showAdd = false },
            onSave = { d ->
                scope.launch { db.debtDao().insert(d) }
                dir = d.direction
                showAdd = false
            }
        )
    }

    payTarget?.let { d ->
        val rem = remaining(d)
        val isRecv = d.direction == "receivable"
        var amount by remember(d.id) { mutableStateOf(rem.toString()) }
        var note by remember(d.id) { mutableStateOf("") }
        var toCash by remember(d.id) { mutableStateOf(true) }
        val amt = Jalali.normalizeDigits(amount).toLongOrNull() ?: 0L
        AlertDialog(
            onDismissRequest = { payTarget = null },
            title = { Text((if (isRecv) "دریافت از " else "پرداخت به ") + d.partyName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("مانده: " + formatNum(rem) + " تومان")
                    OutlinedTextField(amount, { amount = it }, label = { Text("مبلغ (تومان)") }, singleLine = true,
                        isError = amt <= 0 || amt > rem,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(note, { note = it }, label = { Text("توضیح (مثلاً نقد/کارت)") }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = toCash, onCheckedChange = { toCash = it })
                        Text("ثبت در صندوق")
                    }
                    Text("برای تسویه کامل، مبلغ را برابر مانده بگذارید.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(enabled = amt in 1..rem, onClick = {
                    scope.launch {
                        db.debtDao().insertPayment(
                            DebtPaymentEntity(debtId = d.id, amount = amt, note = note.trim(), dateMillis = System.currentTimeMillis())
                        )
                        if (toCash) {
                            db.transactionDao().insert(
                                TransactionEntity(
                                    type = if (isRecv) "income" else "expense",
                                    category = LEDGER_PREFIX + (if (isRecv) "دریافت از " else "پرداخت به ") + d.partyName,
                                    amount = amt,
                                    note = note.trim(),
                                    dateMillis = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                    payTarget = null
                }) { Text("ثبت") }
            },
            dismissButton = { TextButton(onClick = { payTarget = null }) { Text("انصراف") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDebtDialog(initialReceivable: Boolean, onDismiss: () -> Unit, onSave: (DebtEntity) -> Unit) {
    var receivable by remember { mutableStateOf(initialReceivable) }
    var party by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Jalali.format(System.currentTimeMillis())) }
    var note by remember { mutableStateOf("") }

    val dateMillis = Jalali.parse(date)
    val amountVal = Jalali.normalizeDigits(amount).toLongOrNull() ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت طلب / بدهی") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.verticalScrollCompat()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = receivable, onClick = { receivable = true }, label = { Text("مشتری به من بدهکار است") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !receivable, onClick = { receivable = false }, label = { Text("من به مشتری بدهکارم") })
                }
                OutlinedTextField(party, { party = it }, label = { Text("نام مشتری") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(amount, { amount = it }, label = { Text("مبلغ (تومان)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(date, { date = it }, label = { Text("تاریخ شمسی (1405/08/21)") }, singleLine = true,
                    isError = dateMillis == null, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(phone, { phone = it }, label = { Text("تلفن") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(note, { note = it }, label = { Text("بابت (مثلاً خرید سرویس طلا)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = party.isNotBlank() && amountVal > 0 && dateMillis != null, onClick = {
                onSave(
                    DebtEntity(
                        direction = if (receivable) "receivable" else "payable",
                        partyName = party.trim(), phone = phone.trim(),
                        amount = amountVal, note = note.trim(), dateMillis = dateMillis!!
                    )
                )
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun Modifier.verticalScrollCompat(): Modifier =
    this.then(Modifier.verticalScroll(rememberScrollState()))
