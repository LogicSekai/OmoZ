// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.engine

import kotlin.math.exp

/**
 * Turns sparse GPS speed fixes (typically 1 per second) into a smooth, continuous speed curve.
 *
 * A plain "jump to the new value" makes the engine surge once per fix and then stall, which is
 * clearly audible on pure tones such as the turbine whine. Two cascaded one-pole glides with a
 * time constant tied to the GPS interval give a curve without steps *or* kinks.
 */
class SpeedGlide(gpsIntervalSeconds: Float) {
    private var tau = (gpsIntervalSeconds * 0.35f).coerceIn(0.1f, 2f)
    private var target = 0f
    private var s1 = 0f
    private var s2 = 0f

    fun setInterval(gpsIntervalSeconds: Float) { tau = (gpsIntervalSeconds * 0.35f).coerceIn(0.1f, 2f) }

    /** A new GPS speed reading arrived. */
    fun onFix(speedKmh: Float) { target = speedKmh.coerceAtLeast(0f) }

    /** Fade the target (used while the GPS fix is lost). */
    fun decayTarget(factor: Float) { target = (target * factor).let { if (it < 0.5f) 0f else it } }

    /** Advance by [dt] seconds and return the smoothed speed. */
    fun advance(dt: Float): Float {
        val a = 1f - exp(-dt.coerceIn(0f, 0.5f) / tau)
        s1 += a * (target - s1)
        s2 += a * (s1 - s2)
        if (target == 0f && s2 < 0.3f) { s1 = 0f; s2 = 0f }
        return s2
    }

    val value: Float get() = s2

    fun reset() { target = 0f; s1 = 0f; s2 = 0f }
}
