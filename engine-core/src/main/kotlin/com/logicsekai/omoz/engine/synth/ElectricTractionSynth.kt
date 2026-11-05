// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.engine.synth

import com.logicsekai.omoz.engine.EngineSynth
import com.logicsekai.omoz.engine.SynthParams
import com.logicsekai.omoz.engine.dsp.Biquad
import com.logicsekai.omoz.engine.dsp.ControlSmoother
import com.logicsekai.omoz.engine.dsp.Noise
import com.logicsekai.omoz.engine.dsp.TWO_PI
import com.logicsekai.omoz.engine.dsp.softClip
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * High-speed train traction motor ("dinamo kereta cepat").
 *
 * Layers:
 *  - Motor magnetic hum: harmonics 2, 6 and 12 of the motor's electrical frequency, which is
 *    proportional to speed — the rising "wuuuuiiing".
 *  - VVVF inverter whine: PWM carrier tone ring-modulated by twice the electrical frequency
 *    (carrier ± 2·fe sidebands). Loud while pulling (load), fades while coasting, like a real
 *    train whose inverter idles when the driver releases power.
 *  - Gear mesh whine: a thin high tone ~22× the electrical frequency.
 *  - A touch of high-passed air at speed.
 *
 * The carrier rises smoothly (asynchronous PWM) so there are no audible pitch steps.
 */
class ElectricTractionSynth(
    private val sampleRate: Int,
    seed: Int = 0xE1E,
    /** Electrical frequency at full speed (Hz). */
    private val maxElectricalHz: Float = 180f,
    /** PWM carrier at standstill and its rise at full speed (Hz). */
    private val carrierIdleHz: Float = 750f,
    private val carrierRangeHz: Float = 1700f,
) : EngineSynth {

    private val noise = Noise(seed)
    private val airHp = Biquad()
    private val outLp = Biquad()

    private val rpmS = ControlSmoother(0.03f, sampleRate)
    private val loadS = ControlSmoother(0.08f, sampleRate) // inverter fades in/out gently
    private val volS = ControlSmoother(0.02f, sampleRate)

    private var phE = 0f   // motor electrical phase
    private var phC = 0f   // inverter carrier phase
    private var phG = 0f   // gear mesh phase
    private var phAux = 0f // auxiliary converter hum (audible at standstill)

    init {
        airHp.setHighPass(4000f, 0.7f, sampleRate)
        outLp.setLowPass(11000f, 0.7f, sampleRate)
    }

    override fun render(out: FloatArray, frames: Int, params: SynthParams) {
        val tRpm = params.rpm.coerceIn(0f, 1.05f)
        val tLoad = params.load.coerceIn(0f, 1f)
        val tVol = params.volume.coerceIn(0f, 1f)
        val sr = sampleRate.toFloat()

        for (i in 0 until frames) {
            val r = rpmS.next(tRpm)
            val l = loadS.next(tLoad)
            val v = volS.next(tVol)

            // Motor electrical frequency, never fully zero so the phase keeps moving smoothly.
            val fe = 3f + maxElectricalHz * r
            phE += fe / sr; if (phE >= 1f) phE -= 1f

            // Integer harmonics of phE stay continuous across the phase wrap.
            val presence = (r * 12f).coerceAtMost(1f) // fade in over the first few km/h
            val hum = presence * (0.30f * sin(TWO_PI * 2f * phE) +
                0.55f * sin(TWO_PI * 6f * phE) +
                0.28f * sin(TWO_PI * 12f * phE) * r)

            // VVVF carrier with ±2fe sidebands.
            val fc = carrierIdleHz + carrierRangeHz * r.coerceIn(0f, 1f).pow(1.3f)
            phC += fc / sr; if (phC >= 1f) phC -= 1f
            val carrier = sin(TWO_PI * phC)
            val inverter = (0.55f * carrier + 0.45f * carrier * cos(TWO_PI * 2f * phE)) *
                presence * (0.25f + 0.75f * l)

            // Gear mesh whine.
            phG += 22f * fe / sr; if (phG >= 1f) phG -= 1f
            val gear = 0.12f * r * sin(TWO_PI * phG)

            // Quiet auxiliary converter hum so the "train" is audible while standing.
            phAux += 360f / sr; if (phAux >= 1f) phAux -= 1f
            val aux = 0.06f * (1f - 0.7f * r) * (sin(TWO_PI * phAux) + 0.4f * sin(TWO_PI * 3f * phAux))

            val air = airHp.process(noise.next()) * 0.05f * r * r

            val level = 0.35f + 0.65f * r
            var y = (hum * 0.55f + inverter * 0.5f) * level + gear + aux + air
            y = softClip(y * 1.4f)
            y = outLp.process(y)
            out[i] = (y * 1.0f * v).coerceIn(-1f, 1f)
        }
    }

    override fun reset() {
        airHp.reset(); outLp.reset()
        phE = 0f; phC = 0f; phG = 0f; phAux = 0f
        rpmS.snap(0f); loadS.snap(0f); volS.snap(0f)
    }
}
