// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app.engine

import android.os.SystemClock
import com.logicsekai.omoz.engine.EngineState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class GpsStatus { OFF, NO_PERMISSION, DISABLED, SEARCHING, FIX }

enum class StartReason { MANUAL, BLUETOOTH, PREVIEW, RESTART }

/**
 * Process-wide live state shared by [com.logicsekai.omoz.app.service.EngineService] (writer) and
 * the UI (reader). Throttle input flows the other way, from UI to service.
 */
object EngineRuntime {
    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _speedKmh = MutableStateFlow(0f)
    val speedKmh: StateFlow<Float> = _speedKmh.asStateFlow()

    private val _gps = MutableStateFlow(GpsStatus.OFF)
    val gps: StateFlow<GpsStatus> = _gps.asStateFlow()

    private val _engine = MutableStateFlow(EngineState())
    val engine: StateFlow<EngineState> = _engine.asStateFlow()

    /** Last [HISTORY_SIZE] rpm samples (0..1) paired with gear, for the RPM widget. */
    private val _history = MutableStateFlow(List(HISTORY_SIZE) { 0f to 0 })
    val history: StateFlow<List<Pair<Float, Int>>> = _history.asStateFlow()

    @Volatile var startReason: StartReason = StartReason.MANUAL
        internal set

    @Volatile private var throttleHeld = false
    @Volatile private var blipUntil = 0L

    fun setThrottleHeld(held: Boolean) { throttleHeld = held }

    /** Short full-throttle blip, used by the notification "Gas" action. */
    fun blip(durationMs: Long = 900L) { blipUntil = SystemClock.elapsedRealtime() + durationMs }

    fun throttleInput(): Float =
        if (throttleHeld || SystemClock.elapsedRealtime() < blipUntil) 1f else 0f

    // ---- written by the service ------------------------------------------------------
    internal fun setRunning(value: Boolean, reason: StartReason = startReason) {
        startReason = reason
        _running.value = value
        if (!value) {
            _engine.value = EngineState()
            _speedKmh.value = 0f
            _gps.value = GpsStatus.OFF
            throttleHeld = false
        }
    }

    internal fun publish(speed: Float, gps: GpsStatus, state: EngineState) {
        _speedKmh.value = speed
        _gps.value = gps
        _engine.value = state
    }

    internal fun pushHistory(rpm: Float, gear: Int) {
        _history.value = (_history.value.drop(1) + (rpm to gear))
    }

    const val HISTORY_SIZE = 7
}
