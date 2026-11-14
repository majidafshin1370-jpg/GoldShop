package com.example.goldshop.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.goldshop.Audit
import com.example.goldshop.BackupHelper
import com.example.goldshop.PinManager

private enum class PinMode { SET, CHANGE, REMOVE }

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var pinSet by remember { mutableStateOf(PinManager.hasPin(context)) }
    var status by remember { mutableStateOf("") }
    var pinMode by remember { mutableStateOf<PinMode?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            status = if (BackupHelper.export(context, it)) {
                Audit.logAsync(context, "تنظیمات", "خروجی پشتیبان", "")
                "فایل پشتیبان ذخیره شد"
            } else {
                "خطا در ذخیره پشتیبان"
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            status = if (BackupHelper.import(context, it)) {
                Audit.logAsync(context, "تنظیمات", "بازگردانی پشتیبان", "")
                "پشتیبان بازگردانی شد. برنامه را کاملاً ببندید و دوباره باز کنید."
            } else {
                "خطا در بازگردانی پشتیبان"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("امنیت", fontWeight = FontWeight.Bold)
        if (pinSet) {
            GoldButton(
                onClick = { pinMode = PinMode.CHANGE },
                modifier = Modifier.fillMaxWidth()
            ) { Text("تغییر رمز عبور") }
            OutlinedButton(
                onClick = { pinMode = PinMode.REMOVE },
                modifier = Modifier.fillMaxWidth()
            ) { Text("حذف رمز عبور") }
        } else {
            GoldButton(
                onClick = { pinMode = PinMode.SET },
                modifier = Modifier.fillMaxWidth()
            ) { Text("تعیین رمز عبور برای برنامه") }
        }

        HorizontalDivider()
        Text("پشتیبان‌گیری", fontWeight = FontWeight.Bold)
        GoldButton(
            onClick = { exportLauncher.launch("goldshop-backup.db") },
            modifier = Modifier.fillMaxWidth()
        ) { Text("خروجی پشتیبان از همه داده‌ها") }
        OutlinedButton(
            onClick = { importLauncher.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("بازگردانی پشتیبان") }

        CloudSection(onStatus = { status = it })

        ExtraSettings(onStatus = { status = it })

        if (status.isNotBlank()) {
            Text(status, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "نکته: رمز عبور با SHA-256 ذخیره می‌شود و در صورت فراموشی قابل بازیابی نیست؛ " +
                "پشتیبان را در جای امن نگه دارید.",
            style = MaterialTheme.typography.bodySmall
        )
    }

    pinMode?.let { mode ->
        PinDialog(
            mode = mode,
            onDismiss = { pinMode = null },
            onResult = { message ->
                status = message
                pinSet = PinManager.hasPin(context)
                pinMode = null
            }
        )
    }
}

@Composable
private fun PinDialog(mode: PinMode, onDismiss: () -> Unit, onResult: (String) -> Unit) {
    val context = LocalContext.current
    var current by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    val title = when (mode) {
        PinMode.SET -> "تعیین رمز عبور"
        PinMode.CHANGE -> "تغییر رمز عبور"
        PinMode.REMOVE -> "حذف رمز عبور"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (mode != PinMode.SET) {
                    OutlinedTextField(
                        value = current, onValueChange = { current = it },
                        label = { Text("رمز فعلی") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (mode != PinMode.REMOVE) {
                    OutlinedTextField(
                        value = newPin, onValueChange = { newPin = it },
                        label = { Text("رمز جدید (حداقل ۴ رقم)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirm, onValueChange = { confirm = it },
                        label = { Text("تکرار رمز جدید") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (error.isNotEmpty()) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when (mode) {
                    PinMode.SET -> {
                        if (newPin.length < 4) {
                            error = "رمز باید حداقل ۴ رقم باشد"
                        } else if (newPin != confirm) {
                            error = "تکرار رمز مطابق نیست"
                        } else {
                            PinManager.setPin(context, newPin)
                            Audit.logAsync(context, "تنظیمات", "تعیین رمز", "")
                            onResult("رمز عبور تعیین شد")
                        }
                    }
                    PinMode.CHANGE -> {
                        if (!PinManager.check(context, current)) {
                            error = "رمز فعلی اشتباه است"
                        } else if (newPin.length < 4) {
                            error = "رمز باید حداقل ۴ رقم باشد"
                        } else if (newPin != confirm) {
                            error = "تکرار رمز مطابق نیست"
                        } else {
                            PinManager.setPin(context, newPin)
                            onResult("رمز عبور تغییر کرد")
                        }
                    }
                    PinMode.REMOVE -> {
                        if (PinManager.check(context, current)) {
                            PinManager.clearPin(context)
                            Audit.logAsync(context, "تنظیمات", "حذف رمز", "")
                            onResult("رمز عبور حذف شد")
                        } else {
                            error = "رمز فعلی اشتباه است"
                        }
                    }
                }
            }) { Text("تایید") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
