// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import android.util.Log
import com.logicsekai.omoz.engine.EngineSound
import com.logicsekai.omoz.engine.EngineSynth
import com.logicsekai.omoz.engine.SynthParams

/**
 * Streams an [EngineSynth] to an [AudioTrack] from a dedicated high-priority thread.
 *
 * Control values (rpm/load/volume) are plain volatile fields written by the service tick
 * and read once per render block (~6 ms), so no locking is needed on the audio path.
 * Audio focus is intentionally NOT requested: the engine mixes with music/navigation.
 */
class EngineAudioPlayer(private val sampleRate: Int) {

    @Volatile var rpm = 0f
    @Volatile var load = 0f
    @Volatile var volume = 0f

    @Volatile private var pendingSound: EngineSound? = null
    @Volatile private var active = false
    private var thread: Thread? = null

    fun start(sound: EngineSound) {
        if (active) { setSound(sound); return }
        pendingSound = sound
        active = true
        thread = Thread({ loop() }, "omoz-audio").also { it.start() }
    }

    fun setSound(sound: EngineSound) { pendingSound = sound }

    fun stop() {
        active = false
        thread?.join(500)
        thread = null
    }

    private fun createTrack(): AudioTrack {
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(BLOCK * 2)
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            // ~2x the minimum keeps latency low (throttle feels instant) without underruns.
            .setBufferSizeInBytes(minBuf * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private fun loop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val fbuf = FloatArray(BLOCK)
        val sbuf = ShortArray(BLOCK)
        val params = SynthParams()
        var synth: EngineSynth? = null
        var track: AudioTrack? = null

        try {
            while (active) {
                if (track == null) {
                    track = createTrack()
                    track.play()
                }

                val next = pendingSound
                if (next != null) {
                    pendingSound = null
                    // Fade the old synth out over one block to avoid a click.
                    synth?.let { old ->
                        params.rpm = rpm; params.load = load; params.volume = 0f
                        old.render(fbuf, BLOCK, params)
                        writeBlock(track, fbuf, sbuf)
                    }
                    synth = next.createSynth(sampleRate)
                }

                val s = synth ?: continue
                params.rpm = rpm
                params.load = load
                params.volume = volume
                s.render(fbuf, BLOCK, params)
                val written = writeBlock(track, fbuf, sbuf)
                if (written < 0) {
                    // Route change / dead object (e.g. Bluetooth speaker switched): rebuild track.
                    Log.w(TAG, "AudioTrack write error $written, recreating")
                    runCatching { track.release() }
                    track = null
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Audio loop crashed", t)
        } finally {
            track?.let {
                runCatching { it.pause(); it.flush(); it.stop() }
                it.release()
            }
        }
    }

    private fun writeBlock(track: AudioTrack, f: FloatArray, s: ShortArray): Int {
        for (i in 0 until BLOCK) s[i] = (f[i].coerceIn(-1f, 1f) * 32767f).toInt().toShort()
        return track.write(s, 0, BLOCK)
    }

    private companion object {
        const val TAG = "OmoZAudio"
        const val BLOCK = 256
    }
}
