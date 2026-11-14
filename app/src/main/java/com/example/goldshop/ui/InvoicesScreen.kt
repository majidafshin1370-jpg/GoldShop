package com.example.goldshop.ui

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.goldshop.AppScope
import com.example.goldshop.BarcodeScan
import com.example.goldshop.DraftLine
import com.example.goldshop.GoldPriceHolder
import com.example.goldshop.InvoiceExtras
import com.example.goldshop.Jalali
import com.example.goldshop.PdfExporter
import com.example.goldshop.Sales
import com.example.goldshop.ShopProfile
import com.example.goldshop.ThermalPrinter
import com.example.goldshop.data.AppDatabase
import com.example.goldshop.data.InvoiceEntity
import com.example.goldshop.data.ProductEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DAY_MS = 86_400_000L
private val WHATSAPP = listOf("com.whatsapp", "com.whatsapp.w4b")
private val TELEGRAM = listOf("org.telegram.messenger", "org.telegram.messenger.web")

@Composable
fun InvoicesScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val invoices by db.invoiceDao().all().collectAsState(initial = emptyList())
    val debts by db.debtDao().all().collectAsState(initial = emptyList())
    val payments by db.debtDao().allPayments().collectAsState(initial = emptyList())
    var showCreate by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<InvoiceEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<InvoiceEntity?>(null) }
    var query by remember { mutableStateOf("") }

    val paidBy = payments.groupBy { it.debtId }.mapValues { (_, l) -> l.sumOf { it.amount } }
    val remainingByInvoice: Map<Long, Long> = debts.filter { it.invoiceId > 0 }
        .associate { it.invoiceId to (it.amount - (paidBy[it.id] ?: 0L)).coerceAtLeast(0L) }

    val shown = invoices.filter { query.isBlank() || it.customerName.contains(query.trim(), ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        GoldButton(onClick = { showCreate = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.size(6.dp))
            Text("صدور فاکتور جدید")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("جستجوی مشتری") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shown, key = { it.id }) { inv ->
                val rem = remainingByInvoice[inv.id] ?: 0L
                LuxCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { detail = inv }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(inv.customerName, fontWeight = FontWeight.Bold)
                            Text(
                                formatDate(inv.dateMillis) + " | " + inv.itemCount + " قلم",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (rem > 0) {
                                Text(
                                    "نسیه — مانده: " + formatNum(rem) + " ت",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Text(
                            formatNum(inv.total) + " ت",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { deleteTarget = inv }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف")
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateInvoiceDialog(db = db, onClose = { showCreate = false })
    }

    detail?.let { inv ->
        InvoiceDetailDialog(db = db, invoice = inv, onClose = { detail = null })
    }

    deleteTarget?.let { inv ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("حذف فاکتور") },
            text = {
                Text(
                    "فاکتور " + inv.customerName + " حذف شود؟ موجودی اقلام به انبار برمی‌گردد " +
                        "و اگر طلبی از این فاکتور ساخته شده، همراه با اقساطش حذف می‌شود."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    AppScope.io.launch { Sales.deleteInvoice(context, inv) }
                    deleteTarget = null
                }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("انصراف") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateInvoiceDialog(db: AppDatabase, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val products by db.productDao().all().collectAsState(initial = emptyList())
    val customers by db.customerDao().all().collectAsState(initial = emptyList())

    var customer by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var taxPercent by remember { mutableStateOf("9") }
    var productIndex by remember { mutableStateOf(0) }
    var weightText by remember { mutableStateOf("") }
    var feeText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var cashText by remember { mutableStateOf("") }
    var cashTouched by remember { mutableStateOf(false) }
    var dueText by remember { mutableStateOf(Jalali.format(System.currentTimeMillis() + 30 * DAY_MS)) }
    var toRegister by remember { mutableStateOf(false) }
    val draftItems = remember { mutableListOf<DraftLine>().toMutableStateList() }

    val selectedProduct: ProductEntity? =
        if (products.isEmpty()) null else products[productIndex.coerceAtMost(products.size - 1)]

    fun addProduct(p: ProductEntity, weight: Double?, fee: Long?) {
        val base = GoldPriceHolder.geram18 ?: 45000000L
        val gram = (base * p.karat / 18.0).toLong()
        draftItems.add(
            DraftLine(
                productId = p.id,
                name = p.name,
                weight = weight ?: p.weight,
                karat = p.karat,
                gramPrice = gram,
                feePerGram = fee ?: p.feeTomanPerGram
            )
        )
    }

    val taxVal = Jalali.normalizeDigits(taxPercent).toDoubleOrNull() ?: 0.0
    val t = Sales.totals(draftItems.toList(), taxVal)
    val total = t.total

    val cashParsed = Jalali.normalizeDigits(cashText).replace(",", "").toLongOrNull()
    val cash = if (!cashTouched) total else (cashParsed ?: 0L).coerceIn(0L, total)
    val remaining = total - cash
    val dueMillis = Jalali.parse(dueText)
    val dueOk = remaining <= 0 || dueText.isBlank() || dueMillis != null

    val stockWarnings = draftItems.groupBy { it.productId }.mapNotNull { (pid, l) ->
        val p = products.firstOrNull { it.id == pid }
        if (p != null && l.size > p.quantity) "موجودی «" + p.name + "» کافی نیست (موجود: " + p.quantity + ")" else null
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("فاکتور جدید") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                CustomerField(
                    name = customer,
                    onNameChange = { customer = it },
                    customers = customers,
                    onPick = { customer = it.name; phone = it.phone }
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("شماره تماس مشتری (اختیاری)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                if (products.isNotEmpty()) {
                    TextButton(onClick = {
                        BarcodeScan.scan(
                            context,
                            onResult = { code ->
                                scope.launch {
                                    val p = db.productDao().byBarcode(code)
                                    if (p == null) {
                                        Toast.makeText(context, "محصولی با این بارکد ثبت نشده: $code", Toast.LENGTH_LONG).show()
                                    } else {
                                        addProduct(p, null, null)
                                    }
                                }
                            },
                            onMessage = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                        )
                    }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Text(" افزودن با اسکن بارکد/QR")
                    }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedProduct?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("انتخاب محصول") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            products.forEachIndexed { i, p ->
                                DropdownMenuItem(
                                    text = { Text(p.name + " (موجودی " + p.quantity + ")") },
                                    onClick = {
                                        productIndex = i
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text("وزن (گرم) — خالی = وزن محصول") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    ScaleButton(onWeight = { weightText = it.toString() })
                    OutlinedTextField(
                        value = feeText,
                        onValueChange = { feeText = it },
                        label = { Text("اجرت (تومان/گرم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    GoldButton(
                        onClick = {
                            val p = selectedProduct
                            if (p != null) {
                                addProduct(
                                    p,
                                    Jalali.normalizeDigits(weightText).toDoubleOrNull(),
                                    Jalali.normalizeDigits(feeText).toLongOrNull()
                                )
                                weightText = ""
                                feeText = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("افزودن به فاکتور") }
                } else {
                    Text("ابتدا از صفحه محصولات، محصولی ثبت کنید")
                }

                draftItems.forEachIndexed { i, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            item.name + " | " + item.weight + "g | " + formatNum(item.lineTotal) + " ت",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        IconButton(onClick = { draftItems.removeAt(i) }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف")
                        }
                    }
                }
                stockWarnings.forEach {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                if (draftItems.isNotEmpty()) {
                    HorizontalDivider()
                    OutlinedTextField(
                        value = taxPercent,
                        onValueChange = { taxPercent = it },
                        label = { Text("مالیات (٪)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("جمع اقلام: " + formatNum(t.subtotal) + " تومان")
                    Text("اجرت: " + formatNum(t.feeTotal) + " تومان")
                    Text("مالیات: " + formatNum(t.tax) + " تومان")
                    Text(
                        "مبلغ نهایی: " + formatNum(total) + " تومان",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider()
                    Text("نحوه پرداخت", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = cashTouched && cash == total,
                            onClick = { cashTouched = false; cashText = "" },
                            label = { Text("همه نقد") }
                        )
                        FilterChip(
                            selected = cashTouched && cash == 0L,
                            onClick = { cashTouched = true; cashText = "0" },
                            label = { Text("همه نسیه") }
                        )
                    }
                    OutlinedTextField(
                        value = if (cashTouched) cashText else total.toString(),
                        onValueChange = { cashTouched = true; cashText = it },
                        label = { Text("نقد دریافتی (تومان)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (remaining > 0) {
                        Text(
                            "مانده نسیه: " + formatNum(remaining) + " تومان — به‌عنوان طلب از «" +
                                customer.ifBlank { "مشتری" } + "» ثبت می‌شود",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = dueText,
                            onValueChange = { dueText = it },
                            label = { Text("سررسید پرداخت مانده (1405/08/21) — اختیاری") },
                            singleLine = true,
                            isError = !dueOk,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (cash > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = toRegister, onCheckedChange = { toRegister = it })
                            Text("  ثبت نقد دریافتی در صندوق", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val lines = draftItems.toList()
                    val c = customer
                    val ph = phone
                    val cashNow = cash
                    val due = if (remaining > 0 && dueText.isNotBlank()) (dueMillis ?: 0L) else 0L
                    val reg = toRegister
                    AppScope.io.launch {
                        Sales.createInvoice(context, c, ph, lines, taxVal, cashNow, due, reg)
                    }
                    onClose()
                },
                enabled = customer.isNotBlank() && draftItems.isNotEmpty() && dueOk
            ) { Text("ثبت فاکتور") }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("انصراف") }
        }
    )
}

@Composable
private fun Act(label: String, modifier: Modifier, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) { Text(label, fontSize = 12.sp, maxLines = 1) }
}

@Composable
private fun InvoiceDetailDialog(
    db: AppDatabase,
    invoice: InvoiceEntity,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val items by db.invoiceDao().items(invoice.id).collectAsState(initial = emptyList())
    val debt by db.debtDao().byInvoice(invoice.id).collectAsState(initial = null)
    val payments by db.debtDao().allPayments().collectAsState(initial = emptyList())
    var showPrint by remember { mutableStateOf(false) }
    var showPay by remember { mutableStateOf(false) }

    val d = debt
    val remaining = if (d == null) 0L
    else (d.amount - payments.filter { it.debtId == d.id }.sumOf { it.amount }).coerceAtLeast(0L)
    val extras = InvoiceExtras(remaining, d?.dueMillis ?: 0L)
    val shown = invoice.copy(paidAmount = invoice.total - remaining)

    fun send(png: Boolean, packages: List<String>) {
        scope.launch {
            val file = withContext(Dispatchers.IO) {
                val full = db.invoiceDao().itemsOnce(invoice.id)
                if (png) PdfExporter.exportImage(context, shown, full, extras)
                else PdfExporter.export(context, shown, full, extras)
            }
            shareFile(
                context, file, if (png) "image/png" else "application/pdf", packages,
                buildInvoiceText(context, shown, remaining)
            )
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("فاکتور " + invoice.customerName) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    "شماره " + invoice.id + " | " + formatDate(invoice.dateMillis),
                    style = MaterialTheme.typography.bodySmall
                )
                if (invoice.customerPhone.isNotBlank()) {
                    Text("تماس: " + invoice.customerPhone, style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider()
                items.forEach { item ->
                    Text(
                        item.productName + " | " + item.weight + " گرم | عیار " + item.karat +
                            " | " + formatNum(item.lineTotal) + " تومان",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                HorizontalDivider()
                Text("جمع اقلام: " + formatNum(invoice.subtotal) + " تومان")
                Text("اجرت: " + formatNum(invoice.feeTotal) + " تومان")
                Text("مالیات: " + formatNum(invoice.taxTotal) + " تومان")
                Text(
                    "مبلغ نهایی: " + formatNum(invoice.total) + " تومان",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (d != null) {
                    HorizontalDivider()
                    Text("نقد دریافتی: " + formatNum(invoice.total - remaining) + " تومان")
                    Text(
                        if (remaining > 0) "مانده نسیه: " + formatNum(remaining) + " تومان" else "نسیه تسویه شده ✓",
                        fontWeight = FontWeight.Bold,
                        color = if (remaining > 0) MaterialTheme.colorScheme.error else Lux.Positive
                    )
                    if (d.dueMillis > 0 && remaining > 0) {
                        Text("سررسید: " + Jalali.format(d.dueMillis), style = MaterialTheme.typography.bodySmall)
                    }
                    if (remaining > 0) {
                        TextButton(onClick = { showPay = true }) { Text("ثبت دریافت قسط/تسویه") }
                    }
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Act("PDF", Modifier.weight(1f)) { send(false, emptyList()) }
                    Act("تصویر", Modifier.weight(1f)) { send(true, emptyList()) }
                    Act("واتساپ", Modifier.weight(1f)) { send(true, WHATSAPP) }
                    Act("تلگرام", Modifier.weight(1f)) { send(true, TELEGRAM) }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Act("متن", Modifier.weight(1f)) {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, buildInvoiceText(context, shown, remaining))
                        }
                        context.startActivity(Intent.createChooser(send, "ارسال با واتساپ/تلگرام/..."))
                    }
                    Act("پیامک", Modifier.weight(1f)) {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + invoice.customerPhone)).apply {
                            putExtra("sms_body", buildInvoiceText(context, shown, remaining))
                        }
                        runCatching { context.startActivity(intent) }
                    }
                    Act("چاپ", Modifier.weight(1f)) { showPrint = true }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onClose) { Text("بستن") }
        }
    )

    if (showPay && d != null) {
        DebtPaymentDialog(db = db, debt = d, remaining = remaining, onDismiss = { showPay = false })
    }

    if (showPrint) {
        PrintDeviceDialog(
            onDismiss = { showPrint = false },
            onDeviceSelected = { device ->
                showPrint = false
                val lines = buildList {
                    add(ShopProfile.name(context).ifBlank { "فاکتور فروش طلا" })
                    add("فاکتور شماره " + invoice.id)
                    add("مشتری: " + invoice.customerName)
                    add("تاریخ: " + formatDate(invoice.dateMillis))
                    add("--------------------")
                    items.forEach { add(it.productName + " " + formatNum(it.lineTotal)) }
                    add("--------------------")
                    add("مبلغ نهایی: " + formatNum(invoice.total) + " تومان")
                    if (remaining > 0) {
                        add("نقد: " + formatNum(invoice.total - remaining))
                        add("مانده نسیه: " + formatNum(remaining))
                    }
                }
                ThermalPrinter.print(device, lines) { _, msg ->
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

private fun buildInvoiceText(context: android.content.Context, invoice: InvoiceEntity, remaining: Long): String {
    val shop = ShopProfile.name(context)
    return (if (shop.isNotBlank()) shop + "\n" else "") +
        "فاکتور فروش طلا — شماره " + invoice.id + "\n" +
        "مشتری: " + invoice.customerName + "\n" +
        "تاریخ: " + Jalali.format(invoice.dateMillis) + "\n" +
        "مبلغ نهایی: " + formatNum(invoice.total) + " تومان" +
        if (remaining > 0) "\nنقد: " + formatNum(invoice.total - remaining) +
            "\nمانده: " + formatNum(remaining) + " تومان" else ""
}

private fun formatDate(millis: Long): String =
    Jalali.format(millis) + " " + SimpleDateFormat("HH:mm", Locale.US).format(Date(millis))
