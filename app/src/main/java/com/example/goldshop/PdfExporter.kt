package com.example.goldshop

import android.content.Context
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.goldshop.data.InvoiceEntity
import com.example.goldshop.data.InvoiceItemEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    fun export(context: Context, invoice: InvoiceEntity, items: List<InvoiceItemEntity>): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = TextPaint()

        var y = 40f
        fun line(text: String, size: Float = 13f, bold: Boolean = false) {
            paint.textSize = size
            paint.isFakeBoldText = bold
            val layout = StaticLayout.Builder
                .obtain(text, 0, text.length, paint, 515)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
            canvas.save()
            canvas.translate(40f, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height.toFloat() + 10f
        }

        line("فاکتور فروش طلا و جواهر", 18f, true)
        line("شماره فاکتور: " + invoice.id)
        line(
            "تاریخ: " + SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
                .format(Date(invoice.dateMillis))
        )
        line("مشتری: " + invoice.customerName)
        line("--------------------------------------------------", 11f)
        items.forEach { item ->
            line(
                item.productName + " | " + item.weight + " گرم | عیار " + item.karat +
                    " | " + formatNum(item.lineTotal) + " تومان"
            )
        }
        line("--------------------------------------------------", 11f)
        line("جمع اقلام: " + formatNum(invoice.subtotal) + " تومان")
        line("اجرت ساخت: " + formatNum(invoice.feeTotal) + " تومان")
        line("مالیات بر ارزش افزوده: " + formatNum(invoice.taxTotal) + " تومان")
        line("مبلغ نهایی: " + formatNum(invoice.total) + " تومان", 16f, true)

        document.finishPage(page)

        val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val file = File(dir, "invoice_" + invoice.id + ".pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun formatNum(n: Long): String =
        java.text.DecimalFormat("#,###").format(n)
}
