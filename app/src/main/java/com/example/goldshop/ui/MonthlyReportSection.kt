package com.example.goldshop.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.goldshop.Jalali
import com.example.goldshop.MonthlyReport
import java.io.File
import kotlinx.coroutines.launch

private fun sharePdf(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    val share = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(share, "اشتراک گزارش PDF"))
}

private fun viewPdf(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    val view = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(view) }.onFailure { sharePdf(context, file) }
}

@Composable
fun MonthlyReportSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val today = remember { Jalali.today() }
    var year by remember { mutableStateOf(today.y) }
    var month by remember { mutableStateOf(today.m) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var files by remember { mutableStateOf(MonthlyReport.existing(context)) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("گزارش PDF ماهانه", fontWeight = FontWeight.Bold)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = {
                    if (month == 1) { month = 12; year -= 1 } else month -= 1
                }) { Text("ماه قبل") }
                Text(Jalali.monthLabel(year, month), fontWeight = FontWeight.Bold)
                TextButton(onClick = {
                    if (month == 12) { month = 1; year += 1 } else month += 1
                }) { Text("ماه بعد") }
            }
            Button(
                enabled = !busy,
                onClick = {
                    busy = true
                    error = ""
                    scope.launch {
                        try {
                            val f = MonthlyReport.generate(context, year, month)
                            files = MonthlyReport.existing(context)
                            sharePdf(context, f)
                        } catch (e: Exception) {
                            error = "خطا در ساخت گزارش: " + (e.message ?: e.javaClass.simpleName)
                        }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) "در حال ساخت…" else "ساخت و ارسال گزارش PDF این ماه") }
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)

            if (files.isNotEmpty()) {
                HorizontalDivider()
                Text("گزارش‌های ذخیره‌شده", style = MaterialTheme.typography.bodySmall)
                files.take(12).forEach { f ->
                    val parts = f.nameWithoutExtension.removePrefix("report_").split("_")
                    val label = if (parts.size == 2) {
                        val m = parts[1].toIntOrNull() ?: 1
                        Jalali.monthLabel(parts[0].toIntOrNull() ?: 0, m.coerceIn(1, 12))
                    } else f.name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label)
                        Row {
                            TextButton(onClick = { viewPdf(context, f) }) { Text("باز کردن") }
                            TextButton(onClick = { sharePdf(context, f) }) { Text("ارسال") }
                        }
                    }
                }
            }
        }
    }
}
