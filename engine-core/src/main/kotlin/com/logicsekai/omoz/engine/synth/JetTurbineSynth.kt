// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.engine.synth

import com.logicsekai.omoz.engine.EngineSynth
import com.logicsekai.omoz.engine.SynthParams
import com.logicsekai.omoz.engine.dsp.Biquad
import com.logicsekai.omoz.engine.dsp.Noise
import com.logicsekai.omoz.engine.dsp.OnePole
import com.logicsekai.omoz.engine.dsp.ControlSmoother
import com.logicsekai.omoz.engine.dsp.TWO_PI
import com.logicsekai.omoz.engine.dsp.softClip
import kotlin.math.sin

/**
 * Jet / turbofan synthesizer.
 *
 * Layers:
 *  - Compressor whine: three tones (blade-pass frequency + stage partials) that sweep with N1.
 *  - Exhaust roar: band-passed noise whose centre and level rise with spool.
 *  - Low rumble: heavily low-passed noise with slow turbulence modulation.
 *  - Hiss: high-passed noise for the "air" of the intake.
 */
class JetTurbineSynth(
    private val sampleRate: Int,
    seed: Int = 0x7E7,
    /** 0 = pure turbine whine ("nging"), 1 = full jet with roar/rumble/hiss of moving air. */
    private val airMix: Float = 1f,
    /** Gain of the compressor whine layer. */
    private val whineGain: Float = 1f,
    /** Blade-pass frequency at idle and its rise at full spool (Hz). */
    private val bladeIdleHz: Float = 700f,
    private val bladeRangeHz: Float = 3600f,
    /** Level of the high whine partials — the "nging". */
    private val highGain: Float = 1f,
    /** Level of the low partial (~1/4 of blade-pass) — the "ngung". */
    private val lowGain: Float = 1f,
    /** Extra 2nd harmonic on the low partial to give the "ngung" more body. */
    private val lowBody: Float = 0f,
) : EngineSynth {

    private val noise = Noise(seed)
    private val roarBp = Biquad()
    private val roarLp = Biquad()
    private val rumbleLp = Biquad()
    private val rumbleLp2 = OnePole()
    private val hissHp = Biquad()
    private val turbLfo = OnePole()
    private val outLp = Biquad()

    // Per-sample smoothing of control inputs (~30 ms per stage) — see ControlSmoother.
    private val rpmRamp = ControlSmoother(0.03f, sampleRate)
    private val loadRamp = ControlSmoother(0.03f, sampleRate)
    private val volRamp = ControlSmoother(0.02f, sampleRate)

    private var ph1 = 0f
    private var ph2 = 0f
    private var ph3 = 0f
    private var ph4 = 0f
    private var vibPh = 0f

    init {
        rumbleLp.setLowPass(110f, 0.8f, sampleRate)
        rumbleLp2.setCutoff(160f, sampleRate)
        hissHp.setHighPass(5000f, 0.7f, sampleRate)
        turbLfo.setCutoff(3f, sampleRate)
    }

    override fun render(out: FloatArray, frames: Int, params: SynthParams) {
        val tRpm = params.rpm.coerceIn(0f, 1.05f)
        val tLoad = params.load.coerceIn(0f, 1f)
        val tVol = params.volume.coerceIn(0f, 1f)
        // Block-rate filter settings follow the *smoothed* values, not the raw steps.
        val n = rpmRamp.value
        val load = loadRamp.value

        roarBp.setBandPass(350f + 1700f * n + 600f * load, 0.6f, sampleRate)
        roarLp.setLowPass(900f + 5200f * n, 0.7f, sampleRate)
        outLp.setLowPass(9000f + 4000f * n, 0.7f, sampleRate)

        val sr = sampleRate.toFloat()
        for (i in 0 until frames) {
            val r = rpmRamp.next(tRpm)
            val l = loadRamp.next(tLoad)
            val v = volRamp.next(tVol)

            // --- Compressor whine -------------------------------------------------------
            vibPh += 5.3f / sr
            if (vibPh >= 1f) vibPh -= 1f
            val wobble = 1f + 0.003f * sin(TWO_PI * vibPh) + 0.002f * noise.next()
            val fBlade = (bladeIdleHz + bladeRangeHz * r) * wobble
            ph1 += fBlade / sr; if (ph1 >= 1f) ph1 -= 1f
            ph2 += fBlade * 1.4983f / sr; if (ph2 >= 1f) ph2 -= 1f
            ph3 += fBlade * 0.2512f / sr; if (ph3 >= 1f) ph3 -= 1f
            ph4 += fBlade * 2.013f / sr; if (ph4 >= 1f) ph4 -= 1f
            val whineAmp = 0.09f + 0.11f * r
            val high = sin(TWO_PI * ph1) + 0.45f * sin(TWO_PI * ph2) + 0.18f * sin(TWO_PI * ph4) * r
            val low = (sin(TWO_PI * ph3) + lowBody * sin(TWO_PI * 2f * ph3)) * 0.8f * (0.4f + 0.6f * r)
            val whine = whineGain * whineAmp * (highGain * high + lowGain * low)

            // --- Noise layers -----------------------------------------------------------
            val wn = noise.next()
            val turb = 0.75f + 0.5f * (turbLfo.process(noise.next() * 6f)).coerceIn(-0.5f, 0.5f)
            val roar = roarLp.process(roarBp.process(wn)) * (0.7f + 1.2f * r + 0.8f * l) * turb
            val rumble = rumbleLp2.process(rumbleLp.process(wn)) * (3.0f + 2.6f * r + 2.5f * l) * turb
            val hiss = hissHp.process(wn) * (0.015f + 0.06f * r)

            var y = whine + airMix * (roar + rumble + hiss)
            y = softClip(y * (0.9f + 0.4f * l))
            y = outLp.process(y)
            out[i] = (y * 0.85f * v).coerceIn(-1f, 1f)
        }
    }

    override fun reset() {
        roarBp.reset(); roarLp.reset(); rumbleLp.reset(); rumbleLp2.reset(); hissHp.reset(); outLp.reset(); turbLfo.reset()
        ph1 = 0f; ph2 = 0f; ph3 = 0f; ph4 = 0f
        rpmRamp.snap(0f); loadRamp.snap(0f); volRamp.snap(0f)
    }
}
