package com.example.goldshop

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.util.UUID

// خواندن وزن از ترازوی بلوتوثی (پروفایل سریال SPP). ترازو باید خروجی متنی مثل
// «ST,GS, 12.34 g» بفرستد؛ وزن وقتی ثبت می‌شود که ۳ بار پشت‌سرهم یکسان باشد.
object ScaleReader {
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val numberRegex = Regex("[-+]?\\d+(?:\\.\\d+)?")

    fun parseWeight(line: String): Double? {
        val v = numberRegex.find(line)?.value?.toDoubleOrNull() ?: return null
        return if (v > 0.0 && v < 100000.0) v else null
    }

    @SuppressLint("MissingPermission")
    fun readStable(device: BluetoothDevice, timeoutMs: Long = 20000L, onResult: (Double?, String) -> Unit) {
        Thread {
            var socket: BluetoothSocket? = null
            var weight: Double? = null
            var message = ""
            try {
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                val input = socket.inputStream
                val bytes = ByteArray(256)
                val buf = StringBuilder()
                val deadline = System.currentTimeMillis() + timeoutMs
                var last: Double? = null
                var same = 0
                var stable = false
                while (!stable && System.currentTimeMillis() < deadline) {
                    if (input.available() > 0) {
                        val n = input.read(bytes)
                        if (n < 0) break
                        buf.append(String(bytes, 0, n, Charsets.US_ASCII).replace('\r', '\n'))
                        var idx = buf.indexOf("\n")
                        while (idx >= 0 && !stable) {
                            val line = buf.substring(0, idx).trim()
                            buf.delete(0, idx + 1)
                            val w = parseWeight(line)
                            if (w != null) {
                                val l = last
                                if (l != null && Math.abs(w - l) < 0.0005) same++ else { last = w; same = 1 }
                                if (same >= 3) { stable = true; weight = w }
                            }
                            idx = buf.indexOf("\n")
                        }
                    } else {
                        Thread.sleep(50)
                    }
                }
                if (stable) {
                    message = "وزن پایدار دریافت شد"
                } else if (last != null) {
                    weight = last
                    message = "وزن ناپایدار بود؛ آخرین مقدار ثبت شد"
                } else {
                    message = "داده‌ای از ترازو دریافت نشد (خروجی متنی ترازو را بررسی کنید)"
                }
            } catch (t: Throwable) {
                message = "خطا در اتصال به ترازو: " + (t.message ?: "نامشخص")
            } finally {
                runCatching { socket?.close() }
            }
            onResult(weight, message)
        }.start()
    }
}

// اسکن بارکد/QR با Google Code Scanner (بدون نیاز به مجوز دوربین).
// اسکنرهای بلوتوثی/USB که مثل کیبورد کار می‌کنند هم مستقیم داخل فیلد بارکد تایپ می‌کنند.
object BarcodeScan {
    fun scan(context: Context, onResult: (String) -> Unit, onMessage: (String) -> Unit) {
        runCatching {
            GmsBarcodeScanning.getClient(context).startScan()
                .addOnSuccessListener { b ->
                    val v = b.rawValue
                    if (v.isNullOrBlank()) onMessage("کدی خوانده نشد") else onResult(v.trim())
                }
                .addOnFailureListener {
                    onMessage("اسکن انجام نشد (نیاز به Google Play Services و اینترنت برای بار اول): " + (it.message ?: ""))
                }
        }.onFailure { onMessage("اسکنر در دسترس نیست") }
    }
}
