package com.example.goldshop.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.goldshop.Audit
import com.example.goldshop.CustomerStore
import com.example.goldshop.Jalali
import com.example.goldshop.data.AppDatabase
import com.example.goldshop.data.ChequeEntity
import com.example.goldshop.data.CustomerEntity
import com.example.goldshop.data.DebtEntity
import com.example.goldshop.data.DebtPaymentEntity
import com.example.goldshop.data.InvoiceEntity
import com.example.goldshop.data.OldGoldEntity
import kotlinx.coroutines.launch

private fun waNumber(phone: String): String {
    val d = phone.filter { it.isDigit() }
    return if (d.startsWith("0")) "98" + d.drop(1) else d
}

private fun startSafe(context: Context, intent: Intent) {
    runCatching { context.startActivity(intent) }
        .onFailure { Toast.makeText(context, "برنامه‌ای برای این کار پیدا نشد", Toast.LENGTH_SHORT).show() }
}

@Composable
fun CustomersScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val customers by db.customerDao().all().collectAsState(initial = emptyList())
    val invoices by db.invoiceDao().all().collectAsState(initial = emptyList())
    val debts by db.debtDao().all().collectAsState(initial = emptyList())
    val payments by db.debtDao().allPayments().collectAsState(initial = emptyList())
    val cheques by db.chequeDao().all().collectAsState(initial = emptyList())
    val oldGold by db.oldGoldDao().all().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    BackHandler(enabled = selectedId != null) { selectedId = null }

    val paidBy = payments.groupBy { it.debtId }.mapValues { (_, l) -> l.sumOf { it.amount } }
    fun remaining(d: DebtEntity) = (d.amount - (paidBy[d.id] ?: 0L)).coerceAtLeast(0L)

    val selected = customers.firstOrNull { it.id == selectedId }

    if (selected != null) {
        CustomerDetail(
            customer = selected,
            invoices = invoices.filter { CustomerStore.samePerson(selected.name, selected.phone, it.customerName, it.customerPhone) },
            debts = debts.filter { CustomerStore.samePerson(selected.name, selected.phone, it.partyName, it.phone) },
            payments = payments,
            cheques = cheques.filter { CustomerStore.samePerson(selected.name, selected.phone, it.partyName, it.phone) },
            oldGold = oldGold.filter { CustomerStore.samePerson(selected.name, selected.phone, it.customerName, it.phone) },
            db = db,
            onBack = { selectedId = null }
        )
        return
    }

    val shown = customers.filter {
        query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) || it.phone.contains(query.trim())
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        GoldButton(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.size(6.dp))
            Text("مشتری جدید")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("جستجوی نام یا تلفن") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        if (shown.isEmpty()) {
            Text(
                if (customers.isEmpty()) "هنوز مشتری‌ای ثبت نشده است" else "موردی پیدا نشد",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shown, key = { it.id }) { c ->
                val recv = debts.filter { it.direction == "receivable" && CustomerStore.samePerson(c.name, c.phone, it.partyName, it.phone) }
                    .sumOf { remaining(it) }
                val pay = debts.filter { it.direction == "payable" && CustomerStore.samePerson(c.name, c.phone, it.partyName, it.phone) }
                    .sumOf { remaining(it) }
                LuxCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { selectedId = c.id }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(c.name, fontWeight = FontWeight.Bold)
                            if (c.phone.isNotBlank()) Text(c.phone, style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            if (recv > 0) Text("طلب: " + formatNum(recv), color = Lux.Positive, style = MaterialTheme.typography.bodySmall)
                            if (pay > 0) Text("بدهی: " + formatNum(pay), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        CustomerDialog(
            initial = null,
            onDismiss = { showAdd = false },
            onSave = { c ->
                scope.launch {
                    val exists = db.customerDao().allOnce().any { CustomerStore.norm(it.name) == CustomerStore.norm(c.name) }
                    if (exists) {
                        Toast.makeText(context, "مشتری با این نام قبلاً ثبت شده است", Toast.LENGTH_SHORT).show()
                    } else {
                        db.customerDao().insert(c)
                        Audit.log(context, "مشتری", "افزودن", c.name)
                    }
                }
                showAdd = false
            }
        )
    }
}

@Composable
private fun CustomerDialog(initial: CustomerEntity?, onDismiss: () -> Unit, onSave: (CustomerEntity) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var birth by remember {
        mutableStateOf(if (initial != null && initial.birthMillis > 0) Jalali.format(initial.birthMillis) else "")
    }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    val birthMillis: Long? = if (birth.isBlank()) 0L else Jalali.parse(birth)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "مشتری جدید" else "ویرایش مشتری") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(name, { name = it }, label = { Text("نام") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    phone, { phone = it }, label = { Text("تلفن") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    birth, { birth = it }, label = { Text("تاریخ تولد شمسی (1370/05/12) — اختیاری") }, singleLine = true,
                    isError = birthMillis == null, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(note, { note = it }, label = { Text("توضیح") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank() && birthMillis != null, onClick = {
                onSave(
                    CustomerEntity(
                        id = initial?.id ?: 0L,
                        name = name.trim(),
                        phone = phone.trim(),
                        birthMillis = birthMillis ?: 0L,
                        note = note.trim(),
                        createdMillis = initial?.createdMillis ?: System.currentTimeMillis()
                    )
                )
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun CustomerDetail(
    customer: CustomerEntity,
    invoices: List<InvoiceEntity>,
    debts: List<DebtEntity>,
    payments: List<DebtPaymentEntity>,
    cheques: List<ChequeEntity>,
    oldGold: List<OldGoldEntity>,
    db: AppDatabase,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showEdit by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    val paidBy = payments.groupBy { it.debtId }.mapValues { (_, l) -> l.sumOf { it.amount } }
    fun remaining(d: DebtEntity) = (d.amount - (paidBy[d.id] ?: 0L)).coerceAtLeast(0L)

    val recv = debts.filter { it.direction == "receivable" }.sumOf { remaining(it) }
    val pay = debts.filter { it.direction == "payable" }.sumOf { remaining(it) }
    val pendingRecvCheques = cheques.filter { it.status == "pending" && it.direction == "receivable" }.sumOf { it.amount }
    val pendingPayCheques = cheques.filter { it.status == "pending" && it.direction == "payable" }.sumOf { it.amount }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextButton(onClick = onBack) { Text("← بازگشت به فهرست مشتریان") }

        LuxCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("تلفن: " + customer.phone.ifBlank { "ثبت نشده" })
                Text("تاریخ تولد: " + if (customer.birthMillis > 0) Jalali.format(customer.birthMillis) else "ثبت نشده")
                if (customer.note.isNotBlank()) Text(customer.note, style = MaterialTheme.typography.bodySmall)
                if (customer.phone.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        TextButton(contentPadding = PaddingValues(horizontal = 6.dp), onClick = {
                            startSafe(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + customer.phone)))
                        }) { Text("تماس") }
                        TextButton(contentPadding = PaddingValues(horizontal = 6.dp), onClick = {
                            startSafe(context, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + customer.phone)))
                        }) { Text("پیامک") }
                        TextButton(contentPadding = PaddingValues(horizontal = 6.dp), onClick = {
                            startSafe(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + waNumber(customer.phone))))
                        }) { Text("واتساپ") }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(contentPadding = PaddingValues(horizontal = 6.dp), onClick = { showEdit = true }) { Text("ویرایش") }
                    TextButton(contentPadding = PaddingValues(horizontal = 6.dp), onClick = {
                        val text = "صورت حساب " + customer.name + "\n" +
                            "تعداد فاکتور: " + invoices.size + " | جمع خرید: " + formatNum(invoices.sumOf { it.total }) + " تومان\n" +
                            "مانده بدهی شما: " + formatNum(recv) + " تومان" +
                            if (pay > 0) "\nمانده طلب شما از ما: " + formatNum(pay) + " تومان" else ""
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        startSafe(context, Intent.createChooser(send, "ارسال صورت حساب"))
                    }) { Text("ارسال صورت حساب") }
                    TextButton(contentPadding = PaddingValues(horizontal = 6.dp), onClick = { showDelete = true }) { Text("حذف پرونده") }
                }
            }
        }

        LuxCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("خلاصه حساب", fontWeight = FontWeight.Bold)
                Text("تعداد فاکتور: " + invoices.size + " | جمع خرید: " + formatNum(invoices.sumOf { it.total }) + " تومان")
                Text("طلب من از مشتری: " + formatNum(recv) + " تومان", color = Lux.Positive, fontWeight = FontWeight.Bold)
                Text("بدهی من به مشتری: " + formatNum(pay) + " تومان", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                Text(
                    "چک در انتظار — دریافتی: " + formatNum(pendingRecvCheques) + " | پرداختی: " + formatNum(pendingPayCheques),
                    style = MaterialTheme.typography.bodySmall
                )
                if (oldGold.isNotEmpty()) {
                    Text(
                        "خرید کهنه از او: " + oldGold.size + " بار، جمع " + formatNum(oldGold.sumOf { it.total }) + " تومان",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        SectionCard("فاکتورها (" + invoices.size + ")") {
            if (invoices.isEmpty()) Text("—", style = MaterialTheme.typography.bodySmall)
            invoices.forEach { inv ->
                val d = debts.firstOrNull { it.invoiceId == inv.id }
                val rem = if (d != null) remaining(d) else 0L
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "#" + inv.id + " | " + Jalali.format(inv.dateMillis) + if (rem > 0) " | نسیه، مانده " + formatNum(rem) else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (rem > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(formatNum(inv.total) + " ت", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        SectionCard("طلب و بدهی (" + debts.size + ")") {
            if (debts.isEmpty()) Text("—", style = MaterialTheme.typography.bodySmall)
            debts.forEach { d ->
                val rem = remaining(d)
                val isRecv = d.direction == "receivable"
                Text(
                    (if (isRecv) "طلب" else "بدهی") + " | " + Jalali.format(d.dateMillis) + " | کل " + formatNum(d.amount) +
                        " | " + (if (rem == 0L) "تسویه ✓" else "مانده " + formatNum(rem)) +
                        (if (d.dueMillis > 0 && rem > 0) " | سررسید " + Jalali.format(d.dueMillis) else "") +
                        (if (d.invoiceId > 0) " | فاکتور " + d.invoiceId else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (rem == 0L) MaterialTheme.colorScheme.onSurface else if (isRecv) Lux.Positive else MaterialTheme.colorScheme.error
                )
            }
        }

        SectionCard("چک‌ها (" + cheques.size + ")") {
            if (cheques.isEmpty()) Text("—", style = MaterialTheme.typography.bodySmall)
            cheques.forEach { c ->
                val st = when (c.status) { "cashed" -> "وصول/پاس شده"; "bounced" -> "برگشتی"; else -> "در انتظار" }
                Text(
                    (if (c.direction == "receivable") "دریافتی" else "پرداختی") + " | " + formatNum(c.amount) +
                        " | سررسید " + Jalali.format(c.dueMillis) + " | " + st,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (showEdit) {
        CustomerDialog(
            initial = customer,
            onDismiss = { showEdit = false },
            onSave = { c ->
                scope.launch {
                    db.customerDao().update(c)
                    Audit.log(context, "مشتری", "ویرایش", c.name)
                }
                showEdit = false
            }
        )
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("حذف پرونده مشتری") },
            text = { Text("فقط پرونده «" + customer.name + "» حذف می‌شود؛ فاکتورها، چک‌ها و طلب/بدهی‌ها سر جای خود می‌مانند.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        db.customerDao().delete(customer)
                        Audit.log(context, "مشتری", "حذف", customer.name)
                    }
                    showDelete = false
                    onBack()
                }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    LuxCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            content()
        }
    }
}
