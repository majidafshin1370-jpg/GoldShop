package com.example.goldshop

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import com.example.goldshop.data.AppDatabase
import com.example.goldshop.data.isLedger
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// گزارش PDF ماهانه (ماه شمسی): فروش، کهنه، صندوق، سود، چک‌ها، طلب و بدهی
object MonthlyReport {

    fun reportsDir(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(base, "reports").apply { mkdirs() }
    }

    fun fileFor(context: Context, y: Int, m: Int): File =
        File(reportsDir(context), "report_%04d_%02d.pdf".format(y, m))

    fun existing(context: Context): List<File> =
        (reportsDir(context).listFiles() ?: emptyArray<File>())
            .filter { it.name.startsWith("report_") && it.name.endsWith(".pdf") }
            .sortedByDescending { it.name }

    private fun fmt(n: Long): String = DecimalFormat("#,###").format(n)
    private fun toman(n: Long): String = fmt(n) + " تومان"

    suspend fun generate(context: Context, y: Int, m: Int): File = withContext(Dispatchers.IO) {
        val db = AppDatabase.get(context)
        val start = Jalali.monthStart(y, m)
        val end = Jalali.monthEnd(y, m)
        val now = System.currentTimeMillis()

        val invoices = db.invoiceDao().allOnce().filter { it.dateMillis in start..end }.sortedBy { it.dateMillis }
        val oldGold = db.oldGoldDao().allOnce().filter { it.dateMillis in start..end }
        val txns = db.transactionDao().allOnce().filter { it.dateMillis in start..end && !it.isLedger() }
        val cheques = db.chequeDao().allOnce()
        val debts = db.debtDao().allOnce()
        val payments = db.debtDao().allPaymentsOnce()

        val salesTotal = invoices.sumOf { it.total }
        val feeTotal = invoices.sumOf { it.feeTotal }
        val taxTotal = invoices.sumOf { it.taxTotal }
        val income = txns.filter { it.type == "income" }.sumOf { it.amount }
        val expense = txns.filter { it.type == "expense" }.sumOf { it.amount }
        val profit = feeTotal + income - expense

        val w = PdfWriter()
        w.title("گزارش ماهانه فروشگاه طلا و جواهر")
        w.text(Jalali.monthLabel(y, m), 16f, bold = true)
        w.text(
            "از " + Jalali.format(start) + " تا " + Jalali.format(end) +
                " | تاریخ تهیه گزارش: " + Jalali.format(now),
            10f
        )

        w.section("خلاصه فروش")
        w.row("تعداد فاکتور", invoices.size.toString())
        w.row("جمع فروش", toman(salesTotal), bold = true)
        w.row("جمع اجرت ساخت", toman(feeTotal))
        w.row("جمع مالیات", toman(taxTotal))

        w.section("خرید طلای کهنه")
        w.row("تعداد خرید", oldGold.size.toString())
        w.row("جمع پرداختی", toman(oldGold.sumOf { it.total }))
        w.row("وزن خالص خریداری‌شده", "%.2f گرم".format(oldGold.sumOf { it.pureWeight }))

        w.section("صندوق و سود")
        w.row("درآمد ثبت‌شده", toman(income))
        w.row("هزینه ثبت‌شده", toman(expense))
        w.row("سود تقریبی (اجرت + درآمد − هزینه)", toman(profit), bold = true)

        // چک‌ها
        val cashedIn = cheques.filter { it.status == "cashed" && it.settledMillis in start..end }
        val bouncedIn = cheques.filter { it.status == "bounced" && it.settledMillis in start..end }
        w.section("چک‌ها")
        w.row("چک دریافتی وصول‌شده در این ماه", toman(cashedIn.filter { it.direction == "receivable" }.sumOf { it.amount }))
        w.row("چک پرداختی پاس‌شده در این ماه", toman(cashedIn.filter { it.direction == "payable" }.sumOf { it.amount }))
        w.row("چک‌های برگشتی این ماه", bouncedIn.size.toString() + " فقره / " + toman(bouncedIn.sumOf { it.amount }))
        val horizon = end + 30L * 86_400_000L
        val upcoming = cheques.filter { it.status == "pending" && it.dueMillis <= horizon }.sortedBy { it.dueMillis }
        if (upcoming.isNotEmpty()) {
            w.gap(6f)
            w.text("چک‌های در انتظار (سررسید گذشته یا تا ۳۰ روز پس از پایان ماه)", 11f, bold = true)
            w.header("نوع" to 70f, "نام" to 170f, "سررسید" to 110f, "مبلغ" to 165f)
            upcoming.forEach {
                w.cells(
                    (if (it.direction == "receivable") "دریافتی" else "پرداختی") to 70f,
                    it.partyName to 170f,
                    Jalali.format(it.dueMillis) to 110f,
                    toman(it.amount) to 165f
                )
            }
        }

        // طلب و بدهی
        val paidBy = payments.groupBy { it.debtId }.mapValues { (_, l) -> l.sumOf { p -> p.amount } }
        fun rem(d: com.example.goldshop.data.DebtEntity) = (d.amount - (paidBy[d.id] ?: 0L)).coerceAtLeast(0L)
        val openRecv = debts.filter { it.direction == "receivable" && rem(it) > 0 }.sortedByDescending { rem(it) }
        val openPay = debts.filter { it.direction == "payable" && rem(it) > 0 }.sortedByDescending { rem(it) }
        val debtDir = debts.associate { it.id to it.direction }
        val paymentsIn = payments.filter { it.dateMillis in start..end }
        w.section("طلب و بدهی مشتریان (مانده تا لحظه تهیه گزارش)")
        w.row("جمع مانده طلب از مشتریان", toman(openRecv.sumOf { rem(it) }), bold = true)
        w.row("جمع مانده بدهی به مشتریان", toman(openPay.sumOf { rem(it) }), bold = true)
        w.row("دریافتی از بدهکاران در این ماه", toman(paymentsIn.filter { debtDir[it.debtId] == "receivable" }.sumOf { it.amount }))
        w.row("پرداختی به بستانکاران در این ماه", toman(paymentsIn.filter { debtDir[it.debtId] == "payable" }.sumOf { it.amount }))
        fun debtTable(title: String, list: List<com.example.goldshop.data.DebtEntity>) {
            if (list.isEmpty()) return
            w.gap(6f)
            w.text(title, 11f, bold = true)
            w.header("نام" to 190f, "تاریخ" to 100f, "مانده" to 225f)
            list.take(40).forEach {
                w.cells(it.partyName to 190f, Jalali.format(it.dateMillis) to 100f, toman(rem(it)) to 225f)
            }
        }
        debtTable("بدهکاران (طلب من)", openRecv)
        debtTable("بستانکاران (بدهی من)", openPay)

        // فروش روزانه
        val daily = invoices.groupBy { Jalali.format(it.dateMillis) }
            .map { (d, l) -> Triple(d, l.size, l.sumOf { it.total }) }
            .sortedBy { it.first }
        if (daily.isNotEmpty()) {
            w.section("فروش روزانه")
            w.header("تاریخ" to 150f, "تعداد فاکتور" to 120f, "جمع فروش" to 245f)
            daily.forEach { (d, c, t) -> w.cells(d to 150f, c.toString() to 120f, toman(t) to 245f) }
        }

        // فهرست فاکتورها
        if (invoices.isNotEmpty()) {
            w.section("فهرست فاکتورها")
            w.header("شماره" to 60f, "تاریخ" to 100f, "مشتری" to 175f, "مبلغ" to 180f)
            invoices.forEach {
                w.cells(
                    it.id.toString() to 60f,
                    Jalali.format(it.dateMillis) to 100f,
                    it.customerName to 175f,
                    toman(it.total) to 180f
                )
            }
        }

        val file = fileFor(context, y, m)
        w.save(file)
        file
    }

