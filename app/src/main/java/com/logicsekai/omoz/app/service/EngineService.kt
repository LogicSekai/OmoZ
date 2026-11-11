// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import com.logicsekai.omoz.app.AppGraph
import com.logicsekai.omoz.app.R
import com.logicsekai.omoz.app.data.AppSettings
import com.logicsekai.omoz.app.engine.EngineAudioPlayer
import com.logicsekai.omoz.app.engine.EngineRuntime
import com.logicsekai.omoz.app.engine.GpsStatus
import com.logicsekai.omoz.app.engine.StartReason
import com.logicsekai.omoz.app.location.SpeedTracker
import com.logicsekai.omoz.app.system.Permissions
import com.logicsekai.omoz.app.ui.engineDisplayName
import com.logicsekai.omoz.engine.EngineCatalog
import com.logicsekai.omoz.engine.EngineKind
import com.logicsekai.omoz.engine.EngineSimulator
import com.logicsekai.omoz.engine.EngineSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that owns the whole engine pipeline:
 *
 *   GPS (SpeedTracker) ─┐
 *                       ├─> EngineSimulator ─> EngineAudioPlayer (AudioTrack thread)
 *   Throttle (UI/notif) ┘                 └─> EngineRuntime (UI state) + notification
 *
 * Runs with the app closed or swiped away (stopWithTask=false + foreground notification).
 */
