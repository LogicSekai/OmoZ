// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.logicsekai.omoz.app.R
import com.logicsekai.omoz.app.ui.MainViewModel
import com.logicsekai.omoz.app.ui.components.EngineArt
import com.logicsekai.omoz.app.ui.components.OmoCard
import com.logicsekai.omoz.app.ui.components.SquareIconButton
import com.logicsekai.omoz.app.ui.components.TopBar
import com.logicsekai.omoz.app.ui.components.engineStyle
import com.logicsekai.omoz.app.ui.engineDescription
import com.logicsekai.omoz.app.ui.engineDisplayName
import com.logicsekai.omoz.app.ui.theme.Blue
import com.logicsekai.omoz.app.ui.theme.CardWhite
import com.logicsekai.omoz.app.ui.theme.Ink
import com.logicsekai.omoz.app.ui.theme.InkMuted
import com.logicsekai.omoz.app.ui.theme.Orange
import com.logicsekai.omoz.app.ui.theme.OrangeSoft
import com.logicsekai.omoz.app.ui.theme.Peach
import com.logicsekai.omoz.app.ui.theme.Sky
import com.logicsekai.omoz.app.ui.theme.TrackGray
import com.logicsekai.omoz.app.ui.theme.Warning
import com.logicsekai.omoz.engine.EngineCatalog
import com.logicsekai.omoz.engine.EngineInfo
import com.logicsekai.omoz.engine.EngineKind

