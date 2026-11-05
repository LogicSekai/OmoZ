// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logicsekai.omoz.app.ui.theme.Orange
import com.logicsekai.omoz.app.ui.theme.Peach
import com.logicsekai.omoz.app.ui.theme.Poppins

/**
 * Sidebar in the OmoVibe style: the whole background turns orange and shows the menu, while the
 * active screen shrinks and slides to the right like a card, with one translucent "ghost card"
 * behind it. Tapping the card or pressing Back closes the menu.
 *
 * @param close label for the close button (accessibility)
 * @param menu the menu items, laid out in a Column (use [DrawerItem])
 */
@Composable
fun DrawerScaffold(
    open: Boolean,
    footer: String,
    closeLabel: String,
    onClose: () -> Unit,
    menu: @Composable ColumnScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    val p by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label = "drawer",
    )
    BackHandler(enabled = open, onBack = onClose)

    BoxWithConstraints(Modifier.fillMaxSize().background(if (p > 0f) Orange else Peach)) {
        val w = constraints.maxWidth.toFloat()

        // ---- menu
        if (p > 0f) {
            Column(
                Modifier
                    .fillMaxHeight()
                    .width(maxWidth * 0.62f)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(start = 24.dp, top = 16.dp, bottom = 28.dp)
                    .alpha(p)
                    .graphicsLayer { translationX = (1f - p) * -40.dp.toPx() },
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, closeLabel, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.weight(1f))
                menu()
                Spacer(Modifier.weight(1.4f))
                Text(
                    footer,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // ---- ghost card behind the active screen
        if (p > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        val s = 1f - 0.30f * p
                        scaleX = s; scaleY = s
                        translationX = p * w * 0.55f
                        alpha = p
                    }
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.22f)),
            )
        }

        // ---- active screen
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0.5f)
                    val s = 1f - 0.22f * p
                    scaleX = s; scaleY = s
                    translationX = p * w * 0.64f
                    shape = RoundedCornerShape((32 * p).dp)
                    clip = p > 0f
                    shadowElevation = 24.dp.toPx() * p
                }
                .background(Peach),
        ) {
            content()
            // While the menu is open the screen only acts as a "close" button.
            if (p > 0f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClose,
                        ),
                )
            }
        }
    }
}

/** One sidebar row: translucent rounded icon tile + label. */
@Composable
fun DrawerItem(icon: Painter, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Color.White.copy(alpha = if (selected) 0.30f else 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, label, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(
            label,
            color = Color.White.copy(alpha = if (selected) 1f else 0.85f),
            fontSize = 15.sp,
            fontFamily = Poppins,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
    Spacer(Modifier.height(14.dp))
}
