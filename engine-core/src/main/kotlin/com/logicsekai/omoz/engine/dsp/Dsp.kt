// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.engine.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Small, allocation-free DSP building blocks used by every engine synth.
 * Everything here is pure Kotlin so it can be unit-tested on the JVM.
 */

const val TWO_PI = (2.0 * PI).toFloat()

/** Fast xorshift32 white-noise generator in the range [-1, 1]. */
class Noise(seed: Int = 0x1234567) {
    private var state = if (seed == 0) 0x1234567 else seed

    fun next(): Float {
        var x = state
        x = x xor (x shl 13)
        x = x xor (x ushr 17)
        x = x xor (x shl 5)
        state = x
        // Map signed int to [-1, 1]
        return x * 4.656613e-10f
    }

    /** Uniform value in [0, 1). */
    fun nextUnit(): Float = (next() + 1f) * 0.5f
}

/** One-pole low-pass filter. */
class OnePole {
    private var a = 1f
    private var y = 0f

    fun setCutoff(hz: Float, sampleRate: Int) {
        val f = hz.coerceIn(5f, sampleRate * 0.45f)
        a = (1.0 - exp(-2.0 * PI * f / sampleRate)).toFloat()
    }

    fun process(x: Float): Float {
        y += a * (x - y)
        return y
    }

    fun reset() { y = 0f }
}

/** DC blocking high-pass (removes the offset of unipolar pulse trains). */
class DcBlocker(private val r: Float = 0.995f) {
    private var x1 = 0f
    private var y1 = 0f

    fun process(x: Float): Float {
        val y = x - x1 + r * y1
        x1 = x
        y1 = y
        return y
    }

    fun reset() { x1 = 0f; y1 = 0f }
}

/** RBJ cookbook biquad (transposed direct form II). */
class Biquad {
    private var b0 = 1f
    private var b1 = 0f
    private var b2 = 0f
    private var a1 = 0f
    private var a2 = 0f
    private var z1 = 0f
    private var z2 = 0f

    /** Band-pass with 0 dB peak gain. */
    fun setBandPass(hz: Float, q: Float, sampleRate: Int) {
        val f = hz.coerceIn(10f, sampleRate * 0.45f)
        val w0 = 2.0 * PI * f / sampleRate
        val alpha = sin(w0) / (2.0 * q.coerceAtLeast(0.1f))
        val a0 = 1.0 + alpha
        b0 = (alpha / a0).toFloat()
        b1 = 0f
        b2 = (-alpha / a0).toFloat()
        a1 = (-2.0 * cos(w0) / a0).toFloat()
        a2 = ((1.0 - alpha) / a0).toFloat()
    }

    fun setLowPass(hz: Float, q: Float, sampleRate: Int) {
        val f = hz.coerceIn(10f, sampleRate * 0.45f)
        val w0 = 2.0 * PI * f / sampleRate
        val alpha = sin(w0) / (2.0 * q.coerceAtLeast(0.1f))
        val cw = cos(w0)
        val a0 = 1.0 + alpha
        b0 = (((1.0 - cw) / 2.0) / a0).toFloat()
        b1 = ((1.0 - cw) / a0).toFloat()
        b2 = b0
        a1 = (-2.0 * cw / a0).toFloat()
        a2 = ((1.0 - alpha) / a0).toFloat()
    }

    fun setHighPass(hz: Float, q: Float, sampleRate: Int) {
        val f = hz.coerceIn(10f, sampleRate * 0.45f)
        val w0 = 2.0 * PI * f / sampleRate
        val alpha = sin(w0) / (2.0 * q.coerceAtLeast(0.1f))
        val cw = cos(w0)
        val a0 = 1.0 + alpha
        b0 = (((1.0 + cw) / 2.0) / a0).toFloat()
        b1 = (-(1.0 + cw) / a0).toFloat()
        b2 = b0
        a1 = (-2.0 * cw / a0).toFloat()
        a2 = ((1.0 - alpha) / a0).toFloat()
    }

    fun process(x: Float): Float {
        val y = b0 * x + z1
        z1 = b1 * x - a1 * y + z2
        z2 = b2 * x - a2 * y
        return y
    }

    fun reset() { z1 = 0f; z2 = 0f }
}

/** Linear parameter ramp across one render block to avoid zipper noise. */
class Ramp(initial: Float = 0f) {
    var value = initial
        private set
    private var step = 0f
    private var remaining = 0

    fun setTarget(target: Float, frames: Int) {
        if (frames <= 0) { value = target; remaining = 0; step = 0f; return }
        step = (target - value) / frames
        remaining = frames
    }

    fun next(): Float {
        if (remaining > 0) {
            value += step
            remaining--
        }
        return value
    }

    fun snap(v: Float) { value = v; remaining = 0; step = 0f }
}

/** Cheap tanh-like soft clipper. */
fun softClip(x: Float): Float {
    val c = x.coerceIn(-3f, 3f)
    return c * (27f + c * c) / (27f + 9f * c * c)
}

/** Fast sine approximation for phase in [0, 1). */
fun fastSin01(phase: Float): Float = sin(TWO_PI * phase)

/** Gaussian-ish bell used to shape power bands, 1 at [center]. */
fun bell(x: Float, center: Float, width: Float): Float {
    val d = (x - center) / width
    return exp(-d * d)
}

/**
 * Two cascaded one-pole low-passes, advanced per sample. Control values (rpm, load, volume)
 * arrive from the app in steps (service tick 20 Hz, GPS 1 Hz); this turns those steps into a
 * smooth, kink-free curve so pitch sweeps never "staircase".
 */
class ControlSmoother(tauSeconds: Float, sampleRate: Int, initial: Float = 0f) {
    private val a = (1.0 - exp(-1.0 / (tauSeconds.coerceAtLeast(0.0005f) * sampleRate))).toFloat()
    private var s1 = initial
    private var s2 = initial

    fun next(target: Float): Float {
        s1 += a * (target - s1)
        s2 += a * (s1 - s2)
        return s2
    }

    val value: Float get() = s2

    fun snap(v: Float) { s1 = v; s2 = v }
}
