package com.example.goldshop

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object LocalBackupPrefs {
    private fun p(c: Context) = c.getSharedPreferences("local_backup", Context.MODE_PRIVATE)

    fun enabled(c: Context) = p(c).getBoolean("enabled", false)
    fun setEnabled(c: Context, v: Boolean) = p(c).edit().putBoolean("enabled", v).apply()

    fun treeUri(c: Context): String? = p(c).getString("tree", null)
    fun setTreeUri(c: Context, v: String?) = p(c).edit().putString("tree", v).apply()

    fun intervalDays(c: Context) = p(c).getInt("interval", 1)
    fun setIntervalDays(c: Context, v: Int) = p(c).edit().putInt("interval", v).apply()

    fun lastSuccess(c: Context) = p(c).getLong("last_success", 0L)
    fun lastStatus(c: Context): String = p(c).getString("status", "") ?: ""
    fun setResult(c: Context, ok: Boolean, msg: String) {
        val e = p(c).edit()
        e.putString("status", Jalali.format(System.currentTimeMillis()) + " — " + msg)
        if (ok) e.putLong("last_success", System.currentTimeMillis())
        e.apply()
    }
}

// پشتیبان محلی: در پوشه‌ای که کاربر انتخاب می‌کند (ماندگار حتی با حذف برنامه)،
// و اگر پوشه‌ای انتخاب نشده باشد در حافظه اختصاصی برنامه. ۱۴ نسخه آخر نگه داشته می‌شود.
object LocalBackup {
    private const val WORK = "local_backup"
    private const val KEEP = 14
    private const val PREFIX = "goldshop-"

    fun schedule(c: Context) {
        val wm = WorkManager.getInstance(c)
        if (LocalBackupPrefs.enabled(c)) {
            val req = PeriodicWorkRequestBuilder<LocalBackupWorker>(
                LocalBackupPrefs.intervalDays(c).toLong(), TimeUnit.DAYS
            ).build()
            wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
        } else {
            wm.cancelUniqueWork(WORK)
        }
    }

    // نسخه‌ی کمکی هنگام باز شدن برنامه (بعضی گوشی‌ها کارهای پس‌زمینه را می‌بندند)
    fun runIfDue(c: Context) {
        if (!LocalBackupPrefs.enabled(c)) return
        val due = LocalBackupPrefs.intervalDays(c) * 24L * 3_600_000L * 9L / 10L
        if (System.currentTimeMillis() - LocalBackupPrefs.lastSuccess(c) < due) return
        Thread {
            runCatching { runNow(c) }
        }.start()
    }

    // یک نسخه می‌سازد و توضیح مقصد را برمی‌گرداند؛ در خطا استثنا می‌دهد
    fun runNow(c: Context): String {
        val name = PREFIX + SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date()) + ".db"
        val tmp = File(c.cacheDir, name)
        try {
            if (!BackupHelper.exportToFile(c, tmp)) error("خروجی گرفتن از دیتابیس ناموفق بود")
            val tree = LocalBackupPrefs.treeUri(c)
            val where: String
            if (tree != null) {
                val dir = DocumentFile.fromTreeUri(c, Uri.parse(tree)) ?: error("پوشه انتخابی در دسترس نیست")
                if (!dir.canWrite()) error("اجازه نوشتن در پوشه انتخابی وجود ندارد؛ پوشه را دوباره انتخاب کنید")
                val doc = dir.createFile("application/octet-stream", name) ?: error("ساخت فایل در پوشه ناموفق بود")
                val out = c.contentResolver.openOutputStream(doc.uri) ?: error("باز کردن فایل مقصد ناموفق بود")
                out.use { o -> tmp.inputStream().use { it.copyTo(o) } }
                dir.listFiles()
                    .filter { f -> (f.name ?: "").let { it.startsWith(PREFIX) && it.endsWith(".db") } }
                    .sortedByDescending { it.name ?: "" }
                    .drop(KEEP)
                    .forEach { it.delete() }
                where = "پوشه انتخابی"
            } else {
                val dir = File(c.getExternalFilesDir(null) ?: c.filesDir, "backups").apply { mkdirs() }
                tmp.copyTo(File(dir, name), overwrite = true)
                (dir.listFiles() ?: emptyArray<File>())
                    .filter { it.name.startsWith(PREFIX) && it.name.endsWith(".db") }
                    .sortedByDescending { it.name }
                    .drop(KEEP)
                    .forEach { it.delete() }
                where = "حافظه برنامه (Android/data)"
            }
            val msg = "موفق ($where): $name"
            LocalBackupPrefs.setResult(c, true, msg)
            return msg
        } catch (e: Exception) {
            LocalBackupPrefs.setResult(c, false, "ناموفق: " + (e.message ?: e.javaClass.simpleName))
            throw e
        } finally {
            tmp.delete()
        }
    }
}

class LocalBackupWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext
        if (!LocalBackupPrefs.enabled(c)) return Result.success()
        return try {
            LocalBackup.runNow(c)
            Result.success()
        } catch (e: Exception) {
            Result.success()
        }
    }
}
