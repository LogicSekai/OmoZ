// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.logicsekai.omoz.app.ui.theme.CardWhite
import com.logicsekai.omoz.app.ui.theme.Hairline
import com.logicsekai.omoz.app.ui.theme.Ink
import com.logicsekai.omoz.app.ui.theme.InkMuted
import com.logicsekai.omoz.app.ui.theme.Orange
import com.logicsekai.omoz.app.ui.theme.OrangeSoft
import com.logicsekai.omoz.app.ui.theme.Success
import com.logicsekai.omoz.app.ui.theme.TrackGray

private val SoftShadow = Color(0x33E8A48C)

/** White rounded card with the warm, soft shadow from the reference design. */
@Composable
fun OmoCard(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    color: Color = CardWhite,
    padding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier
            .shadow(10.dp, shape, ambientColor = SoftShadow, spotColor = SoftShadow)
            .clip(shape)
            .background(color)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** The rounded-square outlined button used for menu / back / close. */
@Composable
fun SquareIconButton(
    painter: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Orange,
    background: Color = CardWhite,
    border: Color = Hairline,
    size: Dp = 52.dp,
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .border(BorderStroke(1.dp, border), RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter, contentDescription, tint = tint, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun SquareIconButton(icon: ImageVector, contentDescription: String?, onClick: () -> Unit, modifier: Modifier = Modifier) =
    SquareIconButton(rememberVectorPainter(icon), contentDescription, onClick, modifier)

/** Screen header: back button, centered title, optional trailing action. */
@Composable
fun TopBar(title: String, onBack: () -> Unit, backLabel: String, trailing: @Composable (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SquareIconButton(Icons.AutoMirrored.Rounded.ArrowBack, backLabel, onBack)
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = Ink,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailing != null) trailing() else Spacer(Modifier.size(52.dp))
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = Ink, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(action, style = MaterialTheme.typography.labelLarge, color = InkMuted)
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = InkMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Row with the orange accent bar on the left (like "Enter a license code"). */
@Composable
fun AccentLinkRow(title: String, subtitle: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OmoCard(modifier.fillMaxWidth(), radius = 20.dp, padding = PaddingValues(0.dp), onClick = onClick) {
        Row(Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .padding(end = 18.dp)
                    .width(3.dp)
                    .height(28.dp)
                    .background(Orange, RoundedCornerShape(2.dp)),
            )
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Ink, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = InkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = InkMuted,
                modifier = Modifier.padding(end = 16.dp).size(20.dp),
            )
        }
    }
}

/** Soft rounded square with an icon (settings rows, drawer items). */
@Composable
fun IconTile(painter: Painter, tint: Color = Orange, background: Color = OrangeSoft, size: Dp = 44.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter, null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun StatusChip(ok: Boolean, okText: String, badText: String) {
    val bg = if (ok) Success.copy(alpha = 0.14f) else OrangeSoft
    val fg = if (ok) Color(0xFF1E9E45) else Orange
    Text(
        if (ok) okText else badText,
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Settings row with icon tile, texts and a trailing slot. */
@Composable
fun SettingRow(
    painter: Painter,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(painter)
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Ink, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = InkMuted)
        }
        trailing()
    }
}

@Composable
fun OmoSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = CardWhite,
            checkedTrackColor = Orange,
            checkedBorderColor = Orange,
            uncheckedThumbColor = CardWhite,
            uncheckedTrackColor = TrackGray,
            uncheckedBorderColor = Hairline,
        ),
    )
}

@Composable
fun ToggleRow(painter: Painter, title: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    SettingRow(painter, title, subtitle, onClick = { onCheckedChange(!checked) }) {
        OmoSwitch(checked, onCheckedChange)
    }
}

@Composable
fun SliderRow(
    painter: Painter,
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    steps: Int = 0,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        SettingRow(painter, title, null) {
            Text(valueLabel, style = MaterialTheme.typography.titleSmall, color = Orange)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = Orange,
                activeTrackColor = Orange,
                inactiveTrackColor = TrackGray,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
    }
}

/** Pill segmented control. */
@Composable
fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TrackGray)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (value, label) ->
            val on = value == selected
            Surface(
                onClick = { onSelect(value) },
                shape = RoundedCornerShape(12.dp),
                color = if (on) Orange else Color.Transparent,
                contentColor = if (on) CardWhite else InkMuted,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(vertical = 10.dp),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun Dot(color: Color, size: Dp = 8.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}
