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

    @Test
    fun glideStopsRightAfterAZeroFix() {
        val g = SpeedGlide(1f)
        for (tick in 0 until 200) { if (tick % 20 == 0) g.onFix(40f); g.advance(0.05f) }
        g.onFix(0f)
        var t = 0f
        while (g.advance(0.05f) > 0f && t < 5f) t += 0.05f
        assertTrue(t <= 0.4f, "speed reached 0 only after $t s")
    }

    @Test
    fun glideFollowsBrakingBetweenFixes() {
        // 1 Hz GPS, braking 10 km/h per second: half a second after a fix the glide must already
        // be below that fix instead of waiting for the next one.
        val g = SpeedGlide(1f)
        for (tick in 0 until 200) { if (tick % 20 == 0) g.onFix(50f); g.advance(0.05f) }
        g.onFix(40f)
        repeat(20) { g.advance(0.05f) }
        g.onFix(30f)
        var v = 0f
        repeat(10) { v = g.advance(0.05f) }
        assertTrue(v < 28f, "glide lags braking: $v km/h")
        assertTrue(v > 20f, "glide over-extrapolated: $v km/h")
    }

    @Test
    fun engineFollowsAStopWithoutTail() {
        for (engine in listOf(EngineCatalog.jetTurbine, EngineCatalog.vTwinCruiser)) {
            val sim = EngineSimulator(engine.info)
            repeat(400) { sim.update(0.05f, 60f, 0f, 80f) }
            var st = EngineState()
            repeat(10) { st = sim.update(0.05f, 0f, 0f, 80f) } // 0.5 s after the speed hit 0
            assertTrue(st.rpmNorm < 0.08f, "${engine.info.id} still at rpm ${st.rpmNorm} 0.5 s after stopping")
        }
    }

    @Test
    fun bigTwinShiftsThroughFourGearsWithAThrottleCut() {
        val sim = EngineSimulator(EngineCatalog.bigTwinClassic.info)
        var speed = 0f
        var maxGear = 0
        var shifts = 0
        var lastGear = 0
        var minLoadWhileShifting = 1f
        var maxRpmMoving = 0f
        repeat(1400) {
            speed += 0.06f // 1.2 km/h per second at 20 Hz
            val st = sim.update(0.05f, speed, 0f, 100f)
            if (st.gear > lastGear && lastGear >= 1) shifts++
            lastGear = st.gear
            maxGear = maxOf(maxGear, st.gear)
            if (st.shift in 0.01f..0.5f) minLoadWhileShifting = minOf(minLoadWhileShifting, st.load)
            if (st.gear in 1..3) maxRpmMoving = maxOf(maxRpmMoving, st.rpmNorm)
        }
        assertEquals(4, maxGear)
        assertEquals(3, shifts)
        assertTrue(minLoadWhileShifting < 0.05f, "throttle not cut during a shift: $minLoadWhileShifting")
        assertTrue(maxRpmMoving < 0.7f, "big twin should short-shift, revved to $maxRpmMoving")
    }

    @Test
    fun enginesWithoutShiftTimeNeverReportAShift() {
        val sim = EngineSimulator(EngineCatalog.vTwinCruiser.info)
        var speed = 0f
        repeat(1000) {
            speed += 0.1f
            assertEquals(0f, sim.update(0.02f, speed, 0.5f, 100f).shift)
        }
    }

    @Test
    fun holdingTheThrottleStandingStillRidesThroughTheGears() {
        for (engine in listOf(EngineCatalog.bigTwinClassic, EngineCatalog.vTwinCruiser, EngineCatalog.inlineFourSport)) {
            val sim = EngineSimulator(engine.info)
            var maxGear = 0
            repeat(300) { maxGear = maxOf(maxGear, sim.update(0.05f, 0f, 1f, 80f).gear) } // 15 s held
            assertEquals(engine.info.gears, maxGear, "${engine.info.id} did not reach top gear")
            var st = EngineState()
            repeat(120) { st = sim.update(0.05f, 0f, 0f, 80f) } // 6 s released
            assertEquals(0, st.gear, "${engine.info.id} should coast back to neutral")
        }
    }

    @Test
    fun aShortThrottleBlipStandingStillStaysInNeutral() {
        val sim = EngineSimulator(EngineCatalog.bigTwinClassic.info)
        repeat(18) { assertEquals(0, sim.update(0.05f, 0f, 1f, 80f).gear) } // 0.9 s, like the notification blip
    }

    @Test
    fun turbineDoesNotGetAVirtualRide() {
        val sim = EngineSimulator(EngineCatalog.jetTurbine.info)
        var st = EngineState()
        repeat(200) { st = sim.update(0.05f, 0f, 1f, 80f) }
        assertTrue(st.rpmNorm > 0.9f, "turbine should still spool up on throttle")
        assertEquals(0, st.gear, "turbine has no gears to ride through")
    }
}
