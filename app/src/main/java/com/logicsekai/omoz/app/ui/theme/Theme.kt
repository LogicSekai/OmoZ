// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val OmoColors = lightColorScheme(
    primary = Orange,
    onPrimary = CardWhite,
    primaryContainer = OrangeSoft,
    onPrimaryContainer = OrangeDeep,
    secondary = Blue,
    onSecondary = CardWhite,
    secondaryContainer = Sky,
    onSecondaryContainer = Blue,
    tertiary = Navy,
    onTertiary = CardWhite,
    background = Peach,
    onBackground = Ink,
    surface = CardWhite,
    onSurface = Ink,
    surfaceVariant = TrackGray,
    onSurfaceVariant = InkMuted,
    outline = Hairline,
)

private val OmoShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
)

@Composable
fun OmoZTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = OmoColors, typography = OmoTypography, shapes = OmoShapes, content = content)
}
