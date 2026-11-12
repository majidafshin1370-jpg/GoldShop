package com.example.goldshop

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build

object BluetoothHelper {

    @SuppressLint("MissingPermission")
    fun bondedDevices(context: Context): List<BluetoothDevice> {
        val adapter: BluetoothAdapter? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            manager.adapter
        } else {
            @Suppress("DEPRECATION")
            BluetoothAdapter.getDefaultAdapter()
        }
        return adapter?.bondedDevices?.toList() ?: emptyList()
    }
}
