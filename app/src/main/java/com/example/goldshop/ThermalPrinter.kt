package com.example.goldshop

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import java.util.UUID

// چاپ ESC/POS روی پرینتر حرارتی بلوتوثی (کاغذ ۵۸ میلی‌متر)
object ThermalPrinter {

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    fun print(device: BluetoothDevice, lines: List<String>, onResult: (Boolean, String) -> Unit) {
        Thread {
            try {
                val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                val out = socket.outputStream

                out.write(0x1B); out.write('@'.code)                 // مقداردهی اولیه
                out.write(0x1B); out.write('a'.code); out.write(1) // تراز وسط

                val content = buildString {
                    lines.forEach { append(it).append('\n') }
                    repeat(4) { append('\n') }
                }
                out.write(content.toByteArray(Charsets.UTF_8))

                out.write(0x1D); out.write('V'.code); out.write(0x30); out.write(0x00) // برش کاغذ
                out.flush()

                socket.close()
                onResult(true, "چاپ با موفقیت انجام شد")
            } catch (t: Throwable) {
                onResult(false, "خطا در چاپ: " + (t.message ?: "نامشخص"))
            }
        }.start()
    }
}
