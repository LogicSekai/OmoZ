// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import com.logicsekai.omoz.app.engine.GpsStatus
import com.logicsekai.omoz.app.system.Permissions
import com.logicsekai.omoz.engine.SpeedGlide

/**
 * Reads vehicle speed straight from the platform GPS provider.
 *
 * We deliberately avoid Google Play Services' FusedLocationProvider so OmoZ works on every
 * brand, including Huawei/Honor devices without GMS and de-Googled ROMs.
 */
class SpeedTracker(private val context: Context) : LocationListener {

    private val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @Volatile var status: GpsStatus = GpsStatus.OFF
        private set

    /** Filtered speed of the latest fix (the glide target). */
    @Volatile private var smoothedKmh = 0f
    /** Continuous speed between fixes (what the engine hears). */
    private val glide = SpeedGlide(1f)
    private var lastReadAt = 0L
    private var lastFixAt = 0L
    private var lastLocation: Location? = null
    private var started = false

    @SuppressLint("MissingPermission")
    fun start(intervalMs: Long): GpsStatus {
        stop()
        if (!Permissions.hasLocation(context)) {
            status = GpsStatus.NO_PERMISSION
            return status
        }
        glide.setInterval(intervalMs / 1000f)
        status = if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) GpsStatus.SEARCHING else GpsStatus.DISABLED
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, intervalMs, 0f, this, Looper.getMainLooper())
            started = true
        } catch (e: SecurityException) {
            status = GpsStatus.NO_PERMISSION
        } catch (e: IllegalArgumentException) {
            // Device without a GPS provider.
            status = GpsStatus.DISABLED
        }
        return status
    }

    fun stop() {
        if (started) runCatching { lm.removeUpdates(this) }
        started = false
        status = GpsStatus.OFF
        smoothedKmh = 0f
        glide.reset()
        lastLocation = null
    }

    /**
     * Call every service tick. Returns a smooth speed that glides between GPS fixes, and decays
     * when the fix is lost (tunnels, parking garages).
     */
    fun currentSpeedKmh(): Float {
        val now = SystemClock.elapsedRealtime()
        val dt = if (lastReadAt == 0L) 0f else (now - lastReadAt) / 1000f
        lastReadAt = now
        if (status == GpsStatus.FIX && now - lastFixAt > FIX_TIMEOUT_MS) {
            status = GpsStatus.SEARCHING
        }
        if (status != GpsStatus.FIX && smoothedKmh > 0f) {
            smoothedKmh = (smoothedKmh * 0.985f).let { if (it < 0.5f) 0f else it }
            glide.onFix(smoothedKmh)
        }
        return glide.advance(dt)
    }

    override fun onLocationChanged(location: Location) {
        val now = SystemClock.elapsedRealtime()
        val prev = lastLocation
        var raw = when {
            location.hasSpeed() -> location.speed * 3.6f
            prev != null -> {
                val dt = (location.elapsedRealtimeNanos - prev.elapsedRealtimeNanos) / 1e9f
                if (dt > 0.2f) prev.distanceTo(location) / dt * 3.6f else smoothedKmh
            }
            else -> 0f
        }
        // Ignore very uncertain speed readings.
        var weight = 0.8f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasSpeedAccuracy()) {
            val acc = location.speedAccuracyMetersPerSecond
            if (acc > 4f) weight = 0.25f else if (acc > 2f) weight = 0.5f
        }
        if (raw < STATIONARY_DEADBAND_KMH) raw = 0f
        if (raw > 400f) raw = smoothedKmh // glitch
        smoothedKmh += (raw - smoothedKmh) * weight
        if (smoothedKmh < 0.5f) smoothedKmh = 0f

        glide.onFix(smoothedKmh)
        lastLocation = location
        lastFixAt = now
        status = GpsStatus.FIX
    }

    // Implemented explicitly: these are abstract on Android < 11 and would crash otherwise.
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

    override fun onProviderEnabled(provider: String) {
        if (status == GpsStatus.DISABLED) status = GpsStatus.SEARCHING
    }

    override fun onProviderDisabled(provider: String) {
        status = GpsStatus.DISABLED
    }

    private companion object {
        const val FIX_TIMEOUT_MS = 5000L
        const val STATIONARY_DEADBAND_KMH = 2.0f
    }
}
