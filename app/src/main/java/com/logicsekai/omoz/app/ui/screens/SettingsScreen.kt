// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.logicsekai.omoz.app.R
import com.logicsekai.omoz.app.data.AudioQuality
import com.logicsekai.omoz.app.data.GPS_CUSTOM_MAX_MS
import com.logicsekai.omoz.app.data.GPS_CUSTOM_MIN_MS
import com.logicsekai.omoz.app.data.GpsMode
import com.logicsekai.omoz.app.system.Permissions
import com.logicsekai.omoz.app.system.SystemSettings
import com.logicsekai.omoz.app.BuildConfig
import com.logicsekai.omoz.app.ui.MainViewModel
import com.logicsekai.omoz.app.update.UpdateManager
import com.logicsekai.omoz.app.update.UpdateManagerOwner
import com.logicsekai.omoz.app.ui.components.OmoCard
import com.logicsekai.omoz.app.ui.components.Segmented
import com.logicsekai.omoz.app.ui.components.SettingRow
import com.logicsekai.omoz.app.ui.components.SliderRow
import com.logicsekai.omoz.app.ui.components.StatusChip
import com.logicsekai.omoz.app.ui.components.ToggleRow
import com.logicsekai.omoz.app.ui.components.TopBar
import com.logicsekai.omoz.app.ui.theme.CardWhite
import com.logicsekai.omoz.app.ui.theme.Hairline
import com.logicsekai.omoz.app.ui.theme.Ink
import com.logicsekai.omoz.app.ui.theme.InkMuted
import com.logicsekai.omoz.app.ui.theme.Orange
import com.logicsekai.omoz.app.ui.theme.Peach
import java.math.BigDecimal
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit, onOpenUpdate: () -> Unit) {
    val ctx = LocalContext.current
    val updates = (ctx.applicationContext as UpdateManagerOwner).updateManager
    val updateState by updates.state.collectAsStateWithLifecycle()
    val autoCheck by updates.autoCheck.collectAsStateWithLifecycle()
    val s by vm.settings.collectAsStateWithLifecycle()
    val p by vm.permissions.collectAsStateWithLifecycle()

    val askForeground = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        vm.refresh()
        if (res.values.any { !it }) SystemSettings.openAppDetails(ctx)
    }
    val askBackground = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        vm.refresh()
        if (!ok) SystemSettings.openAppDetails(ctx)
    }
    val askBt = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        vm.refresh()
        if (res.values.any { !it }) SystemSettings.openAppDetails(ctx)
    }

    val ok = stringResource(R.string.status_ok)
    val needed = stringResource(R.string.status_needed)

    Column(Modifier.fillMaxSize().background(Peach).statusBarsPadding()) {
        TopBar(stringResource(R.string.nav_settings), onBack, stringResource(R.string.back))
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.navigationBarsPadding(),
        ) {
            // ---------------------------------------------------------------- Background
            item { Section(stringResource(R.string.section_background)) }
            item {
                OmoCard(Modifier.fillMaxWidth()) {
                    ToggleRow(
                        painterResource(R.drawable.ic_power), stringResource(R.string.set_run_bg),
                        stringResource(R.string.set_run_bg_sub), s.runInBackground,
                    ) { v -> vm.update { it.copy(runInBackground = v) } }
                    Divider()
                    SettingRow(
                        painterResource(R.drawable.ic_battery), stringResource(R.string.set_battery_opt),
                        stringResource(R.string.set_battery_opt_sub),
                        onClick = { SystemSettings.requestIgnoreBatteryOptimizations(ctx) },
                    ) { StatusChip(p.batteryUnrestricted, ok, needed) }
                    Divider()
                    SettingRow(
                        rememberVectorPainter(Icons.Rounded.Refresh),
                        stringResource(R.string.set_autostart),
                        stringResource(
                            if (SystemSettings.hasOemAutostartScreen) R.string.set_autostart_sub_oem else R.string.set_autostart_sub_generic,
                            SystemSettings.brandKey.replaceFirstChar { it.uppercase() },
                        ),
                        onClick = { SystemSettings.openAutostart(ctx) },
                    )
                    Divider()
                    ToggleRow(
                        rememberVectorPainter(Icons.Rounded.Lock), stringResource(R.string.set_wakelock),
                        stringResource(R.string.set_wakelock_sub), s.keepCpuAwake,
                    ) { v -> vm.update { it.copy(keepCpuAwake = v) } }
                    Divider()
                    SettingRow(
                        rememberVectorPainter(Icons.Rounded.Info), stringResource(R.string.set_guide),
                        stringResource(R.string.set_guide_sub, SystemSettings.deviceLabel),
                        onClick = { SystemSettings.openUrl(ctx, SystemSettings.dontKillMyAppUrl) },
                    )
                }
            }

            // ---------------------------------------------------------------- Permissions
            item { Section(stringResource(R.string.section_permissions)) }
            item {
                OmoCard(Modifier.fillMaxWidth()) {
                    SettingRow(
                        rememberVectorPainter(Icons.Rounded.LocationOn), stringResource(R.string.perm_location),
                        stringResource(if (p.location && !p.preciseLocation) R.string.perm_location_approx else R.string.perm_location_sub),
                        onClick = { if (!p.preciseLocation) askForeground.launch(Permissions.foregroundSet()) else SystemSettings.openAppDetails(ctx) },
                    ) { StatusChip(p.preciseLocation, ok, needed) }
                    Divider()
                    SettingRow(
                        painterResource(R.drawable.ic_location), stringResource(R.string.perm_bg_location),
                        stringResource(R.string.perm_bg_location_sub),
                        onClick = {
                            when {
                                !p.location -> askForeground.launch(Permissions.foregroundSet())
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !p.backgroundLocation ->
                                    askBackground.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                else -> SystemSettings.openAppDetails(ctx)
                            }
                        },
                    ) { StatusChip(p.backgroundLocation, ok, needed) }
                    Divider()
                    SettingRow(
                        painterResource(R.drawable.ic_bluetooth), stringResource(R.string.perm_bluetooth),
                        stringResource(R.string.perm_bluetooth_sub),
                        onClick = { if (!p.bluetooth) askBt.launch(Permissions.bluetoothSet()) else SystemSettings.openAppDetails(ctx) },
                    ) { StatusChip(p.bluetooth, ok, needed) }
                    Divider()
                    SettingRow(
                        rememberVectorPainter(Icons.Rounded.Notifications), stringResource(R.string.perm_notifications),
                        stringResource(R.string.perm_notifications_sub),
                        onClick = {
                            if (!p.notifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                askForeground.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                            } else SystemSettings.openNotificationSettings(ctx)
                        },
                    ) { StatusChip(p.notifications, ok, needed) }
                }
            }

            // ---------------------------------------------------------------- Sound
            item { Section(stringResource(R.string.section_sound)) }
            item {
                OmoCard(Modifier.fillMaxWidth()) {
                    SliderRow(
                        painterResource(R.drawable.ic_wave), stringResource(R.string.set_volume),
                        "${(s.masterVolume * 100).roundToInt()}%", s.masterVolume, 0f..1f,
                        onValueChange = { v -> vm.update { it.copy(masterVolume = v) } },
                    )
                    SliderRow(
                        painterResource(R.drawable.ic_speed), stringResource(R.string.set_full_speed),
                        stringResource(R.string.kmh_value, s.fullScaleSpeedKmh.roundToInt()), s.fullScaleSpeedKmh, 30f..200f,
                        onValueChange = { v -> vm.update { it.copy(fullScaleSpeedKmh = (v / 5f).roundToInt() * 5f) } },
                    )
                    Text(stringResource(R.string.set_full_speed_sub), style = MaterialTheme.typography.bodySmall, color = InkMuted)
                    Spacer(Modifier.height(6.dp))
                    Divider()
                    ToggleRow(
                        painterResource(R.drawable.ic_speed), stringResource(R.string.set_speed_volume),
                        stringResource(R.string.set_speed_volume_sub), s.speedSensitiveVolume,
                    ) { v -> vm.update { it.copy(speedSensitiveVolume = v) } }
                    Divider()
                    ToggleRow(
                        rememberVectorPainter(Icons.Rounded.Star), stringResource(R.string.set_mute_stopped),
                        stringResource(R.string.set_mute_stopped_sub), s.muteWhenStopped,
                    ) { v -> vm.update { it.copy(muteWhenStopped = v) } }
                }
            }

            // ---------------------------------------------------------------- Battery
            item { Section(stringResource(R.string.section_battery)) }
            item {
                OmoCard(Modifier.fillMaxWidth()) {
                    Label(painterResource(R.drawable.ic_location), stringResource(R.string.set_gps_mode), stringResource(R.string.set_gps_mode_sub))
                    Segmented(
                        listOf(
                            GpsMode.ACCURATE to stringResource(R.string.gps_accurate),
                            GpsMode.BALANCED to stringResource(R.string.gps_balanced),
                            GpsMode.SAVER to stringResource(R.string.gps_saver),
                            GpsMode.CUSTOM to stringResource(R.string.gps_custom),
                        ),
                        s.gpsMode, { m -> vm.update { it.copy(gpsMode = m) } },
                    )
                    if (s.gpsMode == GpsMode.CUSTOM) {
                        Spacer(Modifier.height(10.dp))
                        GpsCustomInput(
                            currentMs = s.gpsCustomIntervalMs,
                            onApply = { ms -> vm.update { it.copy(gpsCustomIntervalMs = ms) } },
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Label(painterResource(R.drawable.ic_wave), stringResource(R.string.set_audio_quality), stringResource(R.string.set_audio_quality_sub))
                    Segmented(
                        listOf(
                            AudioQuality.HIGH to stringResource(R.string.audio_high),
                            AudioQuality.SAVER to stringResource(R.string.audio_saver),
                        ),
                        s.audioQuality, { q -> vm.update { it.copy(audioQuality = q) } },
                    )
                    Spacer(Modifier.height(14.dp))
                    Label(painterResource(R.drawable.ic_battery), stringResource(R.string.set_auto_stop), stringResource(R.string.set_auto_stop_sub))
                    Segmented(
                        listOf(
                            0 to stringResource(R.string.off_short),
                            5 to stringResource(R.string.minutes_short, 5),
                            10 to stringResource(R.string.minutes_short, 10),
                            30 to stringResource(R.string.minutes_short, 30),
                        ),
                        s.autoStopMinutes, { m -> vm.update { it.copy(autoStopMinutes = m) } },
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.battery_tip), style = MaterialTheme.typography.bodySmall, color = InkMuted)
                }
            }

            // ---------------------------------------------------------------- App update
            item { Section(stringResource(R.string.nav_update)) }
            item {
                OmoCard(Modifier.fillMaxWidth()) {
                    val available = (updateState as? UpdateManager.State.Available)?.release?.version
                    SettingRow(
                        painterResource(R.drawable.ic_system_update),
                        if (available != null) stringResource(R.string.update_available, available)
                        else stringResource(R.string.set_update_version, BuildConfig.VERSION_NAME),
                        stringResource(
                            when {
                                !updates.isEnabled -> R.string.update_disabled_title
                                available != null -> R.string.set_update_open_new
                                else -> R.string.set_update_open
                            },
                        ),
                        onClick = onOpenUpdate,
                    ) {
                        if (updates.isEnabled) {
                            StatusChip(
                                available == null,
                                stringResource(R.string.set_update_latest),
                                stringResource(R.string.set_update_new),
                            )
                        }
                    }
                    if (updates.isEnabled) {
                        Divider()
                        ToggleRow(
                            rememberVectorPainter(Icons.Rounded.Notifications),
                            stringResource(R.string.update_auto_title),
                            stringResource(R.string.update_auto_caption),
                            autoCheck,
                        ) { v -> updates.setAutoCheck(v) }
                    }
                }
            }
        }
    }
}

