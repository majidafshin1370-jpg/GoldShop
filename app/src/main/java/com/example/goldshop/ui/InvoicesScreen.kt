package com.example.goldshop.ui

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.goldshop.GoldPriceHolder
import com.example.goldshop.PdfExporter
import com.example.goldshop.ThermalPrinter
import com.example.goldshop.data.AppDatabase
import com.example.goldshop.data.InvoiceEntity
import com.example.goldshop.data.InvoiceItemEntity
import com.example.goldshop.data.ProductEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private data class DraftItem(
    val name: String,
    val weight: Double,
    val karat: Int,
    val gramPrice: Long,
    val feePerGram: Long
) {
    val lineTotal: Long get() = ((gramPrice + feePerGram) * weight).toLong()
    val lineFee: Long get() = (feePerGram * weight).toLong()
    val lineSubtotal: Long get() = (gramPrice * weight).toLong()
}

@Composable
fun InvoicesScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val invoices by db.invoiceDao().all().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showCreate by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<InvoiceEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(onClick = { showCreate = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.size(6.dp))
            Text("صدور فاکتور جدید")
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(invoices, key = { it.id }) { inv ->
                Card(modifier = Modifier.fillMaxWidth()) {
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
                        }
                        Text(
                            formatNum(inv.total) + " ت",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = {
                            scope.launch {
                                db.invoiceDao().deleteItems(inv.id)
                                db.invoiceDao().delete(inv.id)
                            }
                        }) {
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
        InvoiceDetailDialog(
            db = db,
            invoice = inv,
            onClose = { detail = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateInvoiceDialog(db: AppDatabase, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val products by db.productDao().all().collectAsState(initial = emptyList())

    var customer by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var taxPercent by remember { mutableStateOf("9") }
    var productIndex by remember { mutableStateOf(0) }
    var weightText by remember { mutableStateOf("") }
    var feeText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val draftItems = remember { mutableListOf<DraftItem>().toMutableStateList() }

    val selectedProduct: ProductEntity? =
        if (products.isEmpty()) null else products[productIndex.coerceAtMost(products.size - 1)]

    val subtotal = draftItems.sumOf { it.lineSubtotal }
    val feeTotal = draftItems.sumOf { it.lineFee }
    val tax = ((subtotal + feeTotal) * (taxPercent.toDoubleOrNull() ?: 0.0) / 100).toLong()
    val total = subtotal + feeTotal + tax

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("فاکتور جدید") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = customer,
                    onValueChange = { customer = it },
                    label = { Text("نام مشتری") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("شماره تماس مشتری (اختیاری)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                if (products.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedProduct?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("انتخاب محصول") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
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
                                    text = { Text(p.name) },
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
                        label = { Text("وزن (گرم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = feeText,
                        onValueChange = { feeText = it },
                        label = { Text("اجرت (تومان/گرم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val p = selectedProduct ?: return@Button
                            val base = GoldPriceHolder.geram18 ?: 45000000L
                            val gram = (base * p.karat / 18.0).toLong()
                            draftItems.add(
                                DraftItem(
                                    name = p.name,
                                    weight = weightText.toDoubleOrNull() ?: p.weight,
                                    karat = p.karat,
                                    gramPrice = gram,
                                    feePerGram = feeText.toLongOrNull() ?: p.feeTomanPerGram
                                )
                            )
                            weightText = ""
                            feeText = ""
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
                            item.name + " | " + item.weight + "g | " +
                                formatNum(item.lineTotal) + " ت",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        IconButton(onClick = { draftItems.removeAt(i) }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف")
                        }
                    }
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
                    Text("جمع اقلام: " + formatNum(subtotal) + " تومان")
                    Text("اجرت: " + formatNum(feeTotal) + " تومان")
                    Text("مالیات: " + formatNum(tax) + " تومان")
                    Text(
                        "مبلغ نهایی: " + formatNum(total) + " تومان",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        val invId = db.invoiceDao().insert(
                            InvoiceEntity(
                                customerName = customer,
                                customerPhone = phone,
                                dateMillis = System.currentTimeMillis(),
                                itemCount = draftItems.size,
                                subtotal = subtotal,
                                feeTotal = feeTotal,
                                taxTotal = tax,
                                total = total
                            )
                        )
                        db.invoiceDao().insertItems(
                            draftItems.map {
                                InvoiceItemEntity(
                                    invoiceId = invId,
                                    productName = it.name,
                                    weight = it.weight,
                                    karat = it.karat,
                                    gramPrice = it.gramPrice,
                                    feePerGram = it.feePerGram,
                                    lineTotal = it.lineTotal
                                )
                            }
                        )
                    }
                    onClose()
                },
                enabled = customer.isNotBlank() && draftItems.isNotEmpty()
            ) { Text("ثبت فاکتور") }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("انصراف") }
        }
    )
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
    var showPrint by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("فاکتور " + invoice.customerName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "تاریخ: " + formatDate(invoice.dateMillis),
                    style = MaterialTheme.typography.bodySmall
                )
                if (invoice.customerPhone.isNotBlank()) {
                    Text(
                        "تماس: " + invoice.customerPhone,
                        style = MaterialTheme.typography.bodySmall
                    )
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
            }
        },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    scope.launch {
                        val fullItems = db.invoiceDao().itemsOnce(invoice.id)
                        val file = PdfExporter.export(context, invoice, fullItems)
                        val uri = FileProvider.getUriForFile(
                            context,
                            context.packageName + ".fileprovider",
                            file
                        )
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(share, "اشتراک PDF"))
                    }
                }) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Text(" PDF")
                }
                TextButton(onClick = {
                    val body = buildInvoiceText(invoice)
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, body)
                    }
                    context.startActivity(Intent.createChooser(send, "ارسال با واتساپ/تلگرام/..."))
                }) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Text(" متن")
                }
                TextButton(onClick = {
                    val body = buildInvoiceText(invoice)
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + invoice.customerPhone)).apply {
                        putExtra("sms_body", body)
                    }
                    runCatching { context.startActivity(intent) }
                }) {
                    Icon(Icons.Default.Sms, contentDescription = null)
                    Text(" پیامک")
                }
                TextButton(onClick = { showPrint = true }) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Text(" چاپ")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("بستن") }
        }
    )

    if (showPrint) {
        PrintDeviceDialog(
            onDismiss = { showPrint = false },
            onDeviceSelected = { device ->
                showPrint = false
                val lines = buildList {
                    add("فاکتور فروش طلا")
                    add("مشتری: " + invoice.customerName)
                    add("تاریخ: " + formatDate(invoice.dateMillis))
                    add("--------------------")
                    items.forEach { add(it.productName + " " + formatNum(it.lineTotal)) }
                    add("--------------------")
                    add("مبلغ نهایی: " + formatNum(invoice.total) + " تومان")
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

private fun buildInvoiceText(invoice: InvoiceEntity): String =
    "فاکتور فروش طلا\n" +
        "مشتری: " + invoice.customerName + "\n" +
        "مبلغ نهایی: " + formatNum(invoice.total) + " تومان"

private fun formatDate(millis: Long): String =
    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(millis))