    // نویسنده ساده PDF چندصفحه‌ای با چیدمان راست‌به‌چپ
    private class PdfWriter {
        private val pageW = 595
        private val pageH = 842
        private val right = 555f
        private val left = 40f
        private val contentW = 515
        private val doc = PdfDocument()
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var pageNo = 0
        private var y = 0f
        private val paint = TextPaint(Paint.ANTI_ALIAS_FLAG)

        init { newPage() }

        private fun layout(text: String, size: Float, bold: Boolean, width: Int, opposite: Boolean, color: Int): StaticLayout {
            paint.textSize = size
            paint.isFakeBoldText = bold
            paint.color = color
            return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(if (opposite) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .build()
        }

        private fun finishPage() {
            val c = canvas ?: return
            val footer = layout("صفحه " + pageNo, 9f, false, contentW, false, Color.GRAY)
            c.save(); c.translate(left, pageH - 35f); footer.draw(c); c.restore()
            doc.finishPage(page)
        }

        private fun newPage() {
            if (page != null) finishPage()
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create())
            canvas = page!!.canvas
            y = 40f
        }

        private fun ensure(h: Float) { if (y + h > pageH - 55f) newPage() }

        fun gap(h: Float) { y += h }

        fun title(t: String) = text(t, 20f, bold = true)

