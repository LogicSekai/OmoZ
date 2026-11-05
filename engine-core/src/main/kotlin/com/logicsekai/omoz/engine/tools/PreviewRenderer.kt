// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.engine.tools

import com.logicsekai.omoz.engine.EngineCatalog
import com.logicsekai.omoz.engine.EngineSimulator
import com.logicsekai.omoz.engine.EngineSound
import com.logicsekai.omoz.engine.SynthParams
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Offline renderer so contributors can hear an engine without a phone:
 *
 *   ./gradlew :engine-core:renderPreviews      (writes WAVs to engine-core/build/previews)
 *
 * The scenario: idle → throttle blips → accelerate through the gears to 90 km/h → coast down.
 */
object PreviewRenderer {

    fun render(sound: EngineSound, sampleRate: Int = 44100, seconds: Float = 16f): FloatArray {
        val synth = sound.createSynth(sampleRate)
        val sim = EngineSimulator(sound.info)
        val params = SynthParams()
        val block = 256
        val total = (seconds * sampleRate).toInt()
        val out = FloatArray(total)
        val buf = FloatArray(block)
        val dt = block.toFloat() / sampleRate
        var t = 0f
        var pos = 0
        while (pos < total) {
            val (speed, throttle) = scenario(t)
            val st = sim.update(dt, speed, throttle, 100f)
            params.rpm = st.rpmNorm
            params.load = st.load
            params.volume = if (t < 0.3f) t / 0.3f else 1f
            val n = minOf(block, total - pos)
            synth.render(buf, n, params)
            System.arraycopy(buf, 0, out, pos, n)
            pos += n
            t += dt
        }
        return out
    }

    /** (speed km/h, throttle 0..1) over time. */
    fun scenario(t: Float): Pair<Float, Float> = when {
        t < 2f -> 0f to 0f
        t < 2.5f -> 0f to 1f
        t < 3.5f -> 0f to 0f
        t < 4.3f -> 0f to 1f
        t < 5.5f -> 0f to 0f
        t < 13f -> ((t - 5.5f) / 7.5f * 90f) to 0.6f
        else -> (90f - (t - 13f) * 25f).coerceAtLeast(0f) to 0f
    }

    fun writeWav(file: File, samples: FloatArray, sampleRate: Int) {
        file.parentFile?.mkdirs()
        DataOutputStream(FileOutputStream(file).buffered()).use { o ->
            val dataLen = samples.size * 2
            o.writeBytes("RIFF"); o.writeIntLE(36 + dataLen); o.writeBytes("WAVE")
            o.writeBytes("fmt "); o.writeIntLE(16); o.writeShortLE(1); o.writeShortLE(1)
            o.writeIntLE(sampleRate); o.writeIntLE(sampleRate * 2); o.writeShortLE(2); o.writeShortLE(16)
            o.writeBytes("data"); o.writeIntLE(dataLen)
            for (s in samples) o.writeShortLE((s.coerceIn(-1f, 1f) * 32767f).toInt())
        }
    }

    private fun DataOutputStream.writeIntLE(v: Int) {
        write(v and 0xff); write((v shr 8) and 0xff); write((v shr 16) and 0xff); write((v shr 24) and 0xff)
    }

    private fun DataOutputStream.writeShortLE(v: Int) {
        write(v and 0xff); write((v shr 8) and 0xff)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val dir = File(args.firstOrNull() ?: "build/previews")
        for (s in EngineCatalog.all) {
            val pcm = render(s)
            val f = File(dir, "${s.info.id}.wav")
            writeWav(f, pcm, 44100)
            println("wrote ${f.path}")
        }
    }
}
