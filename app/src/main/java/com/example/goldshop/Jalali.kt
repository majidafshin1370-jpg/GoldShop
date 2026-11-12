package com.example.goldshop

import android.icu.util.Calendar
import android.icu.util.ULocale

// ابزار تاریخ شمسی (بر پایه ICU اندروید، دقیق برای سال‌های کبیسه)
object Jalali {
    val monthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    private fun newCal(): Calendar = Calendar.getInstance(ULocale("fa_IR@calendar=persian"))

    data class Ymd(val y: Int, val m: Int, val d: Int)

    fun parts(millis: Long): Ymd {
        val c = newCal()
        c.timeInMillis = millis
        return Ymd(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    fun today(): Ymd = parts(System.currentTimeMillis())

    fun format(millis: Long): String {
        val p = parts(millis)
        return "%04d/%02d/%02d".format(p.y, p.m, p.d)
    }

    fun toMillis(y: Int, m: Int, d: Int, endOfDay: Boolean = false): Long {
        val c = newCal()
        c.clear()
        c.set(Calendar.YEAR, y)
        c.set(Calendar.MONTH, m - 1)
        c.set(Calendar.DAY_OF_MONTH, d)
        if (endOfDay) {
            c.set(Calendar.HOUR_OF_DAY, 23)
            c.set(Calendar.MINUTE, 59)
            c.set(Calendar.SECOND, 59)
            c.set(Calendar.MILLISECOND, 999)
        }
        return c.timeInMillis
    }

    fun monthStart(y: Int, m: Int): Long = toMillis(y, m, 1)

    fun monthEnd(y: Int, m: Int): Long {
        val next = if (m == 12) Ymd(y + 1, 1, 1) else Ymd(y, m + 1, 1)
        return toMillis(next.y, next.m, next.d) - 1
    }

    fun previousMonth(y: Int, m: Int): Pair<Int, Int> = if (m == 1) Pair(y - 1, 12) else Pair(y, m - 1)

    // تبدیل ارقام فارسی/عربی به لاتین
    fun normalizeDigits(s: String): String {
        val sb = StringBuilder()
        for (ch in s) {
            sb.append(
                when (ch) {
                    in '۰'..'۹' -> '0' + (ch - '۰')
                    in '٠'..'٩' -> '0' + (ch - '٠')
                    else -> ch
                }
            )
        }
        return sb.toString()
    }

    // ورودی «1405/08/21» (یا با - و ارقام فارسی) → میلی‌ثانیه؛ نامعتبر = null
    fun parse(input: String): Long? {
        val parts = normalizeDigits(input.trim()).split('/', '-').map { it.trim() }
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null
        if (y !in 1300..1500 || m !in 1..12 || d < 1) return null
        val maxDay = when {
            m <= 6 -> 31
            m <= 11 -> 30
            else -> 30 // اسفند: صحت کبیسه با بازگشت‌گیری پایین بررسی می‌شود
        }
        if (d > maxDay) return null
        val millis = toMillis(y, m, d)
        val back = parts(millis)
        return if (back.y == y && back.m == m && back.d == d) millis else null
    }

    fun monthLabel(y: Int, m: Int): String = monthNames[m - 1] + " " + y
}
