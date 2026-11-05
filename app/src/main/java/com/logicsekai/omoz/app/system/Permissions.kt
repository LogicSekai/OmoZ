// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app.system

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager

/** One place for every runtime-permission / system-state check used by the app. */
object Permissions {

    fun granted(context: Context, permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    fun hasLocation(context: Context): Boolean =
        granted(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
            granted(context, Manifest.permission.ACCESS_COARSE_LOCATION)

    fun hasPreciseLocation(context: Context): Boolean =
        granted(context, Manifest.permission.ACCESS_FINE_LOCATION)

    fun hasBackgroundLocation(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            granted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else hasLocation(context)

    fun hasBluetooth(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            granted(context, Manifest.permission.BLUETOOTH_CONNECT)
        } else true

    fun hasNotifications(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            granted(context, Manifest.permission.POST_NOTIFICATIONS)
        } else true

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Permissions to ask for together on first use. */
    fun foregroundSet(): Array<String> = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

    fun bluetoothSet(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) arrayOf(Manifest.permission.BLUETOOTH_CONNECT) else emptyArray()
}

/** Snapshot used by the Settings screen. */
data class PermissionState(
    val notifications: Boolean = false,
    val location: Boolean = false,
    val preciseLocation: Boolean = false,
    val backgroundLocation: Boolean = false,
    val bluetooth: Boolean = false,
    val batteryUnrestricted: Boolean = false,
) {
    companion object {
        fun read(context: Context) = PermissionState(
            notifications = Permissions.hasNotifications(context),
            location = Permissions.hasLocation(context),
            preciseLocation = Permissions.hasPreciseLocation(context),
            backgroundLocation = Permissions.hasBackgroundLocation(context),
            bluetooth = Permissions.hasBluetooth(context),
            batteryUnrestricted = Permissions.isIgnoringBatteryOptimizations(context),
        )
    }
}
