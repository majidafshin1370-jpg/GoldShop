package com.example.goldshop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

// مشخصات فروشگاه برای سربرگ فاکتور
object ShopProfile {
    private fun p(c: Context) = c.getSharedPreferences("shop_profile", Context.MODE_PRIVATE)

    fun name(c: Context): String = p(c).getString("name", "") ?: ""
    fun phone(c: Context): String = p(c).getString("phone", "") ?: ""
    fun address(c: Context): String = p(c).getString("address", "") ?: ""

    fun save(c: Context, name: String, phone: String, address: String) {
        p(c).edit().putString("name", name.trim()).putString("phone", phone.trim())
            .putString("address", address.trim()).apply()
    }

    private fun logoFile(c: Context) = File(c.filesDir, "shop_logo.png")

    fun loadLogo(c: Context): Bitmap? =
        logoFile(c).takeIf { it.exists() }?.let { runCatching { BitmapFactory.decodeFile(it.path) }.getOrNull() }

    fun removeLogo(c: Context) { logoFile(c).delete() }

    // لوگو را کوچک (حداکثر ۴۰۰ پیکسل) ذخیره می‌کند
    fun saveLogo(c: Context, uri: Uri): Boolean = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > 800 || bounds.outHeight / sample > 800) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: return false
        val scale = 400f / maxOf(bmp.width, bmp.height)
        val out = if (scale < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt().coerceAtLeast(1), (bmp.height * scale).toInt().coerceAtLeast(1), true) else bmp
        logoFile(c).outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
        true
    }.getOrElse { false }
}
