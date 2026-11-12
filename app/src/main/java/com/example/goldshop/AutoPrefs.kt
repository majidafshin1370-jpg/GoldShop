package com.example.goldshop

import android.content.Context

// تنظیمات پشتیبان خودکار و گزارش ماهانه
object AutoPrefs {
    private fun p(c: Context) = c.getSharedPreferences("auto_tasks", Context.MODE_PRIVATE)

    fun driveEnabled(c: Context) = p(c).getBoolean("drive_enabled", false)
    fun setDriveEnabled(c: Context, v: Boolean) = p(c).edit().putBoolean("drive_enabled", v).apply()

    // فاصله پشتیبان‌گیری به روز (۱ = روزانه، ۷ = هفتگی)
    fun intervalDays(c: Context) = p(c).getInt("interval_days", 1)
    fun setIntervalDays(c: Context, v: Int) = p(c).edit().putInt("interval_days", v).apply()

    fun monthlyAuto(c: Context) = p(c).getBoolean("monthly_auto", false)
    fun setMonthlyAuto(c: Context, v: Boolean) = p(c).edit().putBoolean("monthly_auto", v).apply()

    fun lastBackupStatus(c: Context): String = p(c).getString("last_backup_status", "") ?: ""
    fun setLastBackupStatus(c: Context, msg: String) {
        p(c).edit()
            .putString("last_backup_status", Jalali.format(System.currentTimeMillis()) + " — " + msg)
            .apply()
    }
}
