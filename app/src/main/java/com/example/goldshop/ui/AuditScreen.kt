package com.example.goldshop.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.goldshop.Jalali
import com.example.goldshop.data.AppDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// گزارش تغییرات: چه کسی، کی، چه چیزی را ثبت/ویرایش/حذف کرد
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val logs by db.auditDao().recent().collectAsState(initial = emptyList())
    var section by remember { mutableStateOf<String?>(null) }
    var actor by remember { mutableStateOf<String?>(null) }

    val sections = logs.map { it.section }.distinct()
    val actors = logs.map { it.actor }.distinct()
    val shown = logs.filter { (section == null || it.section == section) && (actor == null || it.actor == actor) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip(selected = section == null, onClick = { section = null }, label = { Text("همه بخش‌ها") })
            sections.forEach { s ->
                FilterChip(selected = section == s, onClick = { section = s }, label = { Text(s) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip(selected = actor == null, onClick = { actor = null }, label = { Text("همه کاربران") })
            actors.forEach { a ->
                FilterChip(selected = actor == a, onClick = { actor = a }, label = { Text(a) })
            }
        }
        if (shown.isEmpty()) {
            Text("هنوز تغییری ثبت نشده است", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(shown, key = { it.id }) { a ->
                LuxCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(a.actor + " • " + a.section + " • " + a.action, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Text(
                                Jalali.format(a.timeMillis) + " " + SimpleDateFormat("HH:mm", Locale.US).format(Date(a.timeMillis)),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(a.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
