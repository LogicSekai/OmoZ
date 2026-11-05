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
    val all: List<EngineSound> = listOf(jetTurbine, jetWhine, jetHum, bulletTrain, vTwinCruiser, inlineFourSport, twoStrokeRacer)

    val default: EngineSound get() = jetTurbine

    fun byId(id: String?): EngineSound = all.firstOrNull { it.info.id == id } ?: default

    private fun pistonSound(info: EngineInfo, cfg: PistonConfig): EngineSound = object : EngineSound {
        override val info = info
        override fun createSynth(sampleRate: Int): EngineSynth =
            PistonEngineSynth(cfg, sampleRate, seed = info.id.hashCode() or 1)
    }
}
