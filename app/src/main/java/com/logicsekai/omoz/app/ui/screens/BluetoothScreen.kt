// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.logicsekai.omoz.app.R
import com.logicsekai.omoz.app.bluetooth.PairedDevice
import com.logicsekai.omoz.app.system.Permissions
import com.logicsekai.omoz.app.system.SystemSettings
import com.logicsekai.omoz.app.ui.MainViewModel
import com.logicsekai.omoz.app.ui.components.Dot
import com.logicsekai.omoz.app.ui.components.IconTile
import com.logicsekai.omoz.app.ui.components.OmoCard
import com.logicsekai.omoz.app.ui.components.SquareIconButton
import com.logicsekai.omoz.app.ui.components.ToggleRow
import com.logicsekai.omoz.app.ui.components.TopBar
import com.logicsekai.omoz.app.ui.theme.Blue
import com.logicsekai.omoz.app.ui.theme.CardWhite
import com.logicsekai.omoz.app.ui.theme.Ink
import com.logicsekai.omoz.app.ui.theme.InkMuted
import com.logicsekai.omoz.app.ui.theme.Orange
import com.logicsekai.omoz.app.ui.theme.OrangeSoft
import com.logicsekai.omoz.app.ui.theme.Peach
import com.logicsekai.omoz.app.ui.theme.Sky
import com.logicsekai.omoz.app.ui.theme.Success

@Composable
fun BluetoothScreen(vm: MainViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val permissions by vm.permissions.collectAsStateWithLifecycle()
    val paired by vm.paired.collectAsStateWithLifecycle()
    val btOn by vm.bluetoothOn.collectAsStateWithLifecycle()

    val askBt = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { vm.refresh() }

    Column(Modifier.fillMaxSize().background(Peach).statusBarsPadding()) {
        TopBar(
            stringResource(R.string.nav_bluetooth), onBack, stringResource(R.string.back),
            trailing = {
                SquareIconButton(rememberVectorPainter(Icons.Rounded.Refresh), stringResource(R.string.refresh), onClick = vm::refreshBluetooth)
            },
        )
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.navigationBarsPadding(),
        ) {
            item {
                OmoCard(Modifier.fillMaxWidth()) {
                    ToggleRow(
                        painterResource(R.drawable.ic_bluetooth),
                        stringResource(R.string.bt_auto_title),
                        stringResource(R.string.bt_auto_sub),
                        settings.bluetoothAutoStart,
                        vm::setBluetoothAuto,
                    )
                    if (settings.bluetoothAutoStart && !permissions.batteryUnrestricted) {
                        Spacer(Modifier.height(8.dp))
                        HintBox(
                            text = stringResource(R.string.bt_battery_hint),
                            action = stringResource(R.string.fix_now),
                            onAction = { SystemSettings.requestIgnoreBatteryOptimizations(ctx) },
                        )
                    }
                }
            }

            if (!permissions.bluetooth) {
                item {
                    HintBox(
                        text = stringResource(R.string.bt_permission_needed),
                        action = stringResource(R.string.grant),
                        onAction = { askBt.launch(Permissions.bluetoothSet()) },
                    )
                }
            } else if (!btOn) {
                item {
                    HintBox(
                        text = stringResource(R.string.bt_off),
                        action = stringResource(R.string.open_settings),
                        onAction = { SystemSettings.openBluetoothSettings(ctx) },
                    )
                }
            }

            if (settings.bluetoothDevices.isNotEmpty()) {
                item { ListTitle(stringResource(R.string.bt_registered)) }
                items(settings.bluetoothDevices, key = { "reg_" + it.address }) { d ->
                    val connected = paired.any { it.address.equals(d.address, true) && it.connected }
                    DeviceRow(d.name, d.address, registered = true, connected = connected, onClick = { vm.removeDevice(d.address) })
                }
            }

            item { ListTitle(stringResource(R.string.bt_paired)) }
            val unregistered = paired.filterNot { settings.isRegistered(it.address) }
            if (unregistered.isEmpty()) {
                item {
                    Text(
                        stringResource(if (paired.isEmpty()) R.string.bt_none_paired else R.string.bt_all_registered),
                        style = MaterialTheme.typography.bodySmall, color = InkMuted,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
            items(unregistered, key = { "p_" + it.address }) { d: PairedDevice ->
                DeviceRow(d.name, d.address, registered = false, connected = d.connected, onClick = { vm.toggleDevice(d) })
            }

            item {
                Text(
                    stringResource(R.string.bt_footer),
                    style = MaterialTheme.typography.bodySmall, color = InkMuted,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ListTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.padding(top = 12.dp, start = 4.dp))
}

@Composable
private fun DeviceRow(name: String, address: String, registered: Boolean, connected: Boolean, onClick: () -> Unit) {
    val bg = if (registered) Orange else Sky
    val fg = if (registered) CardWhite else Blue
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(
            painterResource(R.drawable.ic_bluetooth),
            tint = if (registered) CardWhite else Blue,
            background = if (registered) CardWhite.copy(alpha = 0.22f) else CardWhite.copy(alpha = 0.6f),
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (connected) {
                    Dot(Success, 7.dp)
                    Text(
                        stringResource(R.string.bt_connected), style = MaterialTheme.typography.labelSmall, color = fg,
                        modifier = Modifier.padding(start = 6.dp, end = 8.dp),
                    )
                }
                Text(address, style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.7f))
            }
        }
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(CardWhite.copy(alpha = if (registered) 1f else 0.8f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (registered) Icons.Rounded.Check else Icons.Rounded.Add,
                stringResource(if (registered) R.string.bt_unregister else R.string.bt_register),
                tint = if (registered) Orange else Blue,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
fun HintBox(text: String, action: String, onAction: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(OrangeSoft)
            .padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = Ink, modifier = Modifier.weight(1f).padding(end = 8.dp))
        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = CardWhite),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        ) { Text(action, style = MaterialTheme.typography.labelMedium) }
    }
}
