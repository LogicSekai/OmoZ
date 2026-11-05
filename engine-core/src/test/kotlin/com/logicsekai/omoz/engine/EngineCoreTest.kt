// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.engine

import com.logicsekai.omoz.engine.tools.PreviewRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EngineCoreTest {

    @Test
    fun everyEngineRendersFiniteBoundedAudio() {
        for (sound in EngineCatalog.all) {
            val pcm = PreviewRenderer.render(sound, sampleRate = 22050, seconds = 8f)
            var peak = 0f
            var energy = 0.0
            for (s in pcm) {
                assertTrue(s.isFinite(), "${sound.info.id} produced a non-finite sample")
                peak = maxOf(peak, kotlin.math.abs(s))
                energy += s * s
            }
            assertTrue(peak <= 1f, "${sound.info.id} exceeds full scale")
            assertTrue(energy / pcm.size > 1e-4, "${sound.info.id} is silent")
        }
    }

    @Test
    fun idsAreUniqueAndStable() {
        val ids = EngineCatalog.all.map { it.info.id }
        assertEquals(ids.size, ids.toSet().size, "Duplicate engine id")
        assertTrue(ids.all { it.matches(Regex("[a-z0-9_]+")) }, "Ids must be lowercase snake_case")
        assertTrue("jet_turbine" in ids, "Jet turbine must always ship")
    }

    @Test
    fun simulatorIdlesWhenStoppedAndRevsWithThrottle() {
        val sim = EngineSimulator(EngineCatalog.inlineFourSport.info)
        var st = EngineState()
        repeat(100) { st = sim.update(0.02f, 0f, 0f, 100f) }
        assertTrue(st.rpmNorm < 0.02f)
        assertEquals(0, st.gear)
        repeat(50) { st = sim.update(0.02f, 0f, 1f, 100f) }
        assertTrue(st.rpmNorm > 0.8f, "throttle should rev the engine, was ${st.rpmNorm}")
    }

    @Test
    fun simulatorShiftsUpThroughGears() {
        val sim = EngineSimulator(EngineCatalog.vTwinCruiser.info)
        var maxGear = 0
        var speed = 0f
        repeat(1000) {
            speed += 0.1f
            maxGear = maxOf(maxGear, sim.update(0.02f, speed, 0.5f, 100f).gear)
        }
        assertEquals(EngineCatalog.vTwinCruiser.info.gears, maxGear)
    }

    @Test
    fun turbineSpoolsSlowly() {
        val sim = EngineSimulator(EngineCatalog.jetTurbine.info)
        var st = EngineState()
        repeat(25) { st = sim.update(0.02f, 0f, 1f, 100f) } // 0.5 s
        assertTrue(st.rpmNorm < 0.5f, "turbine should not reach full spool in 0.5 s")
    }

    @Test
    fun gpsGlideHasNoStepsBetweenFixes() {
        // 1 Hz GPS, 20 Hz service tick: speed must change a little every tick, never jump.
        val g = SpeedGlide(1f)
        var prev = 0f
        var maxStep = 0f
        for (tick in 0 until 200) {
            if (tick % 20 == 0) g.onFix(tick / 20 * 8f) // +8 km/h per second
            val v = g.advance(0.05f)
            if (tick > 20) maxStep = maxOf(maxStep, v - prev)
            prev = v
        }
        assertTrue(maxStep < 1f, "speed jumped $maxStep km/h in one 50 ms tick")
    }

    @Test
    fun controlSmootherRemovesStaircase() {
        val sm = com.logicsekai.omoz.engine.dsp.ControlSmoother(0.03f, 44100)
        var prev = 0f
        var maxStep = 0f
        for (i in 0 until 44100) {
            val target = (i / 2205) * 0.05f // a step every 50 ms
            val v = sm.next(target)
            maxStep = maxOf(maxStep, v - prev)
            prev = v
        }
        assertTrue(maxStep < 0.0005f, "control value jumped $maxStep in one sample")
    }

    @Test
    fun turbineHasNoJumpWhenStartingToMove() {
        val sim = EngineSimulator(EngineCatalog.jetTurbine.info)
        var prev = 0f
        var maxStep = 0f
        var speed = 0f
        repeat(400) {
            speed += 0.05f // 1 km/h per second
            val r = sim.update(0.05f, speed, 0f, 80f).rpmNorm
            maxStep = maxOf(maxStep, r - prev)
            prev = r
        }
        assertTrue(maxStep < 0.01f, "turbine rpm jumped $maxStep")
    }
}
