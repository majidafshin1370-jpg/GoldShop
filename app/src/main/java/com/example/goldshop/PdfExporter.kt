package com.example.goldshop

import android.content.Context
import com.example.goldshop.data.InvoiceEntity
import com.example.goldshop.data.InvoiceItemEntity
import java.io.File

// برای سازگاری با کدهای قبلی؛ ساخت واقعی در InvoiceRenderer انجام می‌شود
object PdfExporter {
    fun export(
        context: Context,
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>,
        extras: InvoiceExtras = InvoiceExtras()
    ): File = InvoiceRenderer.renderPdf(context, invoice, items, extras)

    fun exportImage(
        context: Context,
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>,
        extras: InvoiceExtras = InvoiceExtras()
    ): File = InvoiceRenderer.renderPng(context, invoice, items, extras)
}
