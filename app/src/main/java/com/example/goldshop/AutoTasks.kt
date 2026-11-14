package com.example.goldshop

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.io.IOException
import java.util.concurrent.TimeUnit

// زمان‌بندی کارهای خودکار: پشتیبان Google Drive و گزارش ماهانه
object AutoScheduler {
    private const val BACKUP = "drive_backup"
    private const val MONTHLY = "monthly_report"

    fun apply(context: Context) {
        val wm = WorkManager.getInstance(context)

        if (AutoPrefs.driveEnabled(context)) {
            val req = PeriodicWorkRequestBuilder<DriveBackupWorker>(
                AutoPrefs.intervalDays(context).toLong(), TimeUnit.DAYS
            ).setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            ).build()
            wm.enqueueUniquePeriodicWork(BACKUP, ExistingPeriodicWorkPolicy.UPDATE, req)
        } else {
            wm.cancelUniqueWork(BACKUP)
        }

        // یادآور سررسید و پشتیبان محلی
        Reminders.schedule(context)
        LocalBackup.schedule(context)

        if (AutoPrefs.monthlyAuto(context)) {
            val req = PeriodicWorkRequestBuilder<MonthlyReportWorker>(1, TimeUnit.DAYS).build()
            wm.enqueueUniquePeriodicWork(MONTHLY, ExistingPeriodicWorkPolicy.UPDATE, req)
        } else {
            wm.cancelUniqueWork(MONTHLY)
        }
    }
}

class DriveBackupWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext
        if (!AutoPrefs.driveEnabled(c)) return Result.success()
        val token = DriveBackup.accessTokenOrNull(c)
        if (token == null) {
            AutoPrefs.setLastBackupStatus(c, "ناموفق: اتصال به حساب Google نیاز به تایید مجدد دارد (تنظیمات را باز کنید)")
            return Result.success()
        }
        return try {
            val name = DriveBackup.backupNow(c, token)
            AutoPrefs.setLastBackupStatus(c, "موفق: $name")
            Result.success()
        } catch (e: IOException) {
            AutoPrefs.setLastBackupStatus(c, "ناموفق (دسترسی به اینترنت/Drive): " + (e.message ?: ""))
            Result.retry()
        } catch (e: Exception) {
            AutoPrefs.setLastBackupStatus(c, "ناموفق: " + (e.message ?: e.javaClass.simpleName))
            Result.success()
        }
    }
}

// هر روز بررسی می‌کند؛ اگر گزارش ماه شمسیِ قبل ساخته نشده باشد، آن را می‌سازد
class MonthlyReportWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext
        if (!AutoPrefs.monthlyAuto(c)) return Result.success()
        val today = Jalali.today()
        val (y, m) = Jalali.previousMonth(today.y, today.m)
        val file = MonthlyReport.fileFor(c, y, m)
        if (file.exists()) return Result.success()

        return try {
            val pdf = MonthlyReport.generate(c, y, m)
            if (AutoPrefs.driveEnabled(c)) {
                runCatching {
                    DriveBackup.accessTokenOrNull(c)?.let { DriveBackup.uploadReport(it, pdf) }
                }
            }
            notifyReady(c, pdf, Jalali.monthLabel(y, m))
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun notifyReady(c: Context, pdf: java.io.File, label: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel("reports", "گزارش ماهانه", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val uri = FileProvider.getUriForFile(c, c.packageName + ".fileprovider", pdf)
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pi = PendingIntent.getActivity(c, 0, view, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(c, "reports")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("گزارش ماهانه آماده شد")
            .setContentText(label)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(label.hashCode(), n)
    }
}
