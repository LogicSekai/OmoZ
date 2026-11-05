// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.logicsekai.omoz.app.AppGraph
import com.logicsekai.omoz.app.bluetooth.BluetoothHelper
import com.logicsekai.omoz.app.bluetooth.PairedDevice
import com.logicsekai.omoz.app.data.AppSettings
import com.logicsekai.omoz.app.data.RegisteredDevice
import com.logicsekai.omoz.app.engine.EngineRuntime
import com.logicsekai.omoz.app.engine.StartReason
import com.logicsekai.omoz.app.service.EngineService
import com.logicsekai.omoz.app.system.PermissionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppGraph.settings(app)

    val settings: StateFlow<AppSettings> = repo.settings
    val running = EngineRuntime.running
    val speed = EngineRuntime.speedKmh
    val gps = EngineRuntime.gps
    val engine = EngineRuntime.engine
    val history = EngineRuntime.history

    private val _permissions = MutableStateFlow(PermissionState.read(app))
    val permissions: StateFlow<PermissionState> = _permissions.asStateFlow()

    private val _paired = MutableStateFlow<List<PairedDevice>>(emptyList())
    val paired: StateFlow<List<PairedDevice>> = _paired.asStateFlow()

    private val _bluetoothOn = MutableStateFlow(false)
    val bluetoothOn: StateFlow<Boolean> = _bluetoothOn.asStateFlow()

    private val ctx get() = getApplication<Application>()

    /** Re-read permission & Bluetooth state (called on every resume). */
    fun refresh() {
        _permissions.value = PermissionState.read(ctx)
        refreshBluetooth()
    }

    fun refreshBluetooth() {
        _bluetoothOn.value = BluetoothHelper.isEnabled(ctx)
        _paired.value = BluetoothHelper.pairedDevices(ctx)
        BluetoothHelper.queryConnected(ctx) { connected ->
            _paired.value = BluetoothHelper.pairedDevices(ctx, connected)
            maybeAutoStart(connected)
        }
    }

    /** If a registered device is already connected when the app opens, start right away. */
    private fun maybeAutoStart(connected: Set<String>) {
        val s = repo.current
        if (s.bluetoothAutoStart && !running.value && connected.any { s.isRegistered(it) }) {
            EngineService.start(ctx, StartReason.BLUETOOTH, fromForeground = true)
        }
    }

    // ---- engine ------------------------------------------------------------------------
    fun toggleEngine() {
        if (running.value) EngineService.stop(ctx) else EngineService.start(ctx, StartReason.MANUAL, fromForeground = true)
    }

    fun startEngine(reason: StartReason = StartReason.MANUAL) {
        EngineService.start(ctx, reason, fromForeground = true)
    }

    fun stopEngine() = EngineService.stop(ctx)

    fun setThrottle(held: Boolean) {
        if (held && !running.value) startEngine()
        EngineRuntime.setThrottleHeld(held)
    }

    fun selectEngine(id: String) = repo.update { it.copy(engineId = id) }

    fun preview(id: String) {
        selectEngine(id)
        EngineService.start(ctx, StartReason.PREVIEW, fromForeground = true)
    }

    // ---- settings ----------------------------------------------------------------------
    fun update(transform: (AppSettings) -> AppSettings) = repo.update(transform)

    fun toggleDevice(device: PairedDevice) = repo.update { s ->
        val list = if (s.isRegistered(device.address)) {
            s.bluetoothDevices.filterNot { it.address.equals(device.address, ignoreCase = true) }
        } else {
            s.bluetoothDevices + RegisteredDevice(device.address, device.name)
        }
        s.copy(bluetoothDevices = list)
    }

    fun removeDevice(address: String) = repo.update { s ->
        s.copy(bluetoothDevices = s.bluetoothDevices.filterNot { it.address.equals(address, ignoreCase = true) })
    }

    fun setBluetoothAuto(enabled: Boolean) {
        repo.update { it.copy(bluetoothAutoStart = enabled) }
        if (enabled) refreshBluetooth()
    }
}
