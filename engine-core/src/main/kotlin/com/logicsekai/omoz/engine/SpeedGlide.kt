// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.engine

import kotlin.math.exp

/**
 * Turns sparse GPS speed fixes (typically 1 per second) into a continuous speed curve.
 *
 * Rising speed: two cascaded one-pole glides with a time constant tied to the GPS interval, so
 * the engine never surges once per fix (clearly audible on pure tones such as the turbine whine).
 *
 * Falling speed: the engine must follow braking right away, so the fall uses a very short time
 * constant, and while a deceleration is in progress the speed between fixes is extrapolated
 * along the measured slope instead of waiting for the next fix. A stop reaches 0 within a few
 * hundred milliseconds instead of gliding down for seconds.
 */
class SpeedGlide(gpsIntervalSeconds: Float) {
    private var interval = gpsIntervalSeconds.coerceIn(0.1f, 10f)
    private var tau = riseTau(interval)
    private var target = 0f
    private var slope = 0f
    private var sinceFix = 0f
    private var s1 = 0f
    private var s2 = 0f

    fun setInterval(gpsIntervalSeconds: Float) {
        interval = gpsIntervalSeconds.coerceIn(0.1f, 10f)
        tau = riseTau(interval)
    }

    /** A new GPS speed reading arrived. */
    fun onFix(speedKmh: Float) {
        val v = speedKmh.coerceAtLeast(0f)
        // Remember how fast we are slowing down, to keep following the braking until the next fix.
        slope = if (v < target && sinceFix > 0.05f) {
            ((v - target) / sinceFix).coerceAtLeast(-MAX_DECEL_KMH_PER_S)
        } else {
            0f
        }
        if (slope > -MIN_DECEL_KMH_PER_S) slope = 0f
        target = v
        sinceFix = 0f
    }

    /** Fade the target (used while the GPS fix is lost). */
    fun decayTarget(factor: Float) {
        target = (target * factor).let { if (it < 0.5f) 0f else it }
        slope = 0f
    }

    /** Advance by [dt] seconds and return the smoothed speed. */
    fun advance(dt: Float): Float {
        val d = dt.coerceIn(0f, 0.5f)
        sinceFix += d

        // Where the speed is now, extrapolating an ongoing deceleration for at most one interval.
        var goal = target
        if (slope < 0f) goal = (target + slope * minOf(sinceFix, interval)).coerceAtLeast(0f)
        if (goal < SNAP_KMH) goal = 0f

        if (goal < s1) {
            // Slowing down: follow almost immediately.
            val a = 1f - exp(-d / FALL_TAU)
            s1 += a * (goal - s1)
            s2 += a * (s1 - s2)
            if (s2 < s1) s2 = s1
        } else {
            val a = 1f - exp(-d / tau)
            s1 += a * (goal - s1)
            s2 += a * (s1 - s2)
        }
        if (goal == 0f && s2 < STOP_SNAP_KMH) { s1 = 0f; s2 = 0f }
        return s2
    }

    val value: Float get() = s2

    fun reset() { target = 0f; slope = 0f; sinceFix = 0f; s1 = 0f; s2 = 0f }

    private companion object {
        fun riseTau(interval: Float) = (interval * 0.35f).coerceIn(0.1f, 2f)
        const val FALL_TAU = 0.06f
        const val SNAP_KMH = 0.5f
        const val STOP_SNAP_KMH = 1.0f
        const val MIN_DECEL_KMH_PER_S = 2f
        const val MAX_DECEL_KMH_PER_S = 40f
    }
}