@Composable
fun SoundsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val running by vm.running.collectAsStateWithLifecycle()
    val engine by vm.engine.collectAsStateWithLifecycle()
    val sounds = EngineCatalog.all
    val selected = EngineCatalog.byId(settings.engineId)
    val selIndex = sounds.indexOf(selected).coerceAtLeast(0)
    val selStyle = engineStyle(selected.info, selIndex)

    Column(Modifier.fillMaxSize().background(Peach).statusBarsPadding()) {
        TopBar(
            stringResource(R.string.nav_sounds), onBack, stringResource(R.string.back),
            trailing = {
                SquareIconButton(
                    rememberVectorPainter(Icons.Rounded.PlayArrow), stringResource(R.string.preview),
                    onClick = { vm.preview(selected.info.id) },
                )
            },
        )

        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.navigationBarsPadding(),
        ) {
            item {
                OmoCard(Modifier.fillMaxWidth().padding(bottom = 12.dp), radius = 28.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                                val f = if (running) engine.rpmNorm else 0f
                                Canvas(Modifier.fillMaxSize()) {
                                    val sw = 5.dp.toPx()
                                    val inset = Size(size.width - sw, size.height - sw)
                                    drawArc(Sky, 0f, 360f, false, Offset(sw / 2, sw / 2), inset, style = Stroke(sw))
                                    drawArc(Blue, -90f, 360f * f.coerceAtLeast(0.02f), false, Offset(sw / 2, sw / 2), inset, style = Stroke(sw, cap = StrokeCap.Round))
                                }
                                Text("${(engine.rpmNorm * 100).toInt().takeIf { running } ?: 0}%", style = MaterialTheme.typography.labelMedium, color = Ink)
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                engineDisplayName(ctx, selected.info),
                                style = MaterialTheme.typography.titleLarge, color = Ink,
                            )
                            Text(specLine(selected.info), style = MaterialTheme.typography.bodySmall, color = InkMuted)
                        }
                        Box(
                            Modifier
                                .size(128.dp)
                                .clip(RoundedCornerShape(30.dp))
                                .background(selStyle.container),
                            contentAlignment = Alignment.Center,
                        ) {
                            EngineArt(selStyle.art, selStyle.content, if (selStyle.container == Orange) OrangeSoft else Orange, Modifier.size(92.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(engineDescription(ctx, selected.info), style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                }
            }

            // Grouped by engine family so a long list stays easy to scan.
            val groups = sounds.withIndex().groupBy { soundGroup(it.value.info) }
            SoundGroup.entries.forEach { group ->
                val members = groups[group] ?: return@forEach
                item(key = "group_${group.name}") {
                    Text(
                        "${stringResource(group.title)} · ${members.size}",
                        style = MaterialTheme.typography.titleSmall, color = InkMuted,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                    )
                }
                items(members, key = { it.value.info.id }) { (i, s) ->
                val isSel = s.info.id == selected.info.id
                SoundRow(
                    info = s.info,
                    name = engineDisplayName(ctx, s.info),
                    index = i,
                    selected = isSel,
                    liveRpm = if (isSel && running) engine.rpmNorm else 0f,
                    onSelect = { vm.selectEngine(s.info.id) },
                    onPreview = { vm.preview(s.info.id) },
                    previewLabel = stringResource(R.string.preview),
                )
                }
            }

            item {
                Text(
                    stringResource(R.string.sounds_footer),
                    style = MaterialTheme.typography.bodySmall, color = InkMuted,
                    modifier = Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun specLine(info: EngineInfo): String =
    if (info.gears <= 1) {
        stringResource(R.string.spec_turbine, info.specs)
    } else {
        stringResource(R.string.spec_piston, info.gears, "%,d".format(info.redlineRpm))
    }

@Composable
private fun SoundRow(
    info: EngineInfo,
    name: String,
    index: Int,
    selected: Boolean,
    liveRpm: Float,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
    previewLabel: String,
) {
    val style = engineStyle(info, index)
    val bg = if (selected) Orange else Sky
    val fg = if (selected) CardWhite else Blue
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(if (selected) 10.dp else 0.dp, shape, spotColor = Orange, ambientColor = Orange)
            .clip(shape)
            .background(bg)
            .clickable(onClick = onSelect),
    ) {
        Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) CardWhite.copy(alpha = 0.22f) else style.container),
                contentAlignment = Alignment.Center,
            ) {
                EngineArt(style.art, if (selected) CardWhite else style.content, if (selected) OrangeSoft else Orange, Modifier.size(32.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(info.specs, style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(3) { k ->
                    val lit = k < loudnessStars(info)
                    Text("★", color = if (lit) Warning else fg.copy(alpha = 0.3f), style = MaterialTheme.typography.labelLarge)
                }
            }
            Box(
                Modifier
                    .padding(start = 10.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (selected) CardWhite else CardWhite.copy(alpha = 0.7f))
                    .clickable(onClick = onPreview),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.PlayArrow, previewLabel, tint = if (selected) Orange else Blue, modifier = Modifier.size(20.dp))
            }
        }
        if (selected) {
            Box(Modifier.fillMaxWidth().height(4.dp).background(TrackGray.copy(alpha = 0.35f))) {
                Box(Modifier.fillMaxWidth(liveRpm.coerceIn(0.02f, 1f)).height(4.dp).background(Warning))
            }
        }
    }
}

/** "Character" rating shown as stars: how aggressive the sound is. */
private fun loudnessStars(info: EngineInfo): Int = when (info.kind) {
    EngineKind.TURBINE -> 3
    EngineKind.ELECTRIC -> 2
    EngineKind.PISTON_2_STROKE -> 3
    EngineKind.PISTON_4_STROKE -> if (info.redlineRpm > 10000) 3 else 2
    else -> 1
}

/** Families shown as sections on the sounds screen, in this order. */
private enum class SoundGroup(@StringRes val title: Int) {
    JET(R.string.sound_group_jet),
    ELECTRIC(R.string.sound_group_electric),
    TWIN(R.string.sound_group_twin),
    FOUR(R.string.sound_group_four),
    TWO_STROKE(R.string.sound_group_two_stroke),
    OTHER(R.string.sound_group_other),
}

private fun soundGroup(info: EngineInfo): SoundGroup = when (info.kind) {
    EngineKind.TURBINE -> SoundGroup.JET
    EngineKind.ELECTRIC -> SoundGroup.ELECTRIC
    EngineKind.PISTON_2_STROKE -> SoundGroup.TWO_STROKE
    EngineKind.PISTON_4_STROKE -> when (info.cylinders) {
        2 -> SoundGroup.TWIN
        4 -> SoundGroup.FOUR
        else -> SoundGroup.OTHER
    }
    else -> SoundGroup.OTHER
}
