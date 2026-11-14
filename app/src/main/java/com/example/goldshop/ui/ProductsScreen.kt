package com.example.goldshop.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import com.example.goldshop.Audit
import com.example.goldshop.BarcodeScan
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage
import com.example.goldshop.data.AppDatabase
import com.example.goldshop.data.ProductEntity
import kotlinx.coroutines.launch

@Composable
fun ProductsScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val products by db.productDao().all().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf<ProductEntity?>(null) }
    var editTarget by remember { mutableStateOf<ProductEntity?>(null) }

    val shown = products.filter {
        query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) ||
            it.barcode.contains(query.trim())
    }
    // جمع وزن موجودی = وزن هر محصول × تعداد موجود
    val totalWeight = products.sumOf { it.weight * it.quantity }
    val outOfStock = products.filter { it.quantity <= 0 }
    val lowStock = products.filter { it.quantity > 0 && it.minQuantity > 0 && it.quantity <= it.minQuantity }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LuxCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("تعداد محصول", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(products.size.toString(), fontWeight = FontWeight.Bold, color = Lux.GoldLight, style = MaterialTheme.typography.titleLarge)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("جمع وزن", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("%.2f گرم".format(totalWeight), fontWeight = FontWeight.Bold, color = Lux.GoldLight, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        if (outOfStock.isNotEmpty() || lowStock.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LuxCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (lowStock.isNotEmpty()) {
                        Text(
                            "هشدار کم‌بودن موجودی: " + lowStock.joinToString("، ") { it.name + " (" + it.quantity + ")" },
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (outOfStock.isNotEmpty()) {
                        Text(
                            "ناموجود: " + outOfStock.joinToString("، ") { it.name },
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("جستجوی محصول یا بارکد") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        GoldButton(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.size(6.dp))
            Text("افزودن محصول", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (shown.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GemLogo(size = 72.dp)
                Text(
                    if (products.isEmpty()) "هنوز محصولی ثبت نشده است" else "موردی پیدا نشد",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(shown, key = { it.id }) { p ->
                LuxCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editTarget = p }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Lux.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            if (p.imageUri != null) {
                                AsyncImage(
                                    model = p.imageUri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                GemLogo(size = 34.dp)
                            }
                        }
                        Spacer(modifier = Modifier.size(12.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(p.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "عیار " + p.karat,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1305),
                                    modifier = Modifier
                                        .background(Lux.goldBrush, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                                Text(p.weight.toString() + " گرم", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                "اجرت: " + formatNum(p.feeTomanPerGram) + " ت/گرم",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val stockLow = p.quantity <= 0 || (p.minQuantity > 0 && p.quantity <= p.minQuantity)
                            Text(
                                "موجودی: " + p.quantity + if (p.barcode.isNotBlank()) " | کد: " + p.barcode else "",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (stockLow) FontWeight.Bold else FontWeight.Normal,
                                color = if (stockLow) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                formatNum(p.price),
                                fontWeight = FontWeight.Bold,
                                color = Lux.GoldLight,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text("تومان", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            IconButton(onClick = { confirmDelete = p }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddProductDialog(
            onDismiss = { showAdd = false },
            onSave = { entity ->
                scope.launch {
                    db.productDao().upsert(entity)
                    Audit.log(context, "محصول", "افزودن", entity.name + " — موجودی " + entity.quantity)
                }
                showAdd = false
            }
        )
    }

    editTarget?.let { old ->
        AddProductDialog(
            initial = old,
            onDismiss = { editTarget = null },
            onSave = { entity ->
                scope.launch {
                    db.productDao().upsert(entity)
                    val changes = buildList {
                        if (old.quantity != entity.quantity) add("موجودی " + old.quantity + "←" + entity.quantity)
                        if (old.price != entity.price) add("قیمت " + old.price + "←" + entity.price)
                        if (old.weight != entity.weight) add("وزن " + old.weight + "←" + entity.weight)
                        if (old.feeTomanPerGram != entity.feeTomanPerGram) add("اجرت " + old.feeTomanPerGram + "←" + entity.feeTomanPerGram)
                        if (old.name != entity.name) add("نام " + old.name + "←" + entity.name)
                    }
                    Audit.log(context, "محصول", "ویرایش", entity.name + (if (changes.isEmpty()) "" else " — " + changes.joinToString("، ")))
                }
                editTarget = null
            }
        )
    }

    confirmDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("حذف محصول") },
            text = { Text("«" + p.name + "» حذف شود؟") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        db.productDao().delete(p)
                        Audit.log(context, "محصول", "حذف", p.name)
                    }
                    confirmDelete = null
                }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("انصراف") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit,
    initial: ProductEntity? = null
) {
    val context = LocalContext.current
    val karats = listOf(24, 21, 18)
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var weight by remember { mutableStateOf(initial?.weight?.toString() ?: "") }
    var karatIndex by remember {
        mutableIntStateOf(if (initial != null) karats.indexOf(initial.karat).let { if (it < 0) 2 else it } else 2)
    } // پیش‌فرض ۱۸
    var fee by remember { mutableStateOf(initial?.feeTomanPerGram?.toString() ?: "0") }
    var price by remember { mutableStateOf(initial?.price?.toString() ?: "") }
    var imageUri by remember { mutableStateOf<String?>(initial?.imageUri) }
    var quantity by remember { mutableStateOf((initial?.quantity ?: 1).toString()) }
    var minQuantity by remember { mutableStateOf((initial?.minQuantity ?: 0).toString()) }
    var barcode by remember { mutableStateOf(initial?.barcode ?: "") }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            imageUri = it.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "محصول جدید" else "ویرایش محصول") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GoldButton(onClick = { pickImage.launch("image/*") }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.size(4.dp))
                        Text("تصویر")
                    }
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام محصول") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("وزن (گرم)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                ScaleButton(onWeight = { weight = it.toString() })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    karats.forEachIndexed { i, k ->
                        FilterChip(
                            selected = karatIndex == i,
                            onClick = { karatIndex = i },
                            label = { Text(k.toString()) }
                        )
                    }
                }
                OutlinedTextField(
                    value = fee,
                    onValueChange = { fee = it },
                    label = { Text("اجرت (تومان/گرم)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("قیمت فروش (تومان)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("موجودی (تعداد)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minQuantity,
                        onValueChange = { minQuantity = it },
                        label = { Text("حد هشدار") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("بارکد / QR (اختیاری)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        BarcodeScan.scan(
                            context,
                            onResult = { barcode = it },
                            onMessage = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                        )
                    }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Text(" اسکن")
                    }
                    TextButton(onClick = {
                        barcode = "GS" + (System.currentTimeMillis() % 100000000L).toString().padStart(8, '0')
                    }) { Text("ساخت کد داخلی") }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        ProductEntity(
                            id = initial?.id ?: 0L,
                            name = name.trim(),
                            weight = weight.toDoubleOrNull() ?: 0.0,
                            karat = karats[karatIndex],
                            feeTomanPerGram = fee.toLongOrNull() ?: 0L,
                            price = price.toLongOrNull() ?: 0L,
                            imageUri = imageUri,
                            quantity = (quantity.toIntOrNull() ?: 0).coerceAtLeast(0),
                            minQuantity = (minQuantity.toIntOrNull() ?: 0).coerceAtLeast(0),
                            barcode = barcode.trim()
                        )
                    )
                },
                enabled = name.isNotBlank()
            ) { Text("ذخیره") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
