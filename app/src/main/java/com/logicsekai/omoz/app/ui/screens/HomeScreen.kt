// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.logicsekai.omoz.app.R
import com.logicsekai.omoz.app.engine.GpsStatus
import com.logicsekai.omoz.app.ui.MainViewModel
import com.logicsekai.omoz.app.ui.components.AccentLinkRow
import com.logicsekai.omoz.app.ui.components.AddEngineCard
import com.logicsekai.omoz.app.ui.components.EngineBanner
import com.logicsekai.omoz.app.ui.components.EngineCard
import com.logicsekai.omoz.app.ui.components.EngineStatusAvatar
import com.logicsekai.omoz.app.ui.components.RpmWidget
import com.logicsekai.omoz.app.ui.components.SectionHeader
import com.logicsekai.omoz.app.ui.components.SpeedWidget
import com.logicsekai.omoz.app.ui.components.SquareIconButton
import com.logicsekai.omoz.app.ui.components.ThrottleDock
import com.logicsekai.omoz.app.ui.components.engineStyle
import com.logicsekai.omoz.app.ui.engineDisplayName
import com.logicsekai.omoz.app.ui.theme.InkMuted
import com.logicsekai.omoz.app.ui.theme.Peach
import com.logicsekai.omoz.engine.EngineCatalog
import com.logicsekai.omoz.engine.EngineKind

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onMenu: () -> Unit,
    onOpenSounds: () -> Unit,
    onOpenBluetooth: () -> Unit,
    onOpenAbout: () -> Unit,
    updateVersion: String? = null,
    onOpenUpdate: () -> Unit = {},
) {
    val ctx = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val running by vm.running.collectAsStateWithLifecycle()
    val speed by vm.speed.collectAsStateWithLifecycle()
    val gps by vm.gps.collectAsStateWithLifecycle()
    val engine by vm.engine.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()

    val sounds = EngineCatalog.all
    val selected = EngineCatalog.byId(settings.engineId)
    val turbine = selected.info.kind == EngineKind.TURBINE
    val electric = selected.info.kind == EngineKind.ELECTRIC
    // Turbines and electric motors show a 0–100% gauge instead of rpm + gear.
    val percent = turbine || electric
    val gaugeTitle = when {
        turbine -> R.string.n1_title
        electric -> R.string.motor_title
        else -> R.string.rpm_title
    }

    // Fan rotation for the selected turbine card, speed follows RPM.
    var spin by remember { mutableFloatStateOf(0f) }
    val rpmNow by rememberUpdatedState(engine.rpmNorm)
    LaunchedEffect(running) {
        var last = withFrameMillis { it }
        while (running) {
            withFrameMillis { now ->
                spin = (spin + (now - last) / 1000f * (40f + 900f * rpmNow)) % 360f
                last = now
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Peach)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding(),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SquareIconButton(Icons.Rounded.Menu, stringResource(R.string.open_menu), onMenu)
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.home_tagline), style = MaterialTheme.typography.bodySmall, color = InkMuted)
                }
                EngineStatusAvatar(running, onClick = vm::toggleEngine)
            }

            SectionHeader(
                stringResource(R.string.home_sounds_title, sounds.size),
                modifier = Modifier.padding(start = 24.dp, end = 18.dp, top = 8.dp),
                action = stringResource(R.string.show_all),
                onAction = onOpenSounds,
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(sounds, key = { _, s -> s.info.id }) { i, s ->
                    val isSel = s.info.id == selected.info.id
                    EngineCard(
                        name = engineDisplayName(ctx, s.info),
                        style = engineStyle(s.info, i),
                        selected = isSel,
                        spin = if (isSel) spin else 0f,
                        onClick = { vm.selectEngine(s.info.id) },
                    )
                }
                item {
                    AddEngineCard(
                        stringResource(R.string.add_sound_title),
                        stringResource(R.string.add_sound_sub),
                        onClick = onOpenAbout,
                    )
                }
            }

            Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EngineBanner(
                    title = if (running) {
                        stringResource(R.string.banner_on_title, engineDisplayName(ctx, selected.info))
                    } else stringResource(R.string.banner_off_title),
                    subtitle = if (running) stringResource(R.string.banner_on_sub) else stringResource(R.string.banner_off_sub),
                    trailingTop = if (running) "${speed.toInt()}" else stringResource(R.string.off_short),
                    trailingBottom = if (running) stringResource(R.string.unit_kmh) else stringResource(R.string.tap_to_start),
                    running = running,
                    onClick = vm::toggleEngine,
                )

                val btSubtitle = when {
                    settings.bluetoothDevices.isEmpty() -> stringResource(R.string.bt_row_none)
                    !settings.bluetoothAutoStart -> stringResource(R.string.bt_row_auto_off)
                    else -> settings.bluetoothDevices.joinToString { it.name }
                }
                if (updateVersion != null) {
                    AccentLinkRow(
                        stringResource(R.string.home_update_title, updateVersion),
                        stringResource(R.string.home_update_sub),
                        onOpenUpdate,
                    )
                }
                AccentLinkRow(stringResource(R.string.bt_row_title), btSubtitle, onOpenBluetooth)

                SectionHeader(stringResource(R.string.widgets), Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    SpeedWidget(
                        title = stringResource(R.string.speed_title),
                        speed = speed.toInt(),
                        unit = stringResource(R.string.unit_kmh),
                        gpsLabel = gpsLabel(gps),
                        gpsOk = gps == GpsStatus.FIX,
                        fraction = speed / settings.fullScaleSpeedKmh,
                        modifier = Modifier.weight(1f),
                    )
                    RpmWidget(
                        title = stringResource(gaugeTitle),
                        unit = stringResource(if (turbine) R.string.n1_unit else if (electric) R.string.motor_unit else R.string.rpm_unit_k),
                        history = history,
                        redline = selected.info.redlineRpm,
                        idle = selected.info.idleRpm,
                        turbine = percent,
                        neutralLabel = stringResource(R.string.gear_short_neutral),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(200.dp))
        }

        Box(Modifier.align(Alignment.BottomCenter)) {
            ThrottleDock(
                rpmNorm = engine.rpmNorm,
                leftTop = if (percent) "${engine.rpm}%" else "%,d".format(engine.rpm),
                leftBottom = stringResource(if (percent) gaugeTitle else R.string.dock_rpm),
                rightTop = when {
                    percent -> "—"
                    engine.gear == 0 -> stringResource(R.string.gear_short_neutral)
                    else -> "${engine.gear}"
                },
                rightBottom = stringResource(R.string.dock_gear),
                hint = stringResource(R.string.hint_hold),
                accessibilityLabel = stringResource(R.string.throttle_cd),
                onThrottle = vm::setThrottle,
            )
        }
    }
}

@Composable
fun gpsLabel(status: GpsStatus): String = stringResource(
    when (status) {
        GpsStatus.OFF -> R.string.gps_off
        GpsStatus.NO_PERMISSION -> R.string.gps_no_permission
        GpsStatus.DISABLED -> R.string.gps_disabled
        GpsStatus.SEARCHING -> R.string.gps_searching
        GpsStatus.FIX -> R.string.gps_fix
    },
)
