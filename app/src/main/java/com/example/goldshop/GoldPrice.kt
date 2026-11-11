package com.example.goldshop

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

// آخرین قیمت‌های دریافتی از API (بین صفحه‌ها به اشتراک گذاشته می‌شود)
object GoldPriceHolder {
    var geram18: Long? = null
    var geram24: Long? = null
    var mithqal: Long? = null
}

object GoldPriceApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // دریافت JSON و استخراج جفت‌های کلید-عدد
    fun fetch(url: String): Map<String, Double> = runCatching {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP " + resp.code)
            parse(resp.body?.string().orEmpty())
        }
    }.getOrElse { emptyMap() }

    private fun parse(json: String): Map<String, Double> {
        val out = mutableMapOf<String, Double>()
        val regex = Regex("\"([^\"]+)\"\s*:\s*\"?([\d,]+\.?\d*)\"?")
        for (m in regex.findAll(json)) {
            val key = m.groupValues[1].lowercase()
            val value = m.groupValues[2].replace(",", "").toDoubleOrNull() ?: continue
            if (value > 1000) out[key] = value
        }
        return out
    }

    // پیدا کردن یک قیمت با چند نام ممکن برای کلید
    fun pick(map: Map<String, Double>, vararg keys: String): Double? {
        for (k in keys) map[k]?.let { return it }
        return map.entries.firstOrNull { e -> keys.any { e.key.contains(it) } }?.value
    }
}
