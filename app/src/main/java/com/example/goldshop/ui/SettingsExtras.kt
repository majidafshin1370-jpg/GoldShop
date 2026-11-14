package com.example.goldshop.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import com.example.goldshop.Audit
import com.example.goldshop.LocalBackup
import com.example.goldshop.LocalBackupPrefs
import com.example.goldshop.Operators
import com.example.goldshop.ReminderPrefs
import com.example.goldshop.Reminders
import com.example.goldshop.ShopProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// بخش‌های جدید تنظیمات: کاربران، مشخصات فاکتور، یادآور، پشتیبان محلی
@Composable
fun ExtraSettings(onStatus: (String) -> Unit) {
    HorizontalDivider()
    OperatorSection(onStatus)
    HorizontalDivider()
    ShopProfileSection(onStatus)
    HorizontalDivider()
    ReminderSection(onStatus)
    HorizontalDivider()
    LocalBackupSection(onStatus)
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun OperatorSection(onStatus: (String) -> Unit) {
    val context = LocalContext.current
    var names by remember { mutableStateOf(Operators.names(context)) }
    var current by remember { mutableStateOf(Operators.current(context)) }
    var newName by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("کاربران (برای گزارش تغییرات)", fontWeight = FontWeight.Bold)
        Text(
            "کاربر فعال را انتخاب کنید؛ نام او کنار هر ثبت/ویرایش/حذف در «گزارش تغییرات» می‌آید. " +
                "توجه: شناسایی با انتخاب نام است، نه ورود با رمز جدا.",
            style = MaterialTheme.typography.bodySmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            names.forEach { n ->
                FilterChip(selected = current == n, onClick = {
                    Operators.setCurrent(context, n)
                    current = n
                    Audit.logAsync(context, "تنظیمات", "تغییر کاربر فعال", n)
                }, label = { Text(n) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newName, onValueChange = { newName = it },
                label = { Text("نام کاربر جدید") }, singleLine = true, modifier = Modifier.weight(1f)
            )
            TextButton(enabled = newName.isNotBlank(), onClick = {
                Operators.add(context, newName)
                names = Operators.names(context)
                newName = ""
            }) { Text("افزودن") }
        }
        if (names.size > 1) {
            TextButton(onClick = {
                Operators.remove(context, current)
                names = Operators.names(context)
                current = Operators.current(context)
            }) { Text("حذف کاربر فعال (" + current + ")") }
        }
    }
}

@Composable
private fun ShopProfileSection(onStatus: (String) -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(ShopProfile.name(context)) }
    var phone by remember { mutableStateOf(ShopProfile.phone(context)) }
    var address by remember { mutableStateOf(ShopProfile.address(context)) }
    var hasLogo by remember { mutableStateOf(ShopProfile.loadLogo(context) != null) }

    val pickLogo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val ok = ShopProfile.saveLogo(context, uri)
            hasLogo = ok && ShopProfile.loadLogo(context) != null
            onStatus(if (ok) "لوگو ذخیره شد" else "خطا در خواندن تصویر لوگو")
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("مشخصات فروشگاه روی فاکتور", fontWeight = FontWeight.Bold)
        OutlinedTextField(name, { name = it }, label = { Text("نام فروشگاه") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(phone, { phone = it }, label = { Text("تلفن") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(address, { address = it }, label = { Text("آدرس") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pickLogo.launch("image/*") }) {
                Text(if (hasLogo) "تغییر لوگو" else "انتخاب لوگو")
            }
            if (hasLogo) {
                OutlinedButton(onClick = {
                    ShopProfile.removeLogo(context)
                    hasLogo = false
                }) { Text("حذف لوگو") }
            }
        }
        GoldButton(onClick = {
            ShopProfile.save(context, name, phone, address)
            Audit.logAsync(context, "تنظیمات", "ویرایش", "مشخصات فروشگاه")
            onStatus("مشخصات فروشگاه ذخیره شد")
        }, modifier = Modifier.fillMaxWidth()) { Text("ذخیره مشخصات") }
    }
}

@Composable
private fun ReminderSection(onStatus: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var on by remember { mutableStateOf(ReminderPrefs.enabled(context)) }
    var dayBefore by remember { mutableStateOf(ReminderPrefs.dayBefore(context)) }
    var onDay by remember { mutableStateOf(ReminderPrefs.onDay(context)) }
    var overdue by remember { mutableStateOf(ReminderPrefs.overdue(context)) }
    var birthdays by remember { mutableStateOf(ReminderPrefs.birthdays(context)) }
    var lowStock by remember { mutableStateOf(ReminderPrefs.lowStock(context)) }
    var hour by remember { mutableStateOf(ReminderPrefs.hour(context)) }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("یادآوری سررسید چک، قسط و موجودی", fontWeight = FontWeight.Bold)
        SwitchRow("فعال‌سازی اعلان یادآوری", on) {
            on = it
            ReminderPrefs.setEnabled(context, it)
            Reminders.schedule(context)
            if (it && Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (on) {
            SwitchRow("یک روز قبل از سررسید", dayBefore) { dayBefore = it; ReminderPrefs.setDayBefore(context, it) }
            SwitchRow("صبح روز سررسید", onDay) { onDay = it; ReminderPrefs.setOnDay(context, it) }
            SwitchRow("مواردِ سررسید گذشته", overdue) { overdue = it; ReminderPrefs.setOverdue(context, it) }
            SwitchRow("تولد مشتریان", birthdays) { birthdays = it; ReminderPrefs.setBirthdays(context, it) }
            SwitchRow("هشدار کم‌بودن موجودی", lowStock) { lowStock = it; ReminderPrefs.setLowStock(context, it) }
            Text("ساعت اعلان:", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                listOf(7, 8, 9, 10, 12, 16).forEach { h ->
                    FilterChip(selected = hour == h, onClick = {
                        hour = h
                        ReminderPrefs.setHour(context, h)
                        Reminders.schedule(context)
                    }, label = { Text("$h:00") })
                }
            }
            OutlinedButton(onClick = {
                scope.launch {
                    val n = withContext(Dispatchers.IO) { runCatching { Reminders.check(context) }.getOrDefault(-1) }
                    onStatus(
                        if (n < 0) "خطا در بررسی سررسیدها"
                        else if (n == 0) "موردی برای یادآوری وجود ندارد"
                        else "$n مورد اعلام شد (اگر اعلانی نمی‌بینید، مجوز اعلان برنامه را بررسی کنید)"
                    )
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("آزمایش همین حالا") }
        }
        Text(
            "اعلان‌ها به‌صورت تقریبی در ساعت انتخابی نمایش داده می‌شوند؛ بعضی گوشی‌ها برای صرفه‌جویی باتری " +
                "کارهای پس‌زمینه را کمی دیرتر اجرا می‌کنند. ارسال پیامک خودکار انجام نمی‌شود.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun LocalBackupSection(onStatus: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var on by remember { mutableStateOf(LocalBackupPrefs.enabled(context)) }
    var interval by remember { mutableStateOf(LocalBackupPrefs.intervalDays(context)) }
    var folder by remember { mutableStateOf(LocalBackupPrefs.treeUri(context)) }
    var status by remember { mutableStateOf(LocalBackupPrefs.lastStatus(context)) }
    var busy by remember { mutableStateOf(false) }

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val ok = runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }.isSuccess
            if (ok) {
                LocalBackupPrefs.setTreeUri(context, uri.toString())
                folder = uri.toString()
                onStatus("پوشه پشتیبان انتخاب شد")
            } else {
                onStatus("دسترسی به این پوشه ممکن نشد؛ پوشه دیگری انتخاب کنید")
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("پشتیبان محلی خودکار (بدون نیاز به Google)", fontWeight = FontWeight.Bold)
        SwitchRow("پشتیبان‌گیری خودکار روی گوشی", on) {
            on = it
            LocalBackupPrefs.setEnabled(context, it)
            LocalBackup.schedule(context)
        }
        Text(
            if (folder != null) "پوشه انتخابی تنظیم شده است (ماندگار حتی با حذف برنامه)"
            else "پوشه‌ای انتخاب نشده؛ نسخه‌ها در حافظه اختصاصی برنامه ذخیره می‌شوند (با حذف برنامه پاک می‌شوند). " +
                "پیشنهاد: یک پوشه (مثلاً Documents/GoldShopBackup) انتخاب کنید.",
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedButton(onClick = { pickFolder.launch(null) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (folder == null) "انتخاب پوشه پشتیبان" else "تغییر پوشه پشتیبان")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = interval == 1, onClick = {
                interval = 1; LocalBackupPrefs.setIntervalDays(context, 1); LocalBackup.schedule(context)
            }, label = { Text("روزانه") })
            FilterChip(selected = interval == 7, onClick = {
                interval = 7; LocalBackupPrefs.setIntervalDays(context, 7); LocalBackup.schedule(context)
            }, label = { Text("هفتگی") })
        }
        GoldButton(enabled = !busy, onClick = {
            busy = true
            scope.launch {
                val r = withContext(Dispatchers.IO) { runCatching { LocalBackup.runNow(context) } }
                busy = false
                status = LocalBackupPrefs.lastStatus(context)
                onStatus(r.getOrElse { "خطا در پشتیبان محلی: " + (it.message ?: "") })
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "در حال پشتیبان‌گیری…" else "پشتیبان‌گیری محلی همین حالا") }
        if (status.isNotBlank()) Text("آخرین وضعیت: $status", style = MaterialTheme.typography.bodySmall)
        Text("۱۴ نسخه آخر نگه داشته می‌شود. بازگردانی: «بازگردانی پشتیبان» و انتخاب فایل از همین پوشه.", style = MaterialTheme.typography.bodySmall)
    }
}
