// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.engine

import com.logicsekai.omoz.engine.synth.ElectricTractionSynth
import com.logicsekai.omoz.engine.synth.JetTurbineSynth
import com.logicsekai.omoz.engine.synth.PistonConfig
import com.logicsekai.omoz.engine.synth.PistonEngineSynth
import com.logicsekai.omoz.engine.synth.Resonance

/**
 * Registry of every built-in engine sound.
 *
 * Community: append your [EngineSound] to [all]. Keep ids stable once released because
 * the chosen id is persisted in user settings.
 */
object EngineCatalog {

    val jetTurbine: EngineSound = object : EngineSound {
        override val info = EngineInfo(
            id = "jet_turbine",
            name = "Jet Turbine",
            description = "Turbofan whine and exhaust roar that spools up with speed.",
            kind = EngineKind.TURBINE,
            idleRpm = 0,
            redlineRpm = 100, // shown as N1 %
            gears = 1,
            spoolUpSeconds = 1.6f,
            spoolDownSeconds = 2.6f,
            hasRevLimiter = false,
            specs = "Turbofan · N1 0–100%",
        )

        override fun createSynth(sampleRate: Int): EngineSynth = JetTurbineSynth(sampleRate)
    }

    /** Same turbine, whine only: the high "nging" without any air/roar noise. */
    val jetWhine: EngineSound = object : EngineSound {
        override val info = EngineInfo(
            id = "jet_whine",
            name = "Jet Whine",
            description = "Pure turbine whine that rises with speed, no wind or roar.",
            kind = EngineKind.TURBINE,
            idleRpm = 0,
            redlineRpm = 100,
            gears = 1,
            spoolUpSeconds = 1.6f,
            spoolDownSeconds = 2.6f,
            hasRevLimiter = false,
            specs = "Turbine whine · N1 0–100%",
        )

        override fun createSynth(sampleRate: Int): EngineSynth = JetTurbineSynth(
            sampleRate,
            seed = 0x3A7,
            airMix = 0f,
            whineGain = 2.4f,
            bladeIdleHz = 900f,
            bladeRangeHz = 4200f,
            lowGain = 0f, // only the high "nging"
        )
    }

    /** Same turbine, low hum only: the "ngung" without whine or air noise. */
    val jetHum: EngineSound = object : EngineSound {
        override val info = EngineInfo(
            id = "jet_hum",
            name = "Jet Hum",
            description = "Deep turbine hum that rises with speed, no high whine or wind.",
            kind = EngineKind.TURBINE,
            idleRpm = 0,
            redlineRpm = 100,
            gears = 1,
            spoolUpSeconds = 1.6f,
            spoolDownSeconds = 2.6f,
            hasRevLimiter = false,
            specs = "Turbine hum · N1 0–100%",
        )

        override fun createSynth(sampleRate: Int): EngineSynth = JetTurbineSynth(
            sampleRate,
            seed = 0x4B1,
            airMix = 0f,
            whineGain = 4.5f,
            bladeIdleHz = 900f,
            bladeRangeHz = 4200f,
            highGain = 0f, // only the low "ngung"
            lowBody = 0.35f,
        )
    }

    /** High-speed train traction motor: magnetic hum + VVVF inverter whine + gear whine. */
    val bulletTrain: EngineSound = object : EngineSound {
        override val info = EngineInfo(
            id = "bullet_train_motor",
            name = "Bullet Train Motor",
            description = "Electric traction motor and inverter whine of a high-speed train.",
            kind = EngineKind.ELECTRIC,
            idleRpm = 0,
            redlineRpm = 100,
            gears = 1,
            spoolUpSeconds = 0.6f,
            spoolDownSeconds = 0.9f,
            hasRevLimiter = false,
            specs = "Traction motor · VVVF inverter",
        )

        override fun createSynth(sampleRate: Int): EngineSynth = ElectricTractionSynth(sampleRate)
    }

