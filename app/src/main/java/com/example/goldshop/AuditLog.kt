package com.example.goldshop

import android.content.Context
import com.example.goldshop.data.AppDatabase
import com.example.goldshop.data.AuditEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// کاربران (اپراتورها): نام کاربر فعال در گزارش تغییرات ثبت می‌شود
object Operators {
    const val DEFAULT = "مدیر"
    private fun p(c: Context) = c.getSharedPreferences("operators", Context.MODE_PRIVATE)

    fun names(c: Context): List<String> {
        val list = (p(c).getString("names", "") ?: "").split("\n").map { it.trim() }.filter { it.isNotBlank() }
        return if (list.isEmpty()) listOf(DEFAULT) else list
    }

    fun current(c: Context): String {
        val cur = p(c).getString("current", "") ?: ""
        return if (cur.isBlank()) names(c).first() else cur
    }

    fun setCurrent(c: Context, name: String) = p(c).edit().putString("current", name).apply()

    fun add(c: Context, name: String) {
        val n = name.trim()
        if (n.isBlank()) return
        val list = names(c).toMutableList()
        if (list.none { it == n }) list.add(n)
        p(c).edit().putString("names", list.joinToString("\n")).apply()
    }

    fun remove(c: Context, name: String) {
        val list = names(c).filter { it != name }
        p(c).edit().putString("names", list.joinToString("\n")).apply()
        if (current(c) == name) setCurrent(c, list.firstOrNull() ?: DEFAULT)
    }
}

// دامنه‌ای که با بسته شدن دیالوگ/صفحه لغو نمی‌شود (برای ذخیره‌های مهم دیتابیس)
object AppScope {
    val io: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

object Audit {
    private val scope = AppScope.io

    suspend fun log(c: Context, section: String, action: String, details: String) {
        runCatching {
            AppDatabase.get(c).auditDao().insert(
                AuditEntity(
                    timeMillis = System.currentTimeMillis(),
                    actor = Operators.current(c),
                    section = section,
                    action = action,
                    details = details
                )
            )
        }
    }

    // برای فراخوانی از callbackهای معمولی (غیر suspend)
    fun logAsync(c: Context, section: String, action: String, details: String) {
        val app = c.applicationContext
        scope.launch { log(app, section, action, details) }
    }
}
