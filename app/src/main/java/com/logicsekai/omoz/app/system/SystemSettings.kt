// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app.system

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.util.Locale

/**
 * Opens the right system screen on every brand. OEM ROMs (MIUI/HyperOS, ColorOS, FuntouchOS,
 * EMUI/MagicOS, One UI, XOS/HiOS, ...) add their own "autostart" and battery killers on top of
 * stock Android; we try each known screen and fall back to the standard app-details page.
 *
 * Community: if your phone's autostart screen is missing, add its ComponentName to [OEM_SCREENS].
 */
object SystemSettings {

    data class OemScreen(val pkg: String, val cls: String)

    private val OEM_SCREENS: Map<String, List<OemScreen>> = mapOf(
        "xiaomi" to listOf(
            OemScreen("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            OemScreen("com.miui.securitycenter", "com.miui.powercenter.PowerSettings"),
        ),
        "oppo" to listOf(
            OemScreen("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            OemScreen("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            OemScreen("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
            OemScreen("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"),
        ),
        "vivo" to listOf(
            OemScreen("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            OemScreen("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
            OemScreen("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
        ),
        "huawei" to listOf(
            OemScreen("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            OemScreen("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"),
            OemScreen("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
        ),
        "honor" to listOf(
            OemScreen("com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            OemScreen("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        ),
        "samsung" to listOf(
            OemScreen("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
            OemScreen("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity"),
        ),
        "oneplus" to listOf(
            OemScreen("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
            OemScreen("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
        ),
        "asus" to listOf(
            OemScreen("com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity"),
            OemScreen("com.asus.mobilemanager", "com.asus.mobilemanager.entry.FunctionActivity"),
        ),
        "transsion" to listOf( // Infinix, Tecno, itel
            OemScreen("com.transsion.phonemaster", "com.cyin.himgr.autostart.AutoStartActivity"),
        ),
        "letv" to listOf(
            OemScreen("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity"),
        ),
        "nokia" to listOf(
            OemScreen("com.evenwell.powersaving.g3", "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity"),
        ),
        "meizu" to listOf(
            OemScreen("com.meizu.safe", "com.meizu.safe.permission.SmartBGActivity"),
        ),
    )

    /** Normalized brand key used for [OEM_SCREENS] and the dontkillmyapp.com guide. */
    val brandKey: String by lazy {
        val m = (Build.MANUFACTURER + " " + Build.BRAND).lowercase(Locale.ROOT)
        when {
            "xiaomi" in m || "redmi" in m || "poco" in m -> "xiaomi"
            "realme" in m || "oppo" in m -> "oppo"
            "vivo" in m || "iqoo" in m -> "vivo"
            "honor" in m -> "honor"
            "huawei" in m -> "huawei"
            "samsung" in m -> "samsung"
            "oneplus" in m -> "oneplus"
            "asus" in m -> "asus"
            "infinix" in m || "tecno" in m || "itel" in m || "transsion" in m -> "transsion"
            "letv" in m || "leeco" in m -> "letv"
            "nokia" in m || "hmd" in m -> "nokia"
            "meizu" in m -> "meizu"
            else -> Build.MANUFACTURER.lowercase(Locale.ROOT)
        }
    }

    val deviceLabel: String
        get() = "${Build.MANUFACTURER.replaceFirstChar { it.titlecase(Locale.ROOT) }} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}"

    val hasOemAutostartScreen: Boolean get() = OEM_SCREENS.containsKey(brandKey)

    val dontKillMyAppUrl: String
        get() {
            val slug = when (brandKey) {
                "transsion" -> "infinix"
                "oppo" -> if ("realme" in Build.BRAND.lowercase(Locale.ROOT)) "realme" else "oppo"
                else -> brandKey
            }
            return "https://dontkillmyapp.com/$slug"
        }

    /** Try every OEM autostart / background screen for this brand; fall back to app details. */
    fun openAutostart(context: Context): Boolean {
        OEM_SCREENS[brandKey].orEmpty().forEach { screen ->
            val intent = Intent().setComponent(ComponentName(screen.pkg, screen.cls))
            if (tryStart(context, intent)) return true
        }
        return openAppDetails(context)
    }

    /** Ask to be exempted from Doze / battery optimization (Android's standard dialog). */
    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context): Boolean {
        val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:${context.packageName}"))
        if (tryStart(context, direct)) return true
        return tryStart(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) || openAppDetails(context)
    }

    fun openAppDetails(context: Context): Boolean =
        tryStart(
            context,
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:${context.packageName}")),
        )

    fun openNotificationSettings(context: Context): Boolean =
        tryStart(
            context,
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
        ) || openAppDetails(context)

    fun openLocationSourceSettings(context: Context): Boolean =
        tryStart(context, Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))

    fun openBluetoothSettings(context: Context): Boolean =
        tryStart(context, Intent(Settings.ACTION_BLUETOOTH_SETTINGS))

    fun openUrl(context: Context, url: String): Boolean =
        tryStart(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    private fun tryStart(context: Context, intent: Intent): Boolean = try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    } catch (e: IllegalArgumentException) {
        false
    }
}
