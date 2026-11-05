// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logicsekai.omoz.app.R
import com.logicsekai.omoz.app.ui.theme.CardWhite
import com.logicsekai.omoz.app.ui.theme.Hairline
import com.logicsekai.omoz.app.ui.theme.Ink
import com.logicsekai.omoz.app.ui.theme.InkMuted
import com.logicsekai.omoz.app.ui.theme.Orange
import com.logicsekai.omoz.app.ui.theme.OrangeDeep
import com.logicsekai.omoz.app.ui.theme.OrangeSoft
import com.logicsekai.omoz.app.ui.theme.TrackGray

/** Big coloured engine card for the horizontal "Engine sounds" row. */
@Composable
fun EngineCard(
    name: String,
    style: EngineStyle,
    selected: Boolean,
    spin: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(if (selected) 1f else 0.96f, label = "cardScale")
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier
            .width(156.dp)
            .height(236.dp)
            .scale(scale)
            .shadow(if (selected) 14.dp else 6.dp, shape, ambientColor = style.container, spotColor = style.container)
            .clip(shape)
            .background(style.container)
            .clickable(onClick = onClick),
    ) {
        // Decorative bubbles, echoing the playful reference cards.
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(style.content.copy(alpha = 0.08f), size.width * 0.55f, Offset(size.width * 0.95f, size.height * 0.05f))
            drawCircle(style.content.copy(alpha = 0.06f), size.width * 0.18f, Offset(size.width * 0.1f, size.height * 0.55f))
        }
        EngineArt(
            style.art, tint = style.content, accent = if (style.container == Orange) OrangeSoft else Orange,
            modifier = Modifier
                .padding(top = 34.dp)
                .size(104.dp)
                .align(Alignment.TopCenter),
            spin = spin,
        )
        if (selected) {
            Box(
                Modifier
                    .padding(12.dp)
                    .size(30.dp)
                    .align(Alignment.TopEnd)
                    .border(2.dp, style.content.copy(alpha = 0.7f), CircleShape)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(style.content),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Check, null, tint = style.container, modifier = Modifier.size(14.dp))
            }
        }
        Text(
            name,
            color = style.content,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp),
            maxLines = 2,
        )
    }
}

/** White "add" card at the end of the engine row, pointing to the contribution guide. */
@Composable
fun AddEngineCard(title: String, subtitle: String, onClick: () -> Unit) {
    OmoCard(Modifier.width(140.dp).height(236.dp), radius = 26.dp, onClick = onClick, padding = PaddingValues(18.dp)) {
        Spacer(Modifier.weight(1f))
        Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = InkMuted)
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .size(44.dp)
                .shadow(8.dp, CircleShape, spotColor = Orange)
                .clip(CircleShape)
                .background(Orange),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Add, null, tint = CardWhite) }
        Spacer(Modifier.weight(0.4f))
    }
}

