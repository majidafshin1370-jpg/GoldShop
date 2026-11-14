package com.example.goldshop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.goldshop.data.InvoiceEntity
import com.example.goldshop.data.InvoiceItemEntity
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// اطلاعات پرداخت برای چاپ روی فاکتور (فروش نسیه)
data class InvoiceExtras(val remaining: Long = 0L, val dueMillis: Long = 0L)

// ساخت فاکتور با لوگو و نام فروشگاه، به‌صورت PDF یا تصویر PNG
object InvoiceRenderer {
    private const val W = 595f
    private const val M = 36f
    private val GOLD = Color.parseColor("#B8901F")
    private val DARK = Color.parseColor("#1A1305")
    private val GRAY = Color.parseColor("#666666")
    private val RED = Color.parseColor("#C0392B")

    private fun fmt(n: Long): String = DecimalFormat("#,###").format(n)

    private class Pen(val context: Context, val canvas: Canvas) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        private val regular: Typeface = ResourcesCompat.getFont(context, R.font.vazirmatn_regular) ?: Typeface.DEFAULT
        private val bold: Typeface = ResourcesCompat.getFont(context, R.font.vazirmatn_bold) ?: Typeface.DEFAULT_BOLD

        // متن را داخل ستونی به عرض width رسم می‌کند و ارتفاع را برمی‌گرداند
        fun text(
            s: String, x: Float, y: Float, width: Float, size: Float,
            isBold: Boolean = false, color: Int = DARK, align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
        ): Float {
            paint.typeface = if (isBold) bold else regular
            paint.textSize = size
            paint.color = color
            val layout = StaticLayout.Builder.obtain(s, 0, s.length, paint, width.toInt().coerceAtLeast(1))
                .setAlignment(align)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .build()
            canvas.save()
            canvas.translate(x, y)
            layout.draw(canvas)
            canvas.restore()
            return layout.height.toFloat()
        }

        fun line(y: Float, color: Int = Color.LTGRAY) {
            paint.color = color
            paint.strokeWidth = 0.8f
            canvas.drawLine(M, y, W - M, y, paint)
        }