    val vTwinCruiser: EngineSound = pistonSound(
        EngineInfo(
            id = "v_twin_cruiser",
            name = "V-Twin Cruiser",
            description = "Deep, lumpy 45° V-twin with the classic potato-potato idle.",
            kind = EngineKind.PISTON_4_STROKE,
            idleRpm = 850,
            redlineRpm = 5800,
            gears = 5,
            spoolUpSeconds = 0.22f,
            spoolDownSeconds = 0.45f,
            specs = "2 cyl · 45° V · 4-stroke",
        ),
        PistonConfig(
            cylinders = 2,
            strokes = 4,
            // 45° V-twin fires at 315° then 405° crank → uneven "potato-potato".
            firingOffsets = floatArrayOf(0f, 315f / 720f),
            idleRpm = 850,
            redlineRpm = 5800,
            pulseWidth = 0.11f,
            resonances = listOf(
                Resonance(68f, 1.1f, 1.4f),
                Resonance(240f, 2.2f, 0.7f),
                Resonance(780f, 3.0f, 0.18f),
            ),
            lowpassIdleHz = 420f,
            lowpassRedlineHz = 2200f,
            roughness = 0.22f,
            pulseNoise = 0.35f,
            hiss = 0.05f,
            drive = 1.6f,
            crackle = 0.35f,
            outputGain = 1.15f,
        ),
    )

    /**
     * Classic American big twin: 1340 cc, 45° V, single crank pin, solid lifters, open pipes and
     * a 4-speed box. Low, hunting idle; short-shifted around 3,000 rpm with a throttle cut, a
     * gearbox clunk and pops on the overrun.
     */
    val bigTwinClassic: EngineSound = pistonSound(
        EngineInfo(
            id = "big_twin_classic",
            name = "Big Twin Classic",
            description = "Old-school 1340 cc 45° V-twin with a hunting idle, open pipes and a clunky 4-speed.",
            kind = EngineKind.PISTON_4_STROKE,
            idleRpm = 750,
            redlineRpm = 5200,
            gears = 4,
            // Heavy flywheels: slow to rev, slow to settle.
            spoolUpSeconds = 0.3f,
            spoolDownSeconds = 0.55f,
            // Points ignition, no limiter.
            hasRevLimiter = false,
            // Riders short-shift these engines: ~2,100 rpm after the shift, ~3,400 rpm before it.
            gearRpmLow = 0.3f,
            gearRpmHigh = 0.6f,
            shiftSeconds = 0.32f,
            specs = "2 cyl · 1340 cc · 45° V · 4-speed",
        ),
        PistonConfig(
            cylinders = 2,
            strokes = 4,
            // Single crank pin: fires at 315° then 405° → the uneven "potato-potato".
            firingOffsets = floatArrayOf(0f, 315f / 720f),
            idleRpm = 750,
            redlineRpm = 5200,
            pulseWidth = 0.085f,
            resonances = listOf(
                Resonance(52f, 1.0f, 1.7f),
                Resonance(130f, 1.8f, 0.95f),
                Resonance(360f, 2.6f, 0.4f),
                Resonance(1150f, 3.2f, 0.12f),
            ),
            lowpassIdleHz = 360f,
            lowpassRedlineHz = 2000f,
            roughness = 0.3f,
            pulseNoise = 0.45f,
            hiss = 0.04f,
            drive = 1.9f,
            crackle = 0.55f,
            outputGain = 1.2f,
            idleLope = 0.07f,
            missChance = 0.06f,
            valveTick = 0.1f,
            shiftClunk = 0.9f,
            shiftDip = 0.35f,
        ),
    )

    val inlineFourSport: EngineSound = pistonSound(
        EngineInfo(
            id = "inline_four_sport",
            name = "Inline-4 Sport",
            description = "High-revving superbike scream up to 14,000 rpm.",
            kind = EngineKind.PISTON_4_STROKE,
            idleRpm = 1300,
            redlineRpm = 14000,
            gears = 6,
            spoolUpSeconds = 0.14f,
            spoolDownSeconds = 0.32f,
            specs = "4 cyl · inline · 4-stroke",
        ),
        PistonConfig(
            cylinders = 4,
            strokes = 4,
            firingOffsets = floatArrayOf(0f, 0.25f, 0.5f, 0.75f),
            idleRpm = 1300,
            redlineRpm = 14000,
            pulseWidth = 0.15f,
            resonances = listOf(
                Resonance(160f, 1.4f, 0.9f),
                Resonance(560f, 2.6f, 0.55f),
                Resonance(1650f, 3.5f, 0.22f),
            ),
            lowpassIdleHz = 900f,
            lowpassRedlineHz = 6500f,
            roughness = 0.07f,
            pulseNoise = 0.2f,
            hiss = 0.08f,
            drive = 1.1f,
            crackle = 0.45f,
            outputGain = 1.1f,
        ),
    )

