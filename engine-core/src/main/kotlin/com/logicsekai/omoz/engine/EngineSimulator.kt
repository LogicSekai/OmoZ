// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.engine

import kotlin.math.exp
import kotlin.math.pow

/** Snapshot of the simulated engine, safe to show in the UI. */
data class EngineState(
    /** 0 = idle, 1 = redline. */
    val rpmNorm: Float = 0f,
    /** Absolute rpm (or N1 % for turbines). */
    val rpm: Int = 0,
    val load: Float = 0f,
    /** 0 = neutral/idle, 1..n = gear. Always 1 when moving on single-gear engines. */
    val gear: Int = 0,
    val throttle: Float = 0f,
)

/**
 * Turns vehicle speed (from GPS) and the virtual throttle into engine RPM and load.
 *
 * Pure logic, no Android dependencies — see EngineSimulatorTest.
 */
class EngineSimulator(info: EngineInfo) {

    var info: EngineInfo = info
        set(value) { field = value; gear = 0 }

    private var rpm = 0f
    private var load = 0f
    private var throttle = 0f
    private var gear = 0
    private var lastSpeed = 0f
    private var accel = 0f
    private var limiterTimer = 0f

    /**
     * @param dt seconds since last update
     * @param speedKmh current smoothed speed
     * @param throttleInput 0..1 from the on-screen throttle
     * @param fullScaleSpeedKmh speed at which the engine reaches (near) redline in top gear
     */
    fun update(dt: Float, speedKmh: Float, throttleInput: Float, fullScaleSpeedKmh: Float): EngineState {
        val d = dt.coerceIn(0.001f, 0.25f)

        // Throttle has its own short response so tapping feels snappy but not digital.
        val tTau = if (throttleInput > throttle) 0.12f else 0.22f
        throttle += (throttleInput.coerceIn(0f, 1f) - throttle) * (1f - exp(-d / tTau))

        val speed = speedKmh.coerceAtLeast(0f)
        val rawAccel = (speed - lastSpeed) / d
        lastSpeed = speed
        accel += (rawAccel - accel) * (1f - exp(-d / 0.8f))

        val moving = speed >= MOVING_THRESHOLD_KMH
        val frac = (speed / fullScaleSpeedKmh.coerceAtLeast(10f)).coerceIn(0f, 1.1f)

        val continuous = info.gears <= 1
        // Continuous engines (turbines, CVT) follow speed from 0 without any step; geared engines
        // idle in neutral until really moving.
        val speedRpm = when {
            continuous -> continuousRpm(frac)
            !moving -> 0f
            else -> gearedRpm(frac)
        }
        gear = when {
            !moving -> 0
            continuous -> 1
            else -> gear
        }

        // Throttle adds on top of the speed RPM (blended, so there is no jump between the two).
        var target = (speedRpm + throttle * (1f - speedRpm) * if (moving) 0.35f else 1f).coerceAtMost(1f)

        // Rev limiter bounce for piston engines.
        if (info.hasRevLimiter) {
            if (limiterTimer > 0f) {
                limiterTimer -= d
                target = minOf(target, 0.88f)
            } else if (rpm > 0.985f && target > 0.97f) {
                limiterTimer = 0.07f
            }
        }

        val tau = if (target > rpm) info.spoolUpSeconds else info.spoolDownSeconds
        rpm += (target - rpm) * (1f - exp(-d / tau.coerceAtLeast(0.01f)))
        rpm = rpm.coerceIn(0f, 1f)

        val accelLoad = (accel / 8f).coerceIn(0f, 1f)
        val cruise = if (moving) 0.18f else 0f
        val targetLoad = maxOf(throttle, accelLoad, cruise)
        load += (targetLoad - load) * (1f - exp(-d / 0.15f))

        val absRpm = (info.idleRpm + (info.redlineRpm - info.idleRpm) * rpm).toInt()
        return EngineState(rpmNorm = rpm, rpm = absRpm, load = load, gear = gear, throttle = throttle)
    }

    /** Turbines / CVT: smooth curve, no shifts. */
    private fun continuousRpm(frac: Float): Float = 0.95f * frac.coerceIn(0f, 1f).pow(0.85f)

    /** Virtual gearbox: lower gears are shorter, with shift hysteresis. */
    private fun gearedRpm(frac: Float): Float {
        val g = info.gears
        if (gear == 0) gear = 1
        while (gear < g && frac > bound(gear) + 0.01f) gear++
        while (gear > 1 && frac < bound(gear - 1) - 0.02f) gear--
        val lo = if (gear == 1) 0f else bound(gear - 1)
        val hi = bound(gear)
        val f = ((frac - lo) / (hi - lo)).coerceIn(0f, 1.15f)
        val low = if (gear == 1) 0.05f else 0.42f
        return (low + (0.9f - low) * f).coerceAtMost(1f)
    }

    private fun bound(k: Int): Float = (k.toFloat() / info.gears).pow(1.25f)

    fun reset() {
        rpm = 0f; load = 0f; throttle = 0f; gear = 0; lastSpeed = 0f; accel = 0f; limiterTimer = 0f
    }

    companion object {
        const val MOVING_THRESHOLD_KMH = 2.5f
    }
}
