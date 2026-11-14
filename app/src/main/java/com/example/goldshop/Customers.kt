package com.example.goldshop

import com.example.goldshop.data.AppDatabase
import com.example.goldshop.data.CustomerEntity

object CustomerStore {
    // یکسان‌سازی «ي/ك» عربی و فاصله‌ها برای تطبیق نام
    fun norm(s: String): String =
        s.trim().replace('ي', 'ی').replace('ك', 'ک').replace("\u200c", " ").replace(Regex("\\s+"), " ").lowercase()

    fun samePerson(nameA: String, phoneA: String, nameB: String, phoneB: String): Boolean {
        if (norm(nameA).isNotEmpty() && norm(nameA) == norm(nameB)) return true
        val a = phoneA.filter { it.isDigit() }
        val b = phoneB.filter { it.isDigit() }
        return a.length >= 7 && b.length >= 7 && a.takeLast(10) == b.takeLast(10)
    }

    // اگر مشتری با این نام نبود، پرونده می‌سازد؛ اگر تلفن نداشت و اکنون داریم، تکمیل می‌کند
    suspend fun ensure(db: AppDatabase, name: String, phone: String) {
        val n = name.trim()
        if (n.isBlank()) return
        val existing = db.customerDao().allOnce().firstOrNull { norm(it.name) == norm(n) }
        if (existing == null) {
            db.customerDao().insert(
                CustomerEntity(name = n, phone = phone.trim(), createdMillis = System.currentTimeMillis())
            )
        } else if (existing.phone.isBlank() && phone.isNotBlank()) {
            db.customerDao().update(existing.copy(phone = phone.trim()))
        }
    }
}
