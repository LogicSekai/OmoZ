// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.os.Build
import com.logicsekai.omoz.app.system.Permissions

data class PairedDevice(val address: String, val name: String, val connected: Boolean)

object BluetoothHelper {

    private fun adapter(context: Context): BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    fun isSupported(context: Context) = adapter(context) != null

    @SuppressLint("MissingPermission")
    fun isEnabled(context: Context): Boolean =
        Permissions.hasBluetooth(context) && runCatching { adapter(context)?.isEnabled == true }.getOrDefault(false)

    /** Bonded (paired) devices, name-sorted. Requires BLUETOOTH_CONNECT on Android 12+. */
    @SuppressLint("MissingPermission")
    fun pairedDevices(context: Context, connected: Set<String> = emptySet()): List<PairedDevice> {
        if (!Permissions.hasBluetooth(context)) return emptyList()
        val bonded = runCatching { adapter(context)?.bondedDevices }.getOrNull().orEmpty()
        return bonded.map {
            PairedDevice(
                address = it.address,
                name = runCatching { it.name }.getOrNull()?.takeIf { n -> n.isNotBlank() } ?: it.address,
                connected = it.address in connected,
            )
        }.sortedWith(compareByDescending<PairedDevice> { it.connected }.thenBy { it.name.lowercase() })
    }

    /**
     * Asynchronously collects addresses of devices currently connected over A2DP / headset
     * (speakers, helmets, earbuds, car units) and GATT. Calls [onResult] on the main thread.
     */
    @SuppressLint("MissingPermission")
    fun queryConnected(context: Context, onResult: (Set<String>) -> Unit) {
        val adapter = adapter(context)
        if (adapter == null || !Permissions.hasBluetooth(context) || !isEnabled(context)) {
            onResult(emptySet()); return
        }
        val profiles = listOf(BluetoothProfile.A2DP, BluetoothProfile.HEADSET)
        val found = mutableSetOf<String>()
        runCatching {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bm.getConnectedDevices(BluetoothProfile.GATT).forEach { found += it.address }
        }
        var pending = profiles.size
        val done = {
            pending--
            if (pending == 0) onResult(found.toSet())
        }
        profiles.forEach { profile ->
            val ok = runCatching {
                adapter.getProfileProxy(context.applicationContext, object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(p: Int, proxy: BluetoothProfile) {
                        runCatching { proxy.connectedDevices.forEach { found += it.address } }
                        runCatching { adapter.closeProfileProxy(p, proxy) }
                        done()
                    }

                    override fun onServiceDisconnected(p: Int) = Unit
                }, profile)
            }.getOrDefault(false)
            if (!ok) done()
        }
    }

    fun deviceFromIntent(intent: Intent): BluetoothDevice? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
}