/** Text field for a custom GPS interval in seconds (accepts "0,5" or "0.5"). */
@Composable
private fun GpsCustomInput(currentMs: Long, onApply: (Long) -> Unit) {
    val focus = LocalFocusManager.current
    var text by remember(currentMs) { mutableStateOf(formatSeconds(currentMs)) }
    val parsedMs = text.trim().replace(',', '.').toDoubleOrNull()?.let { (it * 1000).roundToLong() }
    val valid = parsedMs != null && parsedMs in GPS_CUSTOM_MIN_MS..GPS_CUSTOM_MAX_MS
    val changed = valid && parsedMs != currentMs
    val apply = {
        if (valid && parsedMs != null) {
            onApply(parsedMs)
            focus.clearFocus()
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = { v -> text = v.filter { it.isDigit() || it == '.' || it == ',' }.take(6) },
            label = { Text(stringResource(R.string.gps_custom_label)) },
            suffix = { Text(stringResource(R.string.seconds_short)) },
            singleLine = true,
            isError = !valid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { apply() }),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Orange,
                focusedLabelColor = Orange,
                cursorColor = Orange,
            ),
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = { apply() },
            enabled = changed,
            colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = CardWhite),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.padding(start = 10.dp, top = 6.dp),
        ) { Text(stringResource(R.string.apply), style = MaterialTheme.typography.labelLarge) }
    }
    val hz = 1000.0 / (if (valid && parsedMs != null) parsedMs else currentMs)
    Text(
        if (valid) {
            stringResource(R.string.gps_custom_hint, formatHz(hz))
        } else {
            stringResource(R.string.gps_custom_invalid)
        },
        style = MaterialTheme.typography.bodySmall,
        color = if (valid) InkMuted else MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(top = 4.dp, start = 4.dp),
    )
}

private fun formatHz(hz: Double): String {
    val pattern = when {
        hz >= 10 -> "%.0f"
        hz >= 1 -> "%.1f"
        else -> "%.2f"
    }
    return String.format(Locale.getDefault(), pattern, hz)
}

private fun formatSeconds(ms: Long): String {
    val text = BigDecimal(ms).divide(BigDecimal(1000)).stripTrailingZeros().toPlainString()
    // Use the locale's decimal separator (Indonesian uses a comma).
    val sep = java.text.DecimalFormatSymbols.getInstance().decimalSeparator
    return text.replace('.', sep)
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, color = Ink, modifier = Modifier.padding(top = 10.dp, start = 2.dp))
}

@Composable
private fun Divider() = HorizontalDivider(color = Hairline, modifier = Modifier.padding(vertical = 2.dp))

@Composable
private fun Label(painter: Painter, title: String, subtitle: String) {
    SettingRow(painter, title, subtitle)
}
