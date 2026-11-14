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
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.goldshop.data.AppDatabase
import java.text.DecimalFormat
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderPrefs {
    private fun p(c: Context) = c.getSharedPreferences("reminders", Context.MODE_PRIVATE)

    fun enabled(c: Context) = p(c).getBoolean("enabled", false)
    fun setEnabled(c: Context, v: Boolean) = p(c).edit().putBoolean("enabled", v).apply()

    fun dayBefore(c: Context) = p(c).getBoolean("day_before", true)
    fun setDayBefore(c: Context, v: Boolean) = p(c).edit().putBoolean("day_before", v).apply()

    fun onDay(c: Context) = p(c).getBoolean("on_day", true)
    fun setOnDay(c: Context, v: Boolean) = p(c).edit().putBoolean("on_day", v).apply()

    fun overdue(c: Context) = p(c).getBoolean("overdue", true)
    fun setOverdue(c: Context, v: Boolean) = p(c).edit().putBoolean("overdue", v).apply()

    fun birthdays(c: Context) = p(c).getBoolean("birthdays", true)
    fun setBirthdays(c: Context, v: Boolean) = p(c).edit().putBoolean("birthdays", v).apply()

    fun lowStock(c: Context) = p(c).getBoolean("low_stock", true)
    fun setLowStock(c: Context, v: Boolean) = p(c).edit().putBoolean("low_stock", v).apply()

    fun hour(c: Context) = p(c).getInt("hour", 9)
    fun setHour(c: Context, v: Int) = p(c).edit().putInt("hour", v).apply()
}

object Reminders {
    private const val WORK = "due_reminders"
    private const val CHANNEL = "due_reminders"

    fun schedule(c: Context) {
        val wm = WorkManager.getInstance(c)
        if (!ReminderPrefs.enabled(c)) {
            wm.cancelUniqueWork(WORK)
            return
        }
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, ReminderPrefs.hour(c))
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        val delay = next.timeInMillis - now.timeInMillis
        val req = PeriodicWorkRequestBuilder<DueReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    private fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun addDays(millis: Long, days: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.add(Calendar.DAY_OF_YEAR, days)
        return cal.timeInMillis
    }

    private fun fmt(n: Long): String = DecimalFormat("#,###").format(n)

    // بررسی سررسیدها و ارسال اعلان؛ تعداد موارد اعلام‌شده را برمی‌گرداند
    suspend fun check(c: Context): Int {
        val db = AppDatabase.get(c)
        val today = startOfDay(System.currentTimeMillis())
        val tomorrow = addDays(today, 1)
        val dayAfter = addDays(today, 2)

        val lines = mutableListOf<String>()
        val wantOnDay = ReminderPrefs.onDay(c)
        val wantBefore = ReminderPrefs.dayBefore(c)
        val wantOverdue = ReminderPrefs.overdue(c)

        // وضعیت یک سررسید: null = اعلام نشود
        fun label(due: Long): String? {
            val d = startOfDay(due)
            return when {
                d < today -> if (wantOverdue) "عقب‌افتاده" else null
                d < tomorrow -> if (wantOnDay) "امروز" else null
                d < dayAfter -> if (wantBefore) "فردا" else null
                else -> null
            }
        }

        for (ch in db.chequeDao().allOnce().filter { it.status == "pending" }) {
            val l = label(ch.dueMillis) ?: continue
            val kind = if (ch.direction == "receivable") "چک دریافتی از " else "چک پرداختی به "
            lines.add("[$l] $kind${ch.partyName} — ${fmt(ch.amount)} تومان" +
                if (ch.chequeNumber.isNotBlank()) " (شماره ${ch.chequeNumber})" else "")
        }

        val paid = db.debtDao().allPaymentsOnce().groupBy { it.debtId }.mapValues { (_, l) -> l.sumOf { it.amount } }
        for (d in db.debtDao().allOnce().filter { it.dueMillis > 0 }) {
            val rem = d.amount - (paid[d.id] ?: 0L)
            if (rem <= 0) continue
            val l = label(d.dueMillis) ?: continue
            val kind = if (d.direction == "receivable") "قسط/طلب از " else "قسط/بدهی به "
            lines.add("[$l] $kind${d.partyName} — مانده ${fmt(rem)} تومان")
        }

        var count = 0
        if (lines.isNotEmpty()) {
            post(c, 1001, "یادآوری سررسید: " + lines.size + " مورد", lines, "ledger")
            count += lines.size
        }

        if (ReminderPrefs.birthdays(c)) {
            val t = Jalali.today()
            val names = db.customerDao().allOnce()
                .filter { it.birthMillis > 0 }
                .filter { val b = Jalali.parts(it.birthMillis); b.m == t.m && b.d == t.d }
                .map { it.name }
            if (names.isNotEmpty()) {
                post(c, 1002, "تولد مشتری", names.map { "امروز تولد $it است" }, "customers")
                count += names.size
            }
        }

        if (ReminderPrefs.lowStock(c)) {
            val low = db.productDao().allOnce().filter { it.minQuantity > 0 && it.quantity <= it.minQuantity }
            if (low.isNotEmpty()) {
                post(c, 1003, "موجودی کم: " + low.size + " محصول",
                    low.map { it.name + " — موجودی " + it.quantity }, "products")
                count += low.size
            }
        }
        return count
    }

    private fun post(c: Context, id: Int, title: String, lines: List<String>, route: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "یادآوری سررسید و موجودی", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val open = Intent(c, MainActivity::class.java).apply {
            putExtra("route", route)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pi = PendingIntent.getActivity(c, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val style = NotificationCompat.InboxStyle()
        lines.take(7).forEach { style.addLine(it) }
        if (lines.size > 7) style.setSummaryText("و " + (lines.size - 7) + " مورد دیگر")
        val n = NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(lines.first())
            .setStyle(style)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        nm.notify(id, n)
    }
}

class DueReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext
        if (!ReminderPrefs.enabled(c)) return Result.success()
        return try {
            Reminders.check(c)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
