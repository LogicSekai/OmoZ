// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.engine.synth

import com.logicsekai.omoz.engine.EngineSynth
import com.logicsekai.omoz.engine.SynthParams
import com.logicsekai.omoz.engine.dsp.Biquad
import com.logicsekai.omoz.engine.dsp.DcBlocker
import com.logicsekai.omoz.engine.dsp.Noise
import com.logicsekai.omoz.engine.dsp.OnePole
import com.logicsekai.omoz.engine.dsp.ControlSmoother
import com.logicsekai.omoz.engine.dsp.bell
import com.logicsekai.omoz.engine.dsp.softClip
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** A fixed exhaust/body resonance (band-pass) mixed into the output. */
data class Resonance(val hz: Float, val q: Float, val gain: Float)

/**
 * Everything needed to describe a piston engine. Tweak these numbers to create a new
 * motorcycle sound without writing any DSP code.
 */
data class PistonConfig(
    val cylinders: Int,
    /** 4 = four-stroke (fires every 2 revolutions), 2 = two-stroke (every revolution). */
    val strokes: Int,
    /** Firing position of each cylinder as a fraction of one engine cycle (0..1). */
    val firingOffsets: FloatArray,
    val idleRpm: Int,
    val redlineRpm: Int,
    /** Length of each exhaust pulse as a fraction of one engine cycle. */
    val pulseWidth: Float,
    val resonances: List<Resonance>,
    /** Final low-pass cutoff at idle (Hz). */
    val lowpassIdleHz: Float,
    /** Final low-pass cutoff at redline (Hz). */
    val lowpassRedlineHz: Float,
    /** Random amplitude variation per firing (0..1). Higher = lumpier, more "mechanical". */
    val roughness: Float,
    /** Noise mixed into each pulse (combustion texture). */
    val pulseNoise: Float,
    /** Intake / mechanical hiss amount. */
    val hiss: Float,
    /** Distortion drive at full load. */
    val drive: Float,
    /** Over-run pops & crackles when closing the throttle at high RPM (0..1). */
    val crackle: Float,
    /** Optional resonance that tracks firing frequency (2-stroke expansion chamber). 0 = off. */
    val trackedResonanceMultiple: Float = 0f,
    val trackedResonanceQ: Float = 5f,
    /** Where (normalized rpm) the tracked resonance is strongest — the "power band". */
    val powerBandCenter: Float = 0.7f,
    val powerBandWidth: Float = 0.25f,
    val trackedResonanceGain: Float = 0f,
    /** Overall output trim so all engines have similar loudness. */
    val outputGain: Float = 1f,
    /**
     * Idle "lope": slow, random wander of the idle speed as a fraction of [idleRpm]
     * (heavy flywheels and a carburettor). Fades out as the rpm rises. 0 = steady idle.
     */
    val idleLope: Float = 0f,
    /** Chance per cylinder and cycle of a weak firing at idle (old big twins stumble). */
    val missChance: Float = 0f,
    /** Valvetrain / primary-chain clatter that is not muffled by the exhaust (0..1). */
    val valveTick: Float = 0f,
    /** Gearbox clunk when a gear engages (needs [com.logicsekai.omoz.engine.EngineInfo.shiftSeconds]). */
    val shiftClunk: Float = 0f,
    /** How much the exhaust drops while the clutch is in during a gear change (0..1). */
    val shiftDip: Float = 0f,
    /** Supercharger whine level (0 = naturally aspirated). Rises with rpm and boost (load). */
    val superchargerWhine: Float = 0f,
    /** Whine pitch as a multiple of the crank speed. */
    val superchargerRatio: Float = 34f,
    /** Fluttering "chirp" when the throttle snaps shut under boost (0..1). */
    val blowOff: Float = 0f,
)

/**
 * Generic piston engine synthesizer.
 *
 * Model: a crank-cycle phase accumulator drives one smooth exhaust pulse per cylinder at its
 * firing offset. The unipolar pulse train is DC-blocked, coloured by a bank of band-pass
 * "exhaust" resonances, distorted with load and finally low-passed with an RPM-tracking filter.
 */
