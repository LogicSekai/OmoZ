// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.logicsekai.omoz.app.AppGraph
import com.logicsekai.omoz.app.engine.EngineRuntime
import com.logicsekai.omoz.app.engine.StartReason
import com.logicsekai.omoz.app.service.EngineService

/**
 * Manifest-registered receiver (ACL_CONNECTED/DISCONNECTED are exempt from the implicit
 * broadcast ban) so auto-start works even when OmoZ is not running.
 */
class BluetoothAutoReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val settings = AppGraph.settings(context).current
        if (!settings.bluetoothAutoStart) return
        val address = BluetoothHelper.deviceFromIntent(intent)?.address ?: return
        if (!settings.isRegistered(address)) return

        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                Log.i(TAG, "Registered device connected → starting engine")
                EngineService.start(context, StartReason.BLUETOOTH, fromForeground = false)
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                if (EngineRuntime.running.value && EngineRuntime.startReason == StartReason.BLUETOOTH) {
                    Log.i(TAG, "Registered device disconnected → stopping engine")
                    EngineService.stop(context)
                }
            }
        }
    }

    private companion object { const val TAG = "OmoZBt" }
}
