package com.example.goldshop.ui

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.goldshop.AppScope
import com.example.goldshop.Audit
import com.example.goldshop.CustomerStore
import com.example.goldshop.Jalali
import com.example.goldshop.ScaleReader
import com.example.goldshop.data.AppDatabase
import com.example.goldshop.data.CustomerEntity
import com.example.goldshop.data.DebtEntity
import com.example.goldshop.data.DebtPaymentEntity
import com.example.goldshop.data.LEDGER_PREFIX
import com.example.goldshop.data.TransactionEntity
import java.io.File
import kotlinx.coroutines.launch

// فیلد نام مشتری با پیشنهاد خودکار از پرونده مشتریان
@Composable
fun CustomerField(
    name: String,
    onNameChange: (String) -> Unit,
    customers: List<CustomerEntity>,
    onPick: (CustomerEntity) -> Unit,
    label: String = "نام مشتری"
) {
    val q = CustomerStore.norm(name)
    val matches = if (q.isEmpty()) emptyList() else customers.filter {
        val n = CustomerStore.norm(it.name)
        n.contains(q) && n != q
    }.take(4)
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        matches.forEach { c ->
            Text(
                c.name + if (c.phone.isNotBlank()) " — " + c.phone else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(c) }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}

// دکمه خواندن وزن از ترازوی بلوتوثی
@Composable
fun ScaleButton(onWeight: (Double) -> Unit) {
    val context = LocalContext.current
    var show by remember { mutableStateOf(false) }
    TextButton(onClick = { show = true }) { Text("وزن از ترازو") }
    if (show) {
        PrintDeviceDialog(
            title = "انتخاب ترازوی بلوتوثی",
            actionLabel = "خواندن وزن",
            onDismiss = { show = false },
            onDeviceSelected = { device ->
                show = false
                Toast.makeText(context, "در حال خواندن وزن…", Toast.LENGTH_SHORT).show()
                ScaleReader.readStable(device) { w, msg ->
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        if (w != null) onWeight(w)
                    }
                }
            }
        )
    }
}

// ارسال فایل؛ اگر برنامه مقصد (واتساپ/تلگرام) نصب نبود، انتخاب‌گر عمومی باز می‌شود
fun shareFile(context: Context, file: File, mime: String, packages: List<String>, caption: String = "") {
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    fun build(pkg: String?): Intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        if (caption.isNotBlank()) putExtra(Intent.EXTRA_TEXT, caption)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (pkg != null) setPackage(pkg)
    }
    for (pkg in packages) {
        val ok = runCatching { context.startActivity(build(pkg)) }.isSuccess
        if (ok) return
    }
    context.startActivity(Intent.createChooser(build(null), "ارسال"))
}

// دریافت/پرداخت قسط یک طلب یا بدهی (مشترک بین صفحه طلب و بدهی و جزئیات فاکتور)
@Composable
fun DebtPaymentDialog(db: AppDatabase, debt: DebtEntity, remaining: Long, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val isRecv = debt.direction == "receivable"
    var amount by remember(debt.id) { mutableStateOf(remaining.toString()) }
    var note by remember(debt.id) { mutableStateOf("") }
    var toCash by remember(debt.id) { mutableStateOf(true) }
    val amt = Jalali.normalizeDigits(amount).replace(",", "").toLongOrNull() ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text((if (isRecv) "دریافت از " else "پرداخت به ") + debt.partyName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("مانده: " + formatNum(remaining) + " تومان")
                OutlinedTextField(
                    amount, { amount = it }, label = { Text("مبلغ (تومان)") }, singleLine = true,
                    isError = amt <= 0 || amt > remaining,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    note, { note = it }, label = { Text("توضیح (مثلاً نقد/کارت)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = toCash, onCheckedChange = { toCash = it })
                    Text("ثبت در صندوق")
                }
                Text("برای تسویه کامل، مبلغ را برابر مانده بگذارید.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(enabled = amt in 1..remaining, onClick = {
                AppScope.io.launch {
                    db.debtDao().insertPayment(
                        DebtPaymentEntity(debtId = debt.id, amount = amt, note = note.trim(), dateMillis = System.currentTimeMillis())
                    )
                    if (toCash) {
                        db.transactionDao().insert(
                            TransactionEntity(
                                type = if (isRecv) "income" else "expense",
                                category = LEDGER_PREFIX + (if (isRecv) "دریافت از " else "پرداخت به ") + debt.partyName,
                                amount = amt,
                                note = note.trim(),
                                dateMillis = System.currentTimeMillis()
                            )
                        )
                    }
                    Audit.log(
                        context, "طلب و بدهی", if (isRecv) "دریافت قسط" else "پرداخت قسط",
                        debt.partyName + " — " + amt + " تومان" + if (debt.invoiceId > 0) " (فاکتور ${debt.invoiceId})" else ""
                    )
                }
                onDismiss()
            }) { Text("ثبت") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
