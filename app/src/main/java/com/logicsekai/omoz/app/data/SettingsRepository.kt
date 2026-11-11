// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app.data

import android.content.Context
import android.content.SharedPreferences
import com.logicsekai.omoz.engine.EngineCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** GPS polling interval — the biggest battery lever after screen-off. */
enum class GpsMode(val intervalMs: Long) { ACCURATE(500L), BALANCED(1000L), SAVER(3000L), CUSTOM(0L) }

/** Allowed range for a user-entered GPS interval. */
const val GPS_CUSTOM_MIN_MS = 100L
const val GPS_CUSTOM_MAX_MS = 60_000L

/** Synth sample rate. 22 kHz halves the DSP cost and still sounds fine on small speakers. */
enum class AudioQuality(val sampleRate: Int) { HIGH(44100), SAVER(22050) }

data class RegisteredDevice(val address: String, val name: String)

data class AppSettings(
    val engineId: String = EngineCatalog.default.info.id,
    /** Recently chosen engine ids, newest first (shown as shortcuts on the home screen). */
    val recentEngineIds: List<String> = emptyList(),
    val masterVolume: Float = 0.8f,
    /** Speed where the engine is near redline in top gear. */
    val fullScaleSpeedKmh: Float = 80f,
    val speedSensitiveVolume: Boolean = true,
    val muteWhenStopped: Boolean = false,
    /** Keep running after the app is swiped away from recents. */
    val runInBackground: Boolean = true,
    /** Hold a partial wake lock (only needed on aggressive ROMs). */
    val keepCpuAwake: Boolean = false,
    val gpsMode: GpsMode = GpsMode.BALANCED,
    /** Interval used when [gpsMode] is [GpsMode.CUSTOM]. */
    val gpsCustomIntervalMs: Long = 1000L,
    val audioQuality: AudioQuality = AudioQuality.HIGH,
    /** Stop automatically after being stationary this long. 0 = never. */
    val autoStopMinutes: Int = 0,
    val bluetoothAutoStart: Boolean = false,
    val bluetoothDevices: List<RegisteredDevice> = emptyList(),
) {
    /** The GPS interval actually requested from the location provider. */
    val gpsIntervalMs: Long
        get() = if (gpsMode == GpsMode.CUSTOM) {
            gpsCustomIntervalMs.coerceIn(GPS_CUSTOM_MIN_MS, GPS_CUSTOM_MAX_MS)
        } else gpsMode.intervalMs

    fun isRegistered(address: String?): Boolean =
        address != null && bluetoothDevices.any { it.address.equals(address, ignoreCase = true) }
}

/**
 * Tiny SharedPreferences-backed store exposing settings as a [StateFlow].
 * SharedPreferences is used (instead of DataStore) so the BroadcastReceiver can read it
 * synchronously and cheaply when a Bluetooth device connects.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("omoz_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    val current: AppSettings get() = _settings.value

    @Synchronized
    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        write(next)
        _settings.value = next
    }

    private fun read(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            engineId = prefs.getString(K_ENGINE, d.engineId) ?: d.engineId,
            recentEngineIds = (prefs.getString(K_RECENT_ENGINES, "") ?: "").split(',').filter { it.isNotBlank() },
            masterVolume = prefs.getFloat(K_VOLUME, d.masterVolume),
            fullScaleSpeedKmh = prefs.getFloat(K_FULL_SPEED, d.fullScaleSpeedKmh),
            speedSensitiveVolume = prefs.getBoolean(K_SPEED_VOL, d.speedSensitiveVolume),
            muteWhenStopped = prefs.getBoolean(K_MUTE_STOPPED, d.muteWhenStopped),
            runInBackground = prefs.getBoolean(K_BACKGROUND, d.runInBackground),
            keepCpuAwake = prefs.getBoolean(K_WAKELOCK, d.keepCpuAwake),
            gpsMode = enumOr(prefs.getString(K_GPS, null), d.gpsMode),
            gpsCustomIntervalMs = prefs.getLong(K_GPS_CUSTOM, d.gpsCustomIntervalMs),
            audioQuality = enumOr(prefs.getString(K_AUDIO, null), d.audioQuality),
            autoStopMinutes = prefs.getInt(K_AUTO_STOP, d.autoStopMinutes),
            bluetoothAutoStart = prefs.getBoolean(K_BT_AUTO, d.bluetoothAutoStart),
            bluetoothDevices = (prefs.getStringSet(K_BT_DEVICES, emptySet()) ?: emptySet())
                .mapNotNull { decodeDevice(it) }
                .sortedBy { it.name.lowercase() },
        )
    }

    private fun write(s: AppSettings) {
        prefs.edit()
            .putString(K_ENGINE, s.engineId)
            .putString(K_RECENT_ENGINES, s.recentEngineIds.joinToString(","))
            .putFloat(K_VOLUME, s.masterVolume)
            .putFloat(K_FULL_SPEED, s.fullScaleSpeedKmh)
            .putBoolean(K_SPEED_VOL, s.speedSensitiveVolume)
            .putBoolean(K_MUTE_STOPPED, s.muteWhenStopped)
            .putBoolean(K_BACKGROUND, s.runInBackground)
            .putBoolean(K_WAKELOCK, s.keepCpuAwake)
            .putString(K_GPS, s.gpsMode.name)
            .putLong(K_GPS_CUSTOM, s.gpsCustomIntervalMs)
            .putString(K_AUDIO, s.audioQuality.name)
            .putInt(K_AUTO_STOP, s.autoStopMinutes)
            .putBoolean(K_BT_AUTO, s.bluetoothAutoStart)
            .putStringSet(K_BT_DEVICES, s.bluetoothDevices.map { encodeDevice(it) }.toSet())
            .apply()
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: fallback

    private fun encodeDevice(d: RegisteredDevice) = "${d.address}$SEP${d.name}"

    private fun decodeDevice(raw: String): RegisteredDevice? {
        val i = raw.indexOf(SEP)
        if (i <= 0) return null
        return RegisteredDevice(raw.substring(0, i), raw.substring(i + 1))
    }

    private companion object {
        const val SEP = '|'
        const val K_ENGINE = "engine_id"
        const val K_RECENT_ENGINES = "recent_engines"
        const val K_VOLUME = "master_volume"
        const val K_FULL_SPEED = "full_scale_speed"
        const val K_SPEED_VOL = "speed_sensitive_volume"
        const val K_MUTE_STOPPED = "mute_when_stopped"
        const val K_BACKGROUND = "run_in_background"
        const val K_WAKELOCK = "keep_cpu_awake"
        const val K_GPS = "gps_mode"
        const val K_GPS_CUSTOM = "gps_custom_interval_ms"
        const val K_AUDIO = "audio_quality"
        const val K_AUTO_STOP = "auto_stop_minutes"
        const val K_BT_AUTO = "bt_auto_start"
        const val K_BT_DEVICES = "bt_devices"
    }
}