        fun rect(l: Float, t: Float, r: Float, b: Float, color: Int) {
            paint.color = color
            paint.style = Paint.Style.FILL
            canvas.drawRect(l, t, r, b, paint)
        }
    }

    // ستون‌ها از راست به چپ: ردیف، شرح، عیار، وزن، قیمت گرم، اجرت، مبلغ
    private val colW = floatArrayOf(28f, 150f, 40f, 55f, 90f, 75f, 85f)

    // رسم کامل فاکتور؛ ارتفاع نهایی را برمی‌گرداند
    private fun draw(
        context: Context, canvas: Canvas, invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>, extras: InvoiceExtras
    ): Float {
        val pen = Pen(context, canvas)
        val full = W - 2 * M
        var y = M

        // سربرگ: لوگو و مشخصات فروشگاه (راست)، شماره و تاریخ (چپ)
        val logo = ShopProfile.loadLogo(context)
        var textRight = W - M
        if (logo != null) {
            val size = 62f
            val scale = size / maxOf(logo.width, logo.height)
            val dw = logo.width * scale
            val dh = logo.height * scale
            val dst = android.graphics.RectF(W - M - dw, y, W - M, y + dh)
            canvas.drawBitmap(logo, null, dst, pen.paint)
            textRight = W - M - dw - 10f
        }
        val shopName = ShopProfile.name(context).ifBlank { "طلا و جواهر" }
        val headW = textRight - M - 150f
        var hy = y
        hy += pen.text(shopName, textRight - headW, hy, headW, 20f, true, DARK, Layout.Alignment.ALIGN_OPPOSITE)
        val sub = listOf(ShopProfile.phone(context), ShopProfile.address(context)).filter { it.isNotBlank() }
        for (s in sub) hy += pen.text(s, textRight - headW, hy, headW, 10f, false, GRAY, Layout.Alignment.ALIGN_OPPOSITE)
        val d = Date(invoice.dateMillis)
        val dateStr = Jalali.format(invoice.dateMillis) + "  " + SimpleDateFormat("HH:mm", Locale.US).format(d)
        var ly = y
        ly += pen.text("فاکتور فروش", M, ly, 140f, 15f, true, GOLD, Layout.Alignment.ALIGN_NORMAL)
        ly += pen.text("شماره: " + invoice.id, M, ly, 140f, 11f)
        ly += pen.text("تاریخ: $dateStr", M, ly, 140f, 10f)
        y = maxOf(hy, ly, y + 66f) + 8f
        pen.rect(M, y, W - M, y + 2.5f, GOLD)
        y += 12f

        // مشتری
        val cust = "مشتری: " + invoice.customerName +
            if (invoice.customerPhone.isNotBlank()) "     تلفن: " + invoice.customerPhone else ""
        y += pen.text(cust, M, y, full, 12f, true, DARK, Layout.Alignment.ALIGN_NORMAL) + 10f

        // سرستون جدول
        val heads = listOf("ردیف", "شرح کالا", "عیار", "وزن (گرم)", "قیمت هر گرم", "اجرت/گرم", "مبلغ")
        val headH = 24f
        pen.rect(M, y, W - M, y + headH, GOLD)
        var x = W - M
        for (i in heads.indices) {
            x -= colW[i]
            pen.text(heads[i], x + 2f, y + 3f, colW[i] - 4f, 9.5f, true, Color.WHITE, Layout.Alignment.ALIGN_CENTER)
        }
        y += headH

        // ردیف‌ها
        val probe = Pen(context, Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        items.forEachIndexed { idx, it ->
            val cells = listOf(
                (idx + 1).toString(), it.productName, it.karat.toString(), it.weight.toString(),
                fmt(it.gramPrice), fmt(it.feePerGram), fmt(it.lineTotal)
            )
            var rowH = 0f
            // ارتفاع ردیف = بلندترین سلول (اندازه‌گیری بدون رسم روی بوم اصلی)
            for (i in cells.indices) {
                rowH = maxOf(rowH, probe.text(cells[i], 0f, 0f, colW[i] - 6f, 10f, false, DARK, Layout.Alignment.ALIGN_CENTER))
            }
            rowH += 8f
            if (idx % 2 == 1) pen.rect(M, y, W - M, y + rowH, Color.parseColor("#F7F1DF"))
            var cx = W - M
            for (i in cells.indices) {
                cx -= colW[i]
                pen.text(cells[i], cx + 3f, y + 4f, colW[i] - 6f, 10f, i == 6, DARK, Layout.Alignment.ALIGN_CENTER)
            }
            y += rowH
            pen.line(y)
        }
        y += 14f

        // جمع‌ها (سمت چپ)
        val boxW = 250f
        val bx = M
        fun row(label: String, value: String, size: Float = 11f, bold: Boolean = false, color: Int = DARK) {
            pen.text(label, bx + boxW / 2f, y, boxW / 2f, size, bold, color, Layout.Alignment.ALIGN_NORMAL)
            val h = pen.text(value, bx, y, boxW / 2f - 6f, size, bold, color, Layout.Alignment.ALIGN_OPPOSITE)
            y += h + 4f
        }
        row("جمع اقلام:", fmt(invoice.subtotal) + " تومان")
        row("اجرت ساخت:", fmt(invoice.feeTotal) + " تومان")
        row("مالیات ارزش افزوده:", fmt(invoice.taxTotal) + " تومان")
        pen.line(y, GOLD); y += 4f
        row("مبلغ نهایی:", fmt(invoice.total) + " تومان", 14f, true, DARK)
        if (invoice.paidAmount < invoice.total || extras.remaining > 0) {
            row("نقد دریافتی:", fmt(invoice.paidAmount) + " تومان", 11f, false, DARK)
            row("مانده (نسیه):", fmt(extras.remaining) + " تومان", 12f, true, RED)
            if (extras.dueMillis > 0) row("سررسید:", Jalali.format(extras.dueMillis), 10f, false, GRAY)
        }
        y += 18f
        pen.line(y, GOLD)
        y += 8f
        y += pen.text("با تشکر از خرید شما", M, y, full, 11f, true, GOLD, Layout.Alignment.ALIGN_CENTER)
        return y + M
    }

    private fun measure(context: Context, inv: InvoiceEntity, items: List<InvoiceItemEntity>, ex: InvoiceExtras): Float {
        val dummy = Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
        return draw(context, dummy, inv, items, ex)
    }

    private fun outDir(context: Context): File = File(context.cacheDir, "pdfs").apply { mkdirs() }

    fun renderPdf(context: Context, inv: InvoiceEntity, items: List<InvoiceItemEntity>, ex: InvoiceExtras): File {
        val h = measure(context, inv, items, ex)
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(W.toInt(), maxOf(h.toInt() + 1, 842), 1).create())
        draw(context, page.canvas, inv, items, ex)
        doc.finishPage(page)
        val file = File(outDir(context), "invoice_" + inv.id + ".pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun renderPng(context: Context, inv: InvoiceEntity, items: List<InvoiceItemEntity>, ex: InvoiceExtras): File {
        val h = measure(context, inv, items, ex)
        val scale = 2.5f
        val bmp = Bitmap.createBitmap((W * scale).toInt(), (h * scale).toInt().coerceAtLeast(10), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        canvas.scale(scale, scale)
        draw(context, canvas, inv, items, ex)
        val file = File(outDir(context), "invoice_" + inv.id + ".png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        return file
    }
}