    /**
     * Quarter-litre inline four (250 cc class): tiny pistons, 180° crank, revs to 17,000 rpm.
     * Thin, buzzy idle that turns into a high intake howl, a hard rev limiter, and quick-shifter
     * upshifts (a short ignition cut instead of a clutch shift).
     */
    val inlineFour250: EngineSound = pistonSound(
        EngineInfo(
            id = "inline_four_250",
            name = "Inline-4 250",
            description = "Quarter-litre inline four that screams to 17,000 rpm, with quick-shifter upshifts.",
            kind = EngineKind.PISTON_4_STROKE,
            idleRpm = 1600,
            redlineRpm = 17000,
            gears = 6,
            // Tiny reciprocating mass: revs up and down very fast.
            spoolUpSeconds = 0.09f,
            spoolDownSeconds = 0.22f,
            // Ridden high in the rev range: ~10,000 rpm after a shift, ~16,000 rpm before it.
            gearRpmLow = 0.55f,
            gearRpmHigh = 0.95f,
            // Quick-shifter: a short ignition cut.
            shiftSeconds = 0.09f,
            specs = "4 cyl · 250 cc · inline · 17,000 rpm",
        ),
        PistonConfig(
            cylinders = 4,
            strokes = 4,
            firingOffsets = floatArrayOf(0f, 0.25f, 0.5f, 0.75f),
            idleRpm = 1600,
            redlineRpm = 17000,
            pulseWidth = 0.17f,
            resonances = listOf(
                Resonance(210f, 1.4f, 0.7f),
                Resonance(700f, 2.4f, 0.6f),
                Resonance(2100f, 3.2f, 0.35f),
                Resonance(4200f, 4.0f, 0.12f),
            ),
            lowpassIdleHz = 1100f,
            lowpassRedlineHz = 9000f,
            roughness = 0.05f,
            pulseNoise = 0.18f,
            hiss = 0.12f,
            drive = 1.0f,
            crackle = 0.6f,
            // Airbox / intake howl that follows the rpm and peaks near the top.
            trackedResonanceMultiple = 2f,
            trackedResonanceQ = 4f,
            powerBandCenter = 0.85f,
            powerBandWidth = 0.3f,
            trackedResonanceGain = 0.6f,
            outputGain = 1.05f,
            valveTick = 0.04f,
            shiftClunk = 0.25f,
            shiftDip = 0.55f,
        ),
    )

    /** 400 cc class inline four: the 250's scream with more body, to 16,000 rpm. */
    val inlineFour400: EngineSound = pistonSound(
        EngineInfo(
            id = "inline_four_400",
            name = "Inline-4 400",
            description = "400 cc inline four: a fuller howl than the 250, to 16,000 rpm, with quick-shifter upshifts.",
            kind = EngineKind.PISTON_4_STROKE,
            idleRpm = 1500,
            redlineRpm = 16000,
            gears = 6,
            spoolUpSeconds = 0.1f,
            spoolDownSeconds = 0.24f,
            gearRpmLow = 0.5f,
            gearRpmHigh = 0.93f,
            shiftSeconds = 0.09f,
            specs = "4 cyl · 400 cc · inline · 16,000 rpm",
        ),
        PistonConfig(
            cylinders = 4,
            strokes = 4,
            firingOffsets = floatArrayOf(0f, 0.25f, 0.5f, 0.75f),
            idleRpm = 1500,
            redlineRpm = 16000,
            pulseWidth = 0.17f,
            resonances = listOf(
                Resonance(170f, 1.4f, 0.8f),
                Resonance(560f, 2.4f, 0.6f),
                Resonance(1700f, 3.2f, 0.32f),
                Resonance(3600f, 4.0f, 0.12f),
            ),
            lowpassIdleHz = 1000f,
            lowpassRedlineHz = 8000f,
            roughness = 0.05f,
            pulseNoise = 0.18f,
            hiss = 0.11f,
            drive = 1.1f,
            crackle = 0.6f,
            trackedResonanceMultiple = 2f,
            trackedResonanceQ = 4f,
            powerBandCenter = 0.82f,
            powerBandWidth = 0.3f,
            trackedResonanceGain = 0.55f,
            outputGain = 1.05f,
            valveTick = 0.04f,
            shiftClunk = 0.25f,
            shiftDip = 0.55f,
        ),
    )