/** Orange banner that shows engine status and toggles it (like the "Resume" banner). */
@Composable
fun EngineBanner(
    title: String,
    subtitle: String,
    trailingTop: String,
    trailingBottom: String,
    running: Boolean,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(if (running) Orange else OrangeDeep.copy(alpha = 0.92f), label = "bannerBg")
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(12.dp, shape, spotColor = Orange, ambientColor = Orange)
            .clip(shape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CardWhite.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_power), null, tint = CardWhite, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(title, color = CardWhite, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = CardWhite.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(trailingTop, color = CardWhite, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(trailingBottom, color = CardWhite.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Speed widget: big number + streak-style bars (like "Learning streak"). */
@Composable
fun SpeedWidget(title: String, speed: Int, unit: String, gpsLabel: String, gpsOk: Boolean, fraction: Float, modifier: Modifier = Modifier) {
    OmoCard(modifier.height(200.dp), radius = 24.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(painterResource(R.drawable.ic_location), null, tint = if (gpsOk) Orange else InkMuted, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(14.dp))
        val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(400), label = "speedBars")
        Canvas(Modifier.fillMaxWidth().height(44.dp)) {
            val n = 9
            val gap = size.width / (n * 2.2f)
            for (i in 0 until n) {
                val lit = i < (f * n).toInt() + 1
                val h = size.height * listOf(0.45f, 0.55f, 0.7f, 0.5f, 0.85f, 1f, 0.6f, 0.4f, 0.3f)[i]
                val x = i * gap * 2.2f
                drawRoundRect(
                    if (lit) Orange else TrackGray,
                    Offset(x, (size.height - h) / 2), Size(gap * 0.5f, h), CornerRadius(gap, gap),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$speed", style = MaterialTheme.typography.displayLarge, color = Ink, fontWeight = FontWeight.Normal)
            Text(unit, style = MaterialTheme.typography.bodyMedium, color = InkMuted, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        }
        Text(gpsLabel, style = MaterialTheme.typography.bodySmall, color = InkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** RPM history bars (like "Activity & Results"). Top labels: rpm (k), bottom: gear. */
@Composable
fun RpmWidget(
    title: String,
    unit: String,
    history: List<Pair<Float, Int>>,
    redline: Int,
    idle: Int,
    turbine: Boolean,
    neutralLabel: String,
    modifier: Modifier = Modifier,
) {
    OmoCard(modifier.height(200.dp), radius = 24.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(painterResource(R.drawable.ic_bars), null, tint = Orange, modifier = Modifier.size(16.dp))
        }
        Text(unit, style = MaterialTheme.typography.bodySmall, color = InkMuted, modifier = Modifier.align(Alignment.End))
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
            history.forEach { (rpmNorm, gear) ->
                val value = idle + (redline - idle) * rpmNorm
                val top = if (turbine) "${value.toInt()}" else "%.0f".format(value / 1000f)
                val anim by animateFloatAsState(rpmNorm, tween(450), label = "bar")
                Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(top, style = MaterialTheme.typography.labelSmall, color = InkMuted, fontSize = 10.sp)
                    Spacer(Modifier.height(4.dp))
                    Canvas(Modifier.width(8.dp).weight(1f)) {
                        val r = CornerRadius(size.width / 2, size.width / 2)
                        drawRoundRect(TrackGray, cornerRadius = r)
                        val h = size.height * (0.06f + 0.94f * anim)
                        drawRoundRect(Orange, Offset(0f, size.height - h), Size(size.width, h), r)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (gear == 0) neutralLabel else "$gear",
                        style = MaterialTheme.typography.labelSmall, color = InkMuted, fontSize = 10.sp,
                    )
                }
            }
        }
    }
}

/**
 * Bottom dock with the big round throttle. Press and hold = open the throttle; release = close.
 * The ring around the button shows live RPM.
 */
@Composable
fun ThrottleDock(
    rpmNorm: Float,
    leftTop: String,
    leftBottom: String,
    rightTop: String,
    rightBottom: String,
    hint: String,
    accessibilityLabel: String,
    onThrottle: (Boolean) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    val throttleCallback by rememberUpdatedState(onThrottle)
    val btnScale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "throttleScale")
    val ring by animateFloatAsState(rpmNorm.coerceIn(0f, 1f), tween(120), label = "ring")
    val dockShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)

    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .padding(top = 40.dp)
                .fillMaxWidth()
                .shadow(18.dp, dockShape, ambientColor = Color(0x22000000), spotColor = Color(0x22000000))
                .clip(dockShape)
                .background(CardWhite)
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(leftTop, style = MaterialTheme.typography.titleMedium, color = Ink)
                Text(leftBottom, style = MaterialTheme.typography.labelSmall, color = InkMuted)
            }
            Column(Modifier.width(140.dp).padding(top = 58.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(hint, style = MaterialTheme.typography.labelSmall, color = InkMuted, textAlign = TextAlign.Center, maxLines = 1)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(rightTop, style = MaterialTheme.typography.titleMedium, color = Ink)
                Text(rightBottom, style = MaterialTheme.typography.labelSmall, color = InkMuted)
            }
        }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(112.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 7.dp.toPx()
                drawCircle(CardWhite, size.minDimension / 2)
                drawArc(TrackGray, 135f, 270f, false, topLeft = Offset(stroke, stroke), size = Size(size.width - 2 * stroke, size.height - 2 * stroke), style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(Orange, 135f, 270f * ring, false, topLeft = Offset(stroke, stroke), size = Size(size.width - 2 * stroke, size.height - 2 * stroke), style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Box(
                Modifier
                    .size(84.dp)
                    .scale(btnScale)
                    .shadow(if (pressed) 4.dp else 14.dp, CircleShape, spotColor = Orange, ambientColor = Orange)
                    .clip(CircleShape)
                    .background(if (pressed) OrangeDeep else Orange)
                    .semantics { contentDescription = accessibilityLabel }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                pressed = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                throttleCallback(true)
                                try {
                                    tryAwaitRelease()
                                } finally {
                                    pressed = false
                                    throttleCallback(false)
                                }
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_throttle), null, tint = CardWhite, modifier = Modifier.size(34.dp))
            }
        }
    }
}

/** Small "status" avatar on the home header: shows a pulsing dot when the engine runs. */
@Composable
fun EngineStatusAvatar(running: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (running) OrangeSoft else CardWhite)
            .border(1.dp, Hairline, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_turbine), null, tint = if (running) Orange else InkMuted, modifier = Modifier.size(26.dp))
        Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            Dot(if (running) Color(0xFF34C759) else Hairline, 9.dp)
        }
    }
}
