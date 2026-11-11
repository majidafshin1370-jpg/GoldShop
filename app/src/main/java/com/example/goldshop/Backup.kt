package com.example.goldshop

import android.content.Context
import android.net.Uri
import com.example.goldshop.data.AppDatabase
import java.io.File

object BackupHelper {
    private const val DB_NAME = "goldshop.db"

    // خروجی پایگاه‌داده به فایل انتخابی کاربر (بدون بستن دیتابیس)
    fun export(context: Context, target: Uri): Boolean = runCatching {
        val db = AppDatabase.get(context)
        // فلش کردن WAL در فایل اصلی تا نسخه کامل کپی شود
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
        val dbFile = context.getDatabasePath(DB_NAME)
        context.contentResolver.openOutputStream(target)?.use { out ->
            dbFile.inputStream().use { it.copyTo(out) }
        }
        true
    }.getOrElse { false }

    // بازگردانی پایگاه‌داده از فایل پشتیبان (نیازمند بستن برنامه پس از آن)
    fun import(context: Context, source: Uri): Boolean = runCatching {
        AppDatabase.closeInstance()
        val dbFile = context.getDatabasePath(DB_NAME)
        File(dbFile.parentFile, "$DB_NAME-wal").delete()
        File(dbFile.parentFile, "$DB_NAME-shm").delete()
        context.contentResolver.openInputStream(source)?.use { input ->
            dbFile.outputStream().use { input.copyTo(it) }
        } ?: return false
        true
    }.getOrElse { false }
}
