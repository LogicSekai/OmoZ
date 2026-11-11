// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.engine

/**
 * Public contract for an engine sound. To add a new sound to OmoZ:
 *
 * 1. Implement [EngineSound] (usually by configuring [com.logicsekai.omoz.engine.synth.PistonEngineSynth]
 *    or writing a brand-new [EngineSynth]).
 * 2. Register it in [EngineCatalog.all].
 * 3. (Optional) add localized strings `engine_<id>_name` / `engine_<id>_desc` in the app module.
 *
 * See docs/ADDING_ENGINE_SOUND.md for a full walkthrough.
 */
interface EngineSound {
    val info: EngineInfo

    /** Create a fresh, independent synth instance. Called on the audio thread's owner. */
    fun createSynth(sampleRate: Int): EngineSynth
}

enum class EngineKind { TURBINE, PISTON_4_STROKE, PISTON_2_STROKE, ELECTRIC, OTHER }

/**
 * Static description of an engine. RPM values are only used for display and for the
 * simulator's gearbox; the synth itself works with a normalized RPM (0 = idle, 1 = redline).
 */
data class EngineInfo(
    /** Stable id, lowercase snake_case. Persisted in settings — never rename once released. */
    val id: String,
    /** English fallback name (the app may override it with a localized string). */
    val name: String,
    /** English fallback one-line description. */
    val description: String,
    val kind: EngineKind,
    val idleRpm: Int,
    val redlineRpm: Int,
    /** Number of virtual gears. 1 = continuous (turbines, EV, CVT scooters). */
    val gears: Int,
    /** Seconds to approach a higher RPM target (time constant). Turbines spool slowly. */
    val spoolUpSeconds: Float,
    /** Seconds to approach a lower RPM target (time constant). */
    val spoolDownSeconds: Float,
    /** Whether the simulator should bounce off a rev limiter at redline. */
    val hasRevLimiter: Boolean = true,
    /** Geared engines: normalized rpm right after an upshift (where a gear 2+ starts). */
    val gearRpmLow: Float = 0.42f,
    /** Geared engines: normalized rpm at which the virtual gearbox shifts up. */
    val gearRpmHigh: Float = 0.9f,
    /**
     * Geared engines: how long a gear change takes (clutch in, throttle closed, clutch out).
     * During it the load drops to zero and [SynthParams.shift] tells the synth to play the
     * shift. 0 = instant shifts without a throttle cut.
     */
    val shiftSeconds: Float = 0f,
    /** Number of cylinders (0 = not a piston engine). Used to group sounds in the app. */
    val cylinders: Int = 0,
    /** Short spec line, e.g. "2 cyl · 45° V · 4-stroke". */
    val specs: String = "",
    /** Community credit. */
    val author: String = "OmoZ",
)

/** Parameters handed to the synth for every render block. */
class SynthParams {
    /** 0 = idle, 1 = redline / full spool. */
    @JvmField var rpm: Float = 0f
    /** 0..1 engine load (throttle / acceleration). Adds grit and brightness. */
    @JvmField var load: Float = 0f
    /** 0..1 output gain. */
    @JvmField var volume: Float = 1f
    /**
     * Gear change in progress: 1 the moment a gear changes, fading to 0 when the clutch is
     * back out. Synths may play a gearbox clunk and dip the exhaust. Always 0 for engines
     * without [EngineInfo.shiftSeconds].
     */
    @JvmField var shift: Float = 0f
}

/** A real-time sound generator. Implementations must not allocate inside [render]. */
interface EngineSynth {
    /**
     * Render [frames] mono samples into [out] (values roughly within [-1, 1]).
     * Parameter changes must be smoothed internally across the block.
     */
    fun render(out: FloatArray, frames: Int, params: SynthParams)

    fun reset()
}