    /** Middleweight (600 class) supersport four: deeper than the small fours, still revs to 16,000 rpm. */
    val inlineFour600: EngineSound = pistonSound(
        EngineInfo(
            id = "inline_four_600",
            name = "Inline-4 600",
            description = "Middleweight supersport four: hard-edged howl to 16,000 rpm and crisp quick-shifts.",
            kind = EngineKind.PISTON_4_STROKE,
            idleRpm = 1300,
            redlineRpm = 16000,
            gears = 6,
            spoolUpSeconds = 0.11f,
            spoolDownSeconds = 0.26f,
            gearRpmLow = 0.5f,
            gearRpmHigh = 0.93f,
            shiftSeconds = 0.09f,
            specs = "4 cyl · 636 cc · inline · 16,000 rpm",
        ),
        PistonConfig(
            cylinders = 4,
            strokes = 4,
            firingOffsets = floatArrayOf(0f, 0.25f, 0.5f, 0.75f),
            idleRpm = 1300,
            redlineRpm = 16000,
            pulseWidth = 0.16f,
            resonances = listOf(
                Resonance(140f, 1.3f, 0.9f),
                Resonance(470f, 2.2f, 0.65f),
                Resonance(1450f, 3.0f, 0.32f),
                Resonance(3100f, 4.0f, 0.12f),
            ),
            lowpassIdleHz = 900f,
            lowpassRedlineHz = 7500f,
            roughness = 0.06f,
            pulseNoise = 0.2f,
            hiss = 0.1f,
            drive = 1.2f,
            crackle = 0.65f,
            trackedResonanceMultiple = 2f,
            trackedResonanceQ = 3.5f,
            powerBandCenter = 0.8f,
            powerBandWidth = 0.3f,
            trackedResonanceGain = 0.5f,
            outputGain = 1.05f,
            valveTick = 0.035f,
            shiftClunk = 0.3f,
            shiftDip = 0.55f,
        ),
    )

    /** Litre-class superbike four: deep bark down low, howl to 14,000 rpm, loud overrun pops. */
    val inlineFour1000: EngineSound = pistonSound(
        EngineInfo(
            id = "inline_four_1000",
            name = "Inline-4 1000",
            description = "Litre superbike four: deep bark at low rpm, savage howl to 14,000 rpm, pops and quick-shifts.",
            kind = EngineKind.PISTON_4_STROKE,
            idleRpm = 1150,
            redlineRpm = 14000,
            gears = 6,
            spoolUpSeconds = 0.13f,
            spoolDownSeconds = 0.3f,
            gearRpmLow = 0.45f,
            gearRpmHigh = 0.9f,
            shiftSeconds = 0.08f,
            specs = "4 cyl · 998 cc · inline · 14,000 rpm",
        ),
        PistonConfig(
            cylinders = 4,
            strokes = 4,
            firingOffsets = floatArrayOf(0f, 0.25f, 0.5f, 0.75f),
            idleRpm = 1150,
            redlineRpm = 14000,
            pulseWidth = 0.16f,
            resonances = listOf(
                Resonance(95f, 1.2f, 1.1f),
                Resonance(330f, 2.0f, 0.75f),
                Resonance(1100f, 3.0f, 0.3f),
                Resonance(2600f, 4.0f, 0.1f),
            ),
            lowpassIdleHz = 750f,
            lowpassRedlineHz = 6000f,
            roughness = 0.07f,
            pulseNoise = 0.24f,
            hiss = 0.08f,
            drive = 1.5f,
            crackle = 0.75f,
            trackedResonanceMultiple = 2f,
            trackedResonanceQ = 3f,
            powerBandCenter = 0.75f,
            powerBandWidth = 0.3f,
            trackedResonanceGain = 0.35f,
            outputGain = 1.1f,
            valveTick = 0.03f,
            shiftClunk = 0.3f,
            shiftDip = 0.5f,
        ),
    )