class PistonEngineSynth(
    private val cfg: PistonConfig,
    private val sampleRate: Int,
    seed: Int = 0x5EED,
) : EngineSynth {

    private val noise = Noise(seed)
    private val dc = DcBlocker(0.998f)
    private val directLp = OnePole()
    private val res = Array(cfg.resonances.size) { Biquad() }
    private val tracked = Biquad()
    private val hissBp = Biquad()
    private val outLp = Biquad()
    private val outLp2 = Biquad()
    private val popBp = Biquad()
    private val tickBp = Biquad()
    private val clunkBp = Biquad()
    private val chirpBp = Biquad()

    // Per-sample smoothing of control inputs (~30 ms per stage) — see ControlSmoother.
    private val rpmRamp = ControlSmoother(0.03f, sampleRate)
    private val loadRamp = ControlSmoother(0.03f, sampleRate)
    private val volRamp = ControlSmoother(0.02f, sampleRate)
    private val shiftRamp = ControlSmoother(0.02f, sampleRate)

    private var phase = 0f
    private val amp = FloatArray(cfg.cylinders) { 1f }
    private var lastRpm = 0f
    private var popEnv = 0f
    private var popDecay = 0f

    // Idle lope (slow random walk of the idle speed).
    private var lope = 0f
    private var lopeTarget = 0f
    private val lopeCoef = (1.0 - exp(-1.0 / (0.22 * sampleRate))).toFloat()

    // Valvetrain clatter: two mechanical events per cylinder and cycle.
    private val tickPos = FloatArray(cfg.cylinders * 2) { i ->
        val c = i / 2
        val p = cfg.firingOffsets[c] + if (i % 2 == 0) 0.38f else 0.71f
        p - p.toInt()
    }
    private var tickEnv = 0f
    private val tickDecay = exp(-1.0 / (0.004 * sampleRate)).toFloat()

    // Gearbox clunk: a low thud plus a short metallic knock.
    private var lastShift = 0f
    private var clunkEnv = 0f
    private var clunkPhase = 0f
    private val clunkDecay = exp(-1.0 / (0.07 * sampleRate)).toFloat()
    private var knockEnv = 0f
    private val knockDecay = exp(-1.0 / (0.012 * sampleRate)).toFloat()

    // Supercharger.
    private var whinePhase = 0f
    private var peakLoad = 0f
    private var chirpEnv = 0f
    private var flutterPhase = 0f
    private val chirpDecay = exp(-1.0 / (0.16 * sampleRate)).toFloat()

    init {
        cfg.resonances.forEachIndexed { i, r -> res[i].setBandPass(r.hz, r.q, sampleRate) }
        hissBp.setBandPass(2400f, 0.8f, sampleRate)
        popBp.setBandPass(900f, 1.2f, sampleRate)
        tickBp.setBandPass(3600f, 2.5f, sampleRate)
        clunkBp.setBandPass(1500f, 1.6f, sampleRate)
        chirpBp.setBandPass(2600f, 3f, sampleRate)
        directLp.setCutoff(900f, sampleRate)
        popDecay = exp(-1.0 / (0.018 * sampleRate)).toFloat()
    }

    override fun render(out: FloatArray, frames: Int, params: SynthParams) {
        val targetRpm = params.rpm.coerceIn(0f, 1.05f)
        val tRpm = targetRpm
        val tLoad = params.load.coerceIn(0f, 1f)
        val tVol = params.volume.coerceIn(0f, 1f)
        val tShift = params.shift.coerceIn(0f, 1f)
        if (cfg.shiftClunk > 0f && tShift > 0.5f && lastShift <= 0.5f) {
            clunkEnv = cfg.shiftClunk
            knockEnv = cfg.shiftClunk
            clunkPhase = 0f
        }
        lastShift = tShift
        // Blow-off chirp: the load (boost) collapses quickly at speed.
        if (cfg.blowOff > 0f) {
            peakLoad = maxOf(tLoad, peakLoad - 0.002f)
            if (peakLoad - tLoad > 0.35f && rpmRamp.value > 0.3f) {
                chirpEnv = cfg.blowOff
                peakLoad = tLoad
            }
        }
        val whineLimit = sampleRate * 0.45f

        // Per-block filter updates follow the *smoothed* values (no stepping).
        val n = rpmRamp.value
        val load = loadRamp.value
        val cutoff = cfg.lowpassIdleHz + (cfg.lowpassRedlineHz - cfg.lowpassIdleHz) * n +
            load * 0.25f * cfg.lowpassRedlineHz
        outLp.setLowPass(cutoff, 0.7f, sampleRate)
        outLp2.setLowPass(cutoff * 1.6f, 0.6f, sampleRate)

        val rpmNowHz = (cfg.idleRpm + (cfg.redlineRpm - cfg.idleRpm) * n) / 60f
        val firingHz = rpmNowHz * (if (cfg.strokes == 4) 0.5f else 1f) * cfg.cylinders
        if (cfg.trackedResonanceMultiple > 0f) {
            tracked.setBandPass(firingHz * cfg.trackedResonanceMultiple, cfg.trackedResonanceQ, sampleRate)
        }
        val trackedGain = cfg.trackedResonanceGain * (0.25f + 0.75f * bell(n, cfg.powerBandCenter, cfg.powerBandWidth))

        // Over-run crackle: throttle closed while RPM is dropping from high.
        val falling = lastRpm - targetRpm
        val crackleChance = if (cfg.crackle > 0f && load < 0.08f && targetRpm > 0.25f && falling > 0.0005f) {
            cfg.crackle * (falling * 40f).coerceAtMost(1f)
        } else 0f
        lastRpm = targetRpm

        val drive = 1f + cfg.drive * (0.3f + 0.7f * load)
        val driveComp = 1f / softClip(drive * 0.6f).coerceAtLeast(0.2f) * 0.6f
        val cycleScale = if (cfg.strokes == 4) 0.5f else 1f
        val rpmSpan = (cfg.redlineRpm - cfg.idleRpm).toFloat()
        val w = cfg.pulseWidth
        val invW = 1f / w

        for (i in 0 until frames) {
            val r = rpmRamp.next(tRpm)
            val l = loadRamp.next(tLoad)
            val v = volRamp.next(tVol)
            val sh = shiftRamp.next(tShift)

            // Idle lope fades out quickly once the engine is off idle.
            var rpm = cfg.idleRpm + rpmSpan * r
            if (cfg.idleLope > 0f) {
                lope += lopeCoef * (lopeTarget - lope)
                val idleWeight = (1f - r * 4f).coerceAtLeast(0f)
                rpm += cfg.idleRpm * lope * idleWeight
            }
            val prevPhase = phase
            val cycleHz = rpm / 60f * cycleScale
            phase += cycleHz / sampleRate
            if (phase >= 1f) {
                phase -= 1f
                // New engine cycle: re-roll per-cylinder combustion strength.
                val idleness = (1f - r * 4f).coerceAtLeast(0f) * (1f - l)
                for (c in 0 until cfg.cylinders) {
                    amp[c] = 1f - cfg.roughness * noise.nextUnit()
                    if (cfg.missChance > 0f && noise.nextUnit() < cfg.missChance * idleness) amp[c] *= 0.3f
                }
                if (cfg.idleLope > 0f) lopeTarget = cfg.idleLope * noise.next()
                if (crackleChance > 0f && noise.nextUnit() < crackleChance) {
                    popEnv = 0.6f + 0.4f * noise.nextUnit()
                }
            }

            var pulse = 0f
            for (c in 0 until cfg.cylinders) {
                var lp = phase - cfg.firingOffsets[c]
                if (lp < 0f) lp += 1f
                if (lp < w) {
                    val x = lp * invW
                    val s = sin(PI.toFloat() * x)
                    // Sharp attack, slower decay: skew the hump towards the front.
                    pulse += s * s * (1.15f - 0.6f * x) * amp[c]
                }
            }
            val nz = noise.next()
            val texture = pulse * (1f + cfg.pulseNoise * nz)

            val x = dc.process(texture)
            var y = directLp.process(x) * 0.8f
            for (k in res.indices) {
                y += res[k].process(x) * cfg.resonances[k].gain
            }
            if (cfg.trackedResonanceMultiple > 0f) {
                y += tracked.process(x) * trackedGain
            }
            // Intake / valvetrain hiss rises with rpm and load.
            y += hissBp.process(nz) * cfg.hiss * (0.2f + 0.5f * r + 0.6f * l)

            // Pops
            if (popEnv > 0.001f) {
                y += popBp.process(noise.next()) * popEnv * 3f
                popEnv *= popDecay
            }

            y = softClip(y * drive) * driveComp
            y = outLp2.process(outLp.process(y))
            if (cfg.shiftDip > 0f) y *= 1f - cfg.shiftDip * sh

            // Mechanical noise is not muffled by the exhaust, so it is added after the filters.
            if (cfg.valveTick > 0f) {
                for (k in tickPos.indices) {
                    val p = tickPos[k]
                    val crossed = if (phase >= prevPhase) p > prevPhase && p <= phase else p > prevPhase || p <= phase
                    if (crossed) tickEnv = 0.7f + 0.3f * noise.nextUnit()
                }
                val tick = tickBp.process(nz)
                if (tickEnv > 0.001f) {
                    y += tick * tickEnv * cfg.valveTick * (0.5f + 0.5f * r) * (1f - 0.6f * l)
                    tickEnv *= tickDecay
                }
            }
            if (clunkEnv > 0.001f) {
                clunkPhase += 72f / sampleRate
                if (clunkPhase >= 1f) clunkPhase -= 1f
                y += sin(2f * PI.toFloat() * clunkPhase) * clunkEnv * 0.55f
                clunkEnv *= clunkDecay
            }
            if (cfg.superchargerWhine > 0f) {
                val whineHz = (rpm / 60f * cfg.superchargerRatio).coerceAtMost(whineLimit)
                whinePhase += whineHz / sampleRate
                if (whinePhase >= 1f) whinePhase -= 1f
                val ang = 2f * PI.toFloat() * whinePhase
                val boost = (0.05f + 0.95f * r) * (0.15f + 0.85f * l)
                y += (sin(ang) + 0.3f * sin(2f * ang)) * cfg.superchargerWhine * boost
            }
            if (cfg.blowOff > 0f) {
                val chirp = chirpBp.process(noise.next())
                if (chirpEnv > 0.001f) {
                    flutterPhase += 22f / sampleRate
                    if (flutterPhase >= 1f) flutterPhase -= 1f
                    y += chirp * chirpEnv * (0.6f + 0.4f * sin(2f * PI.toFloat() * flutterPhase)) * 1.6f
                    chirpEnv *= chirpDecay
                }
            }
            if (cfg.shiftClunk > 0f) {
                val knock = clunkBp.process(noise.next())
                if (knockEnv > 0.001f) {
                    y += knock * knockEnv * 1.4f
                    knockEnv *= knockDecay
                }
            }
            val loudness = 0.62f + 0.38f * maxOf(r, l)
            out[i] = (y * cfg.outputGain * loudness * v).coerceIn(-1f, 1f)
        }
    }

    override fun reset() {
        dc.reset(); directLp.reset(); tracked.reset(); hissBp.reset(); outLp.reset(); outLp2.reset(); popBp.reset()
        tickBp.reset(); clunkBp.reset(); chirpBp.reset()
        whinePhase = 0f; peakLoad = 0f; chirpEnv = 0f; flutterPhase = 0f
        lope = 0f; lopeTarget = 0f; tickEnv = 0f; clunkEnv = 0f; knockEnv = 0f; lastShift = 0f
        shiftRamp.snap(0f)
        res.forEach { it.reset() }
        phase = 0f
        popEnv = 0f
        rpmRamp.snap(0f); loadRamp.snap(0f); volRamp.snap(0f)
    }
}