class EngineService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickJob: Job? = null
    private var settingsJob: Job? = null

    private var player: EngineAudioPlayer? = null
    private var playerRate = 0
    private lateinit var tracker: SpeedTracker
    private lateinit var simulator: EngineSimulator
    private var sound: EngineSound = EngineCatalog.default
    private var wakeLock: PowerManager.WakeLock? = null
    private var gpsIntervalMs = -1L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        tracker = SpeedTracker(this)
        simulator = EngineSimulator(sound.info)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { shutdown(); return START_NOT_STICKY }
            ACTION_BLIP -> {
                if (!EngineRuntime.running.value) startEngine(StartReason.MANUAL, fromForeground = false)
                EngineRuntime.blip()
            }
            ACTION_START -> {
                val reason = intent.getStringExtra(EXTRA_REASON)
                    ?.let { r -> StartReason.entries.firstOrNull { it.name == r } } ?: StartReason.MANUAL
                startEngine(reason, intent.getBooleanExtra(EXTRA_FOREGROUND, false))
            }
            null -> {
                // Restarted by the system after being killed (START_STICKY).
                if (AppGraph.settings(this).current.runInBackground) {
                    startEngine(StartReason.RESTART, fromForeground = false)
                } else {
                    stopSelf(); return START_NOT_STICKY
                }
            }
        }
        return START_STICKY
    }

    private fun startEngine(reason: StartReason, fromForeground: Boolean) {
        val settings = AppGraph.settings(this).current
        sound = EngineCatalog.byId(settings.engineId)

        if (!goForeground(settings, fromForeground)) {
            stopSelf()
            return
        }
        EngineNotification.cancelStartPrompt(this)

        val alreadyRunning = EngineRuntime.running.value
        // A Bluetooth/restart start must not downgrade a manual session.
        val effectiveReason = if (alreadyRunning && EngineRuntime.startReason == StartReason.MANUAL) StartReason.MANUAL else reason
        EngineRuntime.setRunning(true, effectiveReason)
        if (alreadyRunning && tickJob?.isActive == true) {
            if (reason == StartReason.PREVIEW) EngineRuntime.blip(1200)
            return
        }

        simulator = EngineSimulator(sound.info)
        ensurePlayer(settings)
        gpsIntervalMs = settings.gpsIntervalMs
        tracker.start(gpsIntervalMs)
        applyWakeLock(settings.keepCpuAwake)
        if (reason == StartReason.PREVIEW) EngineRuntime.blip(1200)

        settingsJob?.cancel()
        settingsJob = scope.launch {
            AppGraph.settings(this@EngineService).settings.collectLatest { onSettingsChanged(it) }
        }
        tickJob?.cancel()
        tickJob = scope.launch { tickLoop() }
    }

    private fun ensurePlayer(settings: AppSettings) {
        val rate = settings.audioQuality.sampleRate
        if (player == null || playerRate != rate) {
            player?.stop()
            player = EngineAudioPlayer(rate).also { it.start(sound) }
            playerRate = rate
        } else {
            player?.setSound(sound)
        }
    }

    private fun onSettingsChanged(s: AppSettings) {
        val newSound = EngineCatalog.byId(s.engineId)
        if (newSound.info.id != sound.info.id) {
            sound = newSound
            simulator.info = newSound.info
            player?.setSound(newSound)
        }
        if (player != null && playerRate != s.audioQuality.sampleRate) ensurePlayer(s)
        if (gpsIntervalMs != s.gpsIntervalMs) {
            gpsIntervalMs = s.gpsIntervalMs
            tracker.start(gpsIntervalMs)
        }
        applyWakeLock(s.keepCpuAwake)
    }

    private suspend fun CoroutineScope.tickLoop() {
        var last = SystemClock.elapsedRealtime()
        var lastNotif = 0L
        var lastHistory = 0L
        var stillSince = SystemClock.elapsedRealtime()
        var lastNotifText = ""
        var lastGpsRetry = 0L
        val repo = AppGraph.settings(this@EngineService)

        while (isActive) {
            val now = SystemClock.elapsedRealtime()
            val dt = (now - last) / 1000f
            last = now
            val s = repo.current

            // Retry GPS once permission is granted while running.
            if (tracker.status == GpsStatus.NO_PERMISSION && now - lastGpsRetry > 5000) {
                lastGpsRetry = now
                if (Permissions.hasLocation(this@EngineService)) tracker.start(s.gpsIntervalMs)
            }

            val speed = tracker.currentSpeedKmh()
            val throttle = EngineRuntime.throttleInput()
            val state = simulator.update(dt, speed, throttle, s.fullScaleSpeedKmh)

            val moving = speed >= EngineSimulator.MOVING_THRESHOLD_KMH
            var vol = s.masterVolume
            if (s.speedSensitiveVolume) vol *= 0.7f + 0.3f * (speed / s.fullScaleSpeedKmh).coerceIn(0f, 1f)
            if (s.muteWhenStopped && !moving && throttle < 0.05f && state.rpmNorm < 0.08f) vol = 0f

            player?.let {
                it.rpm = state.rpmNorm
                it.load = state.load
                it.shift = state.shift
                it.volume = vol
            }
            EngineRuntime.publish(speed, tracker.status, state)

            if (now - lastHistory >= 500) {
                EngineRuntime.pushHistory(state.rpmNorm, state.gear)
                lastHistory = now
            }

            // Auto-stop when parked.
            if (moving || throttle > 0f) stillSince = now
            if (s.autoStopMinutes > 0 && now - stillSince > s.autoStopMinutes * 60_000L) {
                Log.i(TAG, "Stationary for ${s.autoStopMinutes} min → auto-stop")
                shutdown()
                return
            }

            if (now - lastNotif >= 2000) {
                val text = notificationText(speed, state.gear, state.rpm)
                if (text != lastNotifText) {
                    EngineNotification.update(this@EngineService, notificationTitle(), text)
                    lastNotifText = text
                }
                lastNotif = now
            }
            delay(TICK_MS)
        }
    }

    private fun notificationTitle() = getString(R.string.notif_title, engineDisplayName(this, sound.info))

    private fun notificationText(speed: Float, gear: Int, rpm: Int): String {
        val rpmLabel = when (sound.info.kind) {
            EngineKind.TURBINE -> getString(R.string.n1_value, rpm)
            EngineKind.ELECTRIC -> getString(R.string.motor_value, rpm)
            else -> getString(R.string.rpm_value, rpm)
        }
        val gearLabel = if (gear == 0) getString(R.string.gear_neutral) else getString(R.string.gear_value, gear)
        return getString(R.string.notif_text, speed.toInt(), gearLabel, rpmLabel)
    }

    /** Enter the foreground with the right service types for this Android version. */
    private fun goForeground(settings: AppSettings, fromForeground: Boolean): Boolean {
        val notification = EngineNotification.buildEngine(
            this, notificationTitle(), getString(R.string.notif_starting),
        )
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var types = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                val locationOk = Permissions.hasLocation(this) &&
                    (fromForeground || Permissions.hasBackgroundLocation(this))
                if (locationOk) types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                try {
                    startForeground(EngineNotification.ID_ENGINE, notification, types)
                } catch (e: SecurityException) {
                    Log.w(TAG, "Location FGS type refused, continuing without GPS", e)
                    startForeground(EngineNotification.ID_ENGINE, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                }
            } else {
                startForeground(EngineNotification.ID_ENGINE, notification)
            }
            true
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException (Android 12+) or OEM restrictions.
            Log.e(TAG, "Cannot enter foreground", e)
            EngineNotification.showStartPrompt(this)
            false
        }
    }

    @Suppress("WakelockTimeout")
    private fun applyWakeLock(enabled: Boolean) {
        if (enabled && wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OmoZ:engine").apply {
                setReferenceCounted(false)
                acquire()
            }
        } else if (!enabled && wakeLock != null) {
            wakeLock?.let { if (it.isHeld) it.release() }
            wakeLock = null
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (!AppGraph.settings(this).current.runInBackground) shutdown()
    }

    private fun shutdown() {
        tickJob?.cancel(); tickJob = null
        settingsJob?.cancel(); settingsJob = null
        player?.stop(); player = null
        tracker.stop()
        applyWakeLock(false)
        EngineRuntime.setRunning(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        tickJob?.cancel()
        settingsJob?.cancel()
        player?.stop(); player = null
        if (::tracker.isInitialized) tracker.stop()
        applyWakeLock(false)
        EngineRuntime.setRunning(false)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "OmoZService"
        private const val TICK_MS = 50L

        const val ACTION_START = "com.logicsekai.omoz.action.START"
        const val ACTION_STOP = "com.logicsekai.omoz.action.STOP"
        const val ACTION_BLIP = "com.logicsekai.omoz.action.BLIP"
        private const val EXTRA_REASON = "reason"
        private const val EXTRA_FOREGROUND = "from_foreground"

        /** Start (or update) the engine. Safe to call from UI, receivers or notifications. */
        fun start(context: Context, reason: StartReason, fromForeground: Boolean): Boolean {
            val intent = Intent(context, EngineService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_REASON, reason.name)
                .putExtra(EXTRA_FOREGROUND, fromForeground)
            return try {
                context.startForegroundService(intent)
                true
            } catch (e: Exception) {
                // Android 12+: background FGS start not allowed → ask the user with one tap.
                Log.w(TAG, "startForegroundService refused", e)
                EngineNotification.showStartPrompt(context)
                false
            }
        }

        fun stop(context: Context) {
            if (!EngineRuntime.running.value) return
            runCatching {
                context.startService(Intent(context, EngineService::class.java).setAction(ACTION_STOP))
            }.onFailure { context.stopService(Intent(context, EngineService::class.java)) }
        }
    }
}
