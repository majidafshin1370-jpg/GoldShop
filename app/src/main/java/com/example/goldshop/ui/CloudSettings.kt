package com.example.goldshop.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.goldshop.AutoPrefs
import com.example.goldshop.AutoScheduler
import com.example.goldshop.BackupHelper
import com.example.goldshop.DriveBackup
import com.example.goldshop.DriveBackupFile
import com.example.goldshop.Jalali
import com.example.goldshop.MonthlyReport
import com.google.android.gms.auth.api.identity.Identity
import java.io.File
import kotlinx.coroutines.launch

// بخش «پشتیبان ابری Google Drive» و «گزارش ماهانه خودکار» در تنظیمات
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CloudSection(onStatus: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var driveOn by remember { mutableStateOf(AutoPrefs.driveEnabled(context)) }
    var interval by remember { mutableStateOf(AutoPrefs.intervalDays(context)) }
    var monthlyOn by remember { mutableStateOf(AutoPrefs.monthlyAuto(context)) }
    var lastStatus by remember { mutableStateOf(AutoPrefs.lastBackupStatus(context)) }
    var busy by remember { mutableStateOf(false) }
    var restoreList by remember { mutableStateOf<List<DriveBackupFile>?>(null) }
    var restoreTarget by remember { mutableStateOf<DriveBackupFile?>(null) }

    fun enableDrive() {
        AutoPrefs.setDriveEnabled(context, true)
        driveOn = true
        AutoScheduler.apply(context)
        onStatus("اتصال به Google Drive برقرار شد و پشتیبان خودکار فعال شد")
    }

    val authLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        try {
            val r = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(res.data)
            if (r.accessToken != null) enableDrive() else onStatus("اتصال به Google انجام نشد")
        } catch (e: Exception) {
            onStatus("اتصال به Google ناموفق: " + (e.message ?: ""))
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun connect() {
        Identity.getAuthorizationClient(context).authorize(DriveBackup.authRequest())
            .addOnSuccessListener { r ->
                val pi = r.pendingIntent
                if (r.hasResolution() && pi != null) {
                    authLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                } else {
                    enableDrive()
                }
            }
            .addOnFailureListener { e ->
                onStatus("اتصال به Google ناموفق: " + (e.message ?: "") + " — تنظیمات OAuth در README را بررسی کنید")
            }
    }

    HorizontalDivider()
    Text("پشتیبان ابری (Google Drive)", fontWeight = FontWeight.Bold)

    if (!driveOn) {
        Button(onClick = { connect() }, modifier = Modifier.fillMaxWidth()) {
            Text("اتصال به Google Drive و فعال‌سازی پشتیبان خودکار")
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()) {
            Text("پشتیبان خودکار فعال است")
            Switch(checked = true, onCheckedChange = {
                AutoPrefs.setDriveEnabled(context, false)
                driveOn = false
                AutoScheduler.apply(context)
            })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("تکرار:")
            FilterChip(selected = interval == 1, onClick = {
                interval = 1; AutoPrefs.setIntervalDays(context, 1); AutoScheduler.apply(context)
            }, label = { Text("روزانه") })
            FilterChip(selected = interval == 7, onClick = {
                interval = 7; AutoPrefs.setIntervalDays(context, 7); AutoScheduler.apply(context)
            }, label = { Text("هفتگی") })
        }
        Button(
            enabled = !busy,
            onClick = {
                busy = true
                scope.launch {
                    val token = DriveBackup.accessTokenOrNull(context)
                    if (token == null) {
                        onStatus("ابتدا اتصال به Google را مجدد تایید کنید")
                        connect()
                    } else {
                        try {
                            val name = DriveBackup.backupNow(context, token)
                            AutoPrefs.setLastBackupStatus(context, "موفق: $name")
                            onStatus("پشتیبان در Drive ذخیره شد: $name")
                        } catch (e: Exception) {
                            AutoPrefs.setLastBackupStatus(context, "ناموفق: " + (e.message ?: ""))
                            onStatus("خطا در پشتیبان‌گیری: " + (e.message ?: ""))
                        }
                    }
                    lastStatus = AutoPrefs.lastBackupStatus(context)
                    busy = false
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (busy) "در حال انجام…" else "پشتیبان‌گیری همین حالا در Drive") }

        OutlinedButton(
            enabled = !busy,
            onClick = {
                busy = true
                scope.launch {
                    val token = DriveBackup.accessTokenOrNull(context)
                    if (token == null) {
                        onStatus("ابتدا اتصال به Google را مجدد تایید کنید")
                        connect()
                    } else {
                        try {
                            val list = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                DriveBackup.listBackups(token)
                            }
                            if (list.isEmpty()) onStatus("پشتیبانی در Drive پیدا نشد") else restoreList = list
                        } catch (e: Exception) {
                            onStatus("خطا در دریافت فهرست: " + (e.message ?: ""))
                        }
                    }
                    busy = false
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("بازگردانی از Google Drive") }

        if (lastStatus.isNotBlank()) {
            Text("آخرین پشتیبان: $lastStatus", style = MaterialTheme.typography.bodySmall)
        }
    }

    HorizontalDivider()
    Text("گزارش ماهانه خودکار", fontWeight = FontWeight.Bold)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()) {
        Text("ساخت خودکار PDF ماه قبل در ابتدای هر ماه شمسی")
        Switch(checked = monthlyOn, onCheckedChange = { on ->
            monthlyOn = on
            AutoPrefs.setMonthlyAuto(context, on)
            AutoScheduler.apply(context)
            if (on && Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        })
    }
    Text(
        "گزارش‌ها در بخش «گزارش» → «گزارش PDF ماهانه» قابل مشاهده و ارسال هستند" +
            (if (driveOn) "؛ نسخه‌ای هم در Drive ذخیره می‌شود." else "."),
        style = MaterialTheme.typography.bodySmall
    )

    restoreList?.let { list ->
        AlertDialog(
            onDismissRequest = { restoreList = null },
            title = { Text("انتخاب پشتیبان برای بازگردانی") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    list.take(10).forEach { f ->
                        TextButton(onClick = { restoreTarget = f; restoreList = null }, modifier = Modifier.fillMaxWidth()) {
                            Text(f.name + "  (" + (f.size / 1024) + " KB)")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { restoreList = null }) { Text("بستن") } }
        )
    }

    restoreTarget?.let { f ->
        AlertDialog(
            onDismissRequest = { restoreTarget = null },
            title = { Text("بازگردانی پشتیبان؟") },
            text = { Text("همه داده‌های فعلی با «${f.name}» جایگزین می‌شود. ادامه می‌دهید؟") },
            confirmButton = {
                TextButton(onClick = {
                    restoreTarget = null
                    busy = true
                    scope.launch {
                        try {
                            val token = DriveBackup.accessTokenOrNull(context) ?: throw IllegalStateException("نیاز به اتصال مجدد")
                            val tmp = File(context.cacheDir, "drive-restore.db")
                            DriveBackup.download(token, f.id, tmp)
                            if (!BackupHelper.looksLikeSqlite(tmp)) throw IllegalStateException("فایل دانلودشده معتبر نیست")
                            if (BackupHelper.import(context, Uri.fromFile(tmp))) {
                                onStatus("بازگردانی انجام شد. برنامه را کاملاً ببندید و دوباره باز کنید.")
                            } else {
                                onStatus("خطا در بازگردانی")
                            }
                            tmp.delete()
                        } catch (e: Exception) {
                            onStatus("خطا در بازگردانی: " + (e.message ?: ""))
                        }
                        busy = false
                    }
                }) { Text("بله، بازگردانی کن") }
            },
            dismissButton = { TextButton(onClick = { restoreTarget = null }) { Text("انصراف") } }
        )
    }
}