    /** Supercharged litre four: superbike exhaust plus a supercharger whine that rises with rpm and boost. */
    val superchargedFour1000: EngineSound = pistonSound(
        EngineInfo(
            id = "supercharged_four_1000",
            name = "Supercharged 1000",
            description = "Supercharged litre four: superbike bark plus a rising supercharger whine and a chirp when you roll off.",
            kind = EngineKind.PISTON_4_STROKE,
            idleRpm = 1150,
            redlineRpm = 14000,
            gears = 6,
            spoolUpSeconds = 0.14f,
            spoolDownSeconds = 0.32f,
            gearRpmLow = 0.45f,
            gearRpmHigh = 0.9f,
            shiftSeconds = 0.08f,
            specs = "4 cyl · 998 cc · supercharged · 14,000 rpm",
        ),
        PistonConfig(
            cylinders = 4,
            strokes = 4,
            firingOffsets = floatArrayOf(0f, 0.25f, 0.5f, 0.75f),
            idleRpm = 1150,
            redlineRpm = 14000,
            pulseWidth = 0.16f,
            resonances = listOf(
                Resonance(90f, 1.2f, 1.1f),
                Resonance(310f, 2.0f, 0.75f),
                Resonance(1050f, 3.0f, 0.28f),
                Resonance(2500f, 4.0f, 0.1f),
            ),
            lowpassIdleHz = 750f,
            lowpassRedlineHz = 6000f,
            roughness = 0.06f,
            pulseNoise = 0.22f,
            hiss = 0.07f,
            drive = 1.4f,
            crackle = 0.5f,
            trackedResonanceMultiple = 2f,
            trackedResonanceQ = 3f,
            powerBandCenter = 0.75f,
            powerBandWidth = 0.3f,
            trackedResonanceGain = 0.3f,
            outputGain = 1.05f,
            valveTick = 0.03f,
            shiftClunk = 0.3f,
            shiftDip = 0.5f,
            superchargerWhine = 0.22f,
            superchargerRatio = 34f,
            blowOff = 0.5f,
        ),
    )

    val twoStrokeRacer: EngineSound = pistonSound(
        EngineInfo(
            id = "two_stroke_racer",
            name = "2-Stroke Racer",
            description = "Raspy single-cylinder two-stroke with a ring-ding power band.",
            kind = EngineKind.PISTON_2_STROKE,
            idleRpm = 1600,
            redlineRpm = 10500,
            gears = 5,
            spoolUpSeconds = 0.12f,
            spoolDownSeconds = 0.28f,
            specs = "1 cyl · expansion chamber · 2-stroke",
        ),
        PistonConfig(
            cylinders = 1,
            strokes = 2,
            firingOffsets = floatArrayOf(0f),
            idleRpm = 1600,
            redlineRpm = 10500,
            pulseWidth = 0.32f,
            resonances = listOf(
                Resonance(330f, 2.0f, 0.5f),
                Resonance(1150f, 3.0f, 0.35f),
                Resonance(2600f, 4.0f, 0.12f),
            ),
            lowpassIdleHz = 1800f,
            lowpassRedlineHz = 7000f,
            roughness = 0.25f,
            pulseNoise = 0.55f,
            hiss = 0.1f,
            drive = 2.2f,
            crackle = 0.6f,
            trackedResonanceMultiple = 3f,
            trackedResonanceQ = 6f,
            powerBandCenter = 0.72f,
            powerBandWidth = 0.22f,
            trackedResonanceGain = 1.2f,
            outputGain = 0.95f,
        ),
    )

    /** Order shown in the app. Add your sound here. */
    val all: List<EngineSound> = listOf(
        jetTurbine, jetWhine, jetHum, bulletTrain, vTwinCruiser, bigTwinClassic, inlineFourSport, inlineFour250, inlineFour400,
        inlineFour600, inlineFour1000, superchargedFour1000, twoStrokeRacer,
    )

    val default: EngineSound get() = jetTurbine

    fun byId(id: String?): EngineSound = all.firstOrNull { it.info.id == id } ?: default

    private fun pistonSound(info: EngineInfo, cfg: PistonConfig): EngineSound = object : EngineSound {
        override val info = if (info.cylinders == 0) info.copy(cylinders = cfg.cylinders) else info
        override fun createSynth(sampleRate: Int): EngineSynth =
            PistonEngineSynth(cfg, sampleRate, seed = info.id.hashCode() or 1)
    }
}
