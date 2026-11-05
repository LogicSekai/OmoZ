// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import com.logicsekai.omoz.app.R
import com.logicsekai.omoz.app.system.Permissions
import com.logicsekai.omoz.app.ui.MainActivity

object EngineNotification {
    const val CHANNEL_ENGINE = "engine"
    const val CHANNEL_ALERTS = "alerts"
    const val ID_ENGINE = 42
    const val ID_START_PROMPT = 43

    private const val ORANGE = 0xFFFF7A4F.toInt()

    private fun nm(context: Context) =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannels(context: Context) {
        val nm = nm(context)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ENGINE, context.getString(R.string.channel_engine), NotificationManager.IMPORTANCE_LOW).apply {
                description = context.getString(R.string.channel_engine_desc)
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, context.getString(R.string.channel_alerts), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.channel_alerts_desc)
            },
        )
    }

    private fun openAppIntent(context: Context, startEngine: Boolean = false): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (startEngine) intent.action = MainActivity.ACTION_START_ENGINE
        return PendingIntent.getActivity(
            context, if (startEngine) 1 else 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun serviceIntent(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            context, requestCode,
            Intent(context, EngineService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    fun buildEngine(context: Context, title: String, text: String): Notification =
        Notification.Builder(context, CHANNEL_ENGINE)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ORANGE)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(openAppIntent(context))
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_throttle),
                    context.getString(R.string.action_rev), serviceIntent(context, EngineService.ACTION_BLIP, 10),
                ).build(),
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_power),
                    context.getString(R.string.action_stop), serviceIntent(context, EngineService.ACTION_STOP, 11),
                ).build(),
            )
            .build()

    fun update(context: Context, title: String, text: String) {
        if (!Permissions.hasNotifications(context)) return
        nm(context)
            .notify(ID_ENGINE, buildEngine(context, title, text))
    }

    /**
     * Shown when Android refuses to start the foreground service from the background
     * (Android 12+ without battery-optimization exemption). One tap starts the engine.
     */
    fun showStartPrompt(context: Context) {
        if (!Permissions.hasNotifications(context)) return
        val n = Notification.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ORANGE)
            .setContentTitle(context.getString(R.string.prompt_start_title))
            .setContentText(context.getString(R.string.prompt_start_text))
            .setStyle(Notification.BigTextStyle().bigText(context.getString(R.string.prompt_start_text)))
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context, startEngine = true))
            .build()
        nm(context).notify(ID_START_PROMPT, n)
    }

    fun cancelStartPrompt(context: Context) {
        nm(context).cancel(ID_START_PROMPT)
    }
}