        fun text(t: String, size: Float = 12f, bold: Boolean = false) {
            val l = layout(t, size, bold, contentW, false, Color.BLACK)
            ensure(l.height + 6f)
            canvas!!.save(); canvas!!.translate(left, y); l.draw(canvas!!); canvas!!.restore()
            y += l.height + 6f
        }

        fun section(t: String) {
            ensure(60f)
            y += 10f
            val l = layout(t, 14f, true, contentW, false, Color.rgb(0x8A, 0x5A, 0x00))
            canvas!!.save(); canvas!!.translate(left, y); l.draw(canvas!!); canvas!!.restore()
            y += l.height + 2f
            val p = Paint().apply { color = Color.rgb(0xC9, 0x9A, 0x2E); strokeWidth = 1.2f }
            canvas!!.drawLine(left, y, right, y, p)
            y += 6f
        }

        fun row(label: String, value: String, bold: Boolean = false) {
            val lw = 300
            val vw = contentW - lw
            val a = layout(label, 12f, bold, lw, false, Color.BLACK)
            val b = layout(value, 12f, bold, vw, true, Color.BLACK)
            val h = maxOf(a.height, b.height) + 4f
            ensure(h)
            canvas!!.save(); canvas!!.translate(right - lw, y); a.draw(canvas!!); canvas!!.restore()
            canvas!!.save(); canvas!!.translate(left, y); b.draw(canvas!!); canvas!!.restore()
            y += h
        }

        // سلول‌ها از راست به چپ چیده می‌شوند
        fun cells(vararg cols: Pair<String, Float>, bold: Boolean = false, size: Float = 10.5f) {
            val layouts = cols.map { (t, wd) -> layout(t, size, bold, (wd - 6f).toInt(), false, Color.BLACK) }
            val h = (layouts.maxOfOrNull { it.height } ?: 0) + 4f
            ensure(h)
            var x = right
            layouts.forEachIndexed { i, l ->
                val wd = cols[i].second
                canvas!!.save(); canvas!!.translate(x - wd, y); l.draw(canvas!!); canvas!!.restore()
                x -= wd
            }
            y += h
        }

        fun header(vararg cols: Pair<String, Float>) {
            cells(*cols, bold = true)
            val p = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.8f }
            canvas!!.drawLine(left, y - 1f, right, y - 1f, p)
        }

        fun save(file: File) {
            finishPage()
            page = null
            FileOutputStream(file).use { doc.writeTo(it) }
            doc.close()
        }
    }
}
