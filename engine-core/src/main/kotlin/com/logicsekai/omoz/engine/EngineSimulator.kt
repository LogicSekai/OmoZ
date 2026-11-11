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
    /** 1 when a gear change starts, fading to 0 when it is done (see [EngineInfo.shiftSeconds]). */
    val shift: Float = 0f,
)

/**
 * Turns vehicle speed (from GPS) and the virtual throttle into engine RPM and load.
 *
 * Pure logic, no Android dependencies — see EngineSimulatorTest.
 */
class EngineSimulator(info: EngineInfo) {

    var info: EngineInfo = info
        set(value) { field = value; gear = 0; shiftTimer = 0f; virtualSpeed = 0f; holdTime = 0f }

    private var rpm = 0f
    private var load = 0f
    private var throttle = 0f
    private var gear = 0
    private var lastSpeed = 0f
    private var accel = 0f
    private var limiterTimer = 0f
    private var shiftTimer = 0f
    /** Speed of the "virtual ride" while the throttle is held standing still (geared engines). */
    private var virtualSpeed = 0f
    private var holdTime = 0f
    private var dropTimer = 0f

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

        // Virtual ride: holding the throttle while standing still (testing at home, waiting at a
        // light) pulls away through the gears instead of only revving in neutral. A short blip
        // still revs in neutral; releasing the throttle coasts back down to N.
        val realSpeed = speedKmh.coerceAtLeast(0f)
        val fullScale = fullScaleSpeedKmh.coerceAtLeast(10f)
        if (info.gears > 1 && realSpeed < MOVING_THRESHOLD_KMH && throttleInput > 0.5f) {
            holdTime += d
            if (holdTime > VIRTUAL_RIDE_DELAY_SECONDS) {
                virtualSpeed = (virtualSpeed + fullScale * VIRTUAL_ACCEL_PER_SECOND * d).coerceAtMost(fullScale)
            }
        } else {
            holdTime = 0f
            virtualSpeed = (virtualSpeed - fullScale * VIRTUAL_DECEL_PER_SECOND * d).coerceAtLeast(0f)
        }
        val speed = maxOf(realSpeed, virtualSpeed)
        val rawAccel = (speed - lastSpeed) / d
        lastSpeed = speed
        accel += (rawAccel - accel) * (1f - exp(-d / 0.8f))

        val moving = speed >= MOVING_THRESHOLD_KMH
        val frac = (speed / fullScale).coerceIn(0f, 1.1f)

        val continuous = info.gears <= 1
        val prevGear = gear
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

        // Gear change: clutch in, throttle closed for a moment, then the new gear engages.
        if (info.shiftSeconds > 0f) {
            if (moving && prevGear >= 1 && gear != prevGear) {
                shiftTimer = info.shiftSeconds
            } else if (shiftTimer > 0f) {
                shiftTimer = (shiftTimer - d).coerceAtLeast(0f)
            }
        }
        val shifting = shiftTimer > 0f
        // After an upshift the rpm falls quickly to the new gear (clutch slips in), even on throttle.
        if (moving && prevGear >= 1 && gear > prevGear) dropTimer = UPSHIFT_DROP_SECONDS
        else if (dropTimer > 0f) dropTimer = (dropTimer - d).coerceAtLeast(0f)

        // Throttle adds on top of the speed RPM (blended, so there is no jump between the two).
        // While the clutch is in for a gear change the throttle is closed.
        val thr = if (shifting) 0f else throttle
        var target = (speedRpm + thr * (1f - speedRpm) * if (moving) 0.35f else 1f).coerceAtMost(1f)

        // Rev limiter bounce for piston engines.
        if (info.hasRevLimiter) {
            if (limiterTimer > 0f) {
                limiterTimer -= d
                target = minOf(target, 0.88f)
            } else if (rpm > 0.985f && target > 0.97f) {
                limiterTimer = 0.07f
            }
        }

        // Off the throttle the engine follows the vehicle speed down right away (no long spool-down
        // tail after braking or stopping); a throttle blip still spools down naturally.
        val offThrottle = throttleInput < 0.05f && throttle < 0.05f
        val tau = when {
            target > rpm -> info.spoolUpSeconds
            offThrottle || shifting || dropTimer > 0f -> minOf(info.spoolDownSeconds, FOLLOW_DOWN_SECONDS)
            else -> info.spoolDownSeconds
        }
        rpm += (target - rpm) * (1f - exp(-d / tau.coerceAtLeast(0.01f)))
        rpm = rpm.coerceIn(0f, 1f)

        val accelLoad = (accel / 8f).coerceIn(0f, 1f)
        val cruise = if (moving) 0.18f else 0f
        val targetLoad = if (shifting) 0f else maxOf(throttle, accelLoad, cruise)
        load += (targetLoad - load) * (1f - exp(-d / if (shifting) 0.05f else 0.15f))

        val absRpm = (info.idleRpm + (info.redlineRpm - info.idleRpm) * rpm).toInt()
        val shift = if (info.shiftSeconds > 0f) shiftTimer / info.shiftSeconds else 0f
        return EngineState(rpmNorm = rpm, rpm = absRpm, load = load, gear = gear, throttle = throttle, shift = shift)
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
        val low = if (gear == 1) 0.05f else info.gearRpmLow
        return (low + (info.gearRpmHigh - low) * f).coerceAtMost(1f)
    }

    private fun bound(k: Int): Float = (k.toFloat() / info.gears).pow(1.25f)

    fun reset() {
        rpm = 0f; load = 0f; throttle = 0f; gear = 0; lastSpeed = 0f; accel = 0f; limiterTimer = 0f; shiftTimer = 0f
        virtualSpeed = 0f; holdTime = 0f; dropTimer = 0f
    }

    companion object {
        const val MOVING_THRESHOLD_KMH = 2.5f
        /** rpm time constant when slowing down off the throttle. */
        const val FOLLOW_DOWN_SECONDS = 0.12f
        /** Throttle held this long while standing still starts the virtual ride (a blip stays in N). */
        const val VIRTUAL_RIDE_DELAY_SECONDS = 1.0f
        /** Virtual ride acceleration / coast-down, as a fraction of the full-scale speed per second. */
        const val VIRTUAL_ACCEL_PER_SECOND = 0.11f
        const val VIRTUAL_DECEL_PER_SECOND = 0.3f
        const val UPSHIFT_DROP_SECONDS = 0.25f
    }
}
