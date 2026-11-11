// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

package com.logicsekai.omoz.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.logicsekai.omoz.app.ui.theme.Blue
import com.logicsekai.omoz.app.ui.theme.Navy
import com.logicsekai.omoz.app.ui.theme.Orange
import com.logicsekai.omoz.app.ui.theme.Sky
import com.logicsekai.omoz.engine.EngineInfo
import com.logicsekai.omoz.engine.EngineKind

/** Illustration family for an engine card. */
enum class ArtStyle { TURBINE, V_TWIN, INLINE_FOUR, SINGLE, ELECTRIC, GENERIC }

/** Visual identity of an engine on cards and lists. */
data class EngineStyle(val container: Color, val content: Color, val art: ArtStyle)

/**
 * Community: add an entry for your engine id here to give it a colour and illustration.
 * Unknown ids get a palette colour by position and an art style derived from [EngineKind].
 */
fun engineStyle(info: EngineInfo, index: Int): EngineStyle = when (info.id) {
    "jet_turbine" -> EngineStyle(Navy, Color.White, ArtStyle.TURBINE)
    "jet_whine" -> EngineStyle(Sky, Navy, ArtStyle.TURBINE)
    "jet_hum" -> EngineStyle(Blue, Color.White, ArtStyle.TURBINE)
    "bullet_train_motor" -> EngineStyle(Orange, Color.White, ArtStyle.ELECTRIC)
    "v_twin_cruiser" -> EngineStyle(Orange, Color.White, ArtStyle.V_TWIN)
    "big_twin_classic" -> EngineStyle(Navy, Color.White, ArtStyle.V_TWIN)
    "inline_four_sport" -> EngineStyle(Blue, Color.White, ArtStyle.INLINE_FOUR)
    "inline_four_250" -> EngineStyle(Navy, Sky, ArtStyle.INLINE_FOUR)
    "inline_four_400" -> EngineStyle(Sky, Navy, ArtStyle.INLINE_FOUR)
    "inline_four_600" -> EngineStyle(Blue, Color.White, ArtStyle.INLINE_FOUR)
    "inline_four_1000" -> EngineStyle(Orange, Color.White, ArtStyle.INLINE_FOUR)
    "supercharged_four_1000" -> EngineStyle(Navy, Color.White, ArtStyle.INLINE_FOUR)
    "two_stroke_racer" -> EngineStyle(Sky, Blue, ArtStyle.SINGLE)
    else -> {
        val palette = listOf(Navy to Color.White, Orange to Color.White, Blue to Color.White, Sky to Blue)
        val (bg, fg) = palette[index % palette.size]
        val art = when (info.kind) {
            EngineKind.TURBINE -> ArtStyle.TURBINE
            EngineKind.PISTON_2_STROKE -> ArtStyle.SINGLE
            EngineKind.PISTON_4_STROKE -> ArtStyle.INLINE_FOUR
            EngineKind.ELECTRIC -> ArtStyle.ELECTRIC
            else -> ArtStyle.GENERIC
        }
        EngineStyle(bg, fg, art)
    }
}

/**
 * Original vector illustrations drawn in code (no image assets → tiny APK, crisp on any DPI).
 * @param spin rotation in degrees for the turbine fan (animated by RPM on the home screen).
 */
@Composable
fun EngineArt(style: ArtStyle, tint: Color, accent: Color, modifier: Modifier = Modifier, spin: Float = 0f) {
    Canvas(modifier) {
        when (style) {
            ArtStyle.TURBINE -> drawTurbine(tint, accent, spin)
            ArtStyle.V_TWIN -> drawVTwin(tint, accent)
            ArtStyle.INLINE_FOUR -> drawInlineFour(tint, accent)
            ArtStyle.SINGLE -> drawSingle(tint, accent)
            ArtStyle.ELECTRIC -> drawElectric(tint, accent, spin)
            ArtStyle.GENERIC -> drawWave(tint)
        }
    }
}

private fun DrawScope.drawTurbine(tint: Color, accent: Color, spin: Float) {
    val r = size.minDimension / 2f * 0.92f
    val c = center
    drawCircle(tint.copy(alpha = 0.18f), r, c)
    drawCircle(tint, r, c, style = Stroke(width = r * 0.12f))
    rotate(spin, c) {
        val blades = 9
        for (i in 0 until blades) {
            rotate(360f / blades * i, c) {
                val p = Path().apply {
                    moveTo(c.x, c.y - r * 0.18f)
                    cubicTo(c.x + r * 0.32f, c.y - r * 0.35f, c.x + r * 0.30f, c.y - r * 0.72f, c.x + r * 0.08f, c.y - r * 0.82f)
                    lineTo(c.x - r * 0.06f, c.y - r * 0.80f)
                    cubicTo(c.x + r * 0.10f, c.y - r * 0.60f, c.x + r * 0.08f, c.y - r * 0.36f, c.x - r * 0.08f, c.y - r * 0.18f)
                    close()
                }
                drawPath(p, tint.copy(alpha = 0.92f))
            }
        }
    }
    drawCircle(accent, r * 0.22f, c)
    drawCircle(tint, r * 0.08f, c)
}

private fun DrawScope.drawCylinder(topLeft: Offset, w: Float, h: Float, tint: Color, accent: Color) {
    val fins = 5
    val finH = h * 0.08f
    val gap = (h * 0.62f - fins * finH) / (fins - 1)
    for (i in 0 until fins) {
        drawRoundRect(
            tint, Offset(topLeft.x - w * 0.12f, topLeft.y + i * (finH + gap)),
            Size(w * 1.24f, finH), CornerRadius(finH / 2, finH / 2),
        )
    }
    drawRoundRect(tint.copy(alpha = 0.85f), Offset(topLeft.x, topLeft.y), Size(w, h * 0.66f), CornerRadius(w * 0.15f, w * 0.15f))
    // Spark plug
    drawLine(accent, Offset(topLeft.x + w / 2, topLeft.y - h * 0.12f), Offset(topLeft.x + w / 2, topLeft.y), strokeWidth = w * 0.14f, cap = StrokeCap.Round)
    // Crankcase
    drawRoundRect(tint, Offset(topLeft.x - w * 0.05f, topLeft.y + h * 0.66f), Size(w * 1.1f, h * 0.34f), CornerRadius(w * 0.3f, w * 0.3f))
}

private fun DrawScope.drawVTwin(tint: Color, accent: Color) {
    val w = size.width * 0.26f
    val h = size.height * 0.62f
    val base = Offset(size.width / 2f, size.height * 0.84f)
    rotate(-24f, base) { drawCylinder(Offset(base.x - w / 2, base.y - h), w, h, tint, accent) }
    rotate(24f, base) { drawCylinder(Offset(base.x - w / 2, base.y - h), w, h, tint, accent) }
    drawCircle(accent, size.minDimension * 0.12f, base)
    drawCircle(tint, size.minDimension * 0.05f, base)
}

private fun DrawScope.drawInlineFour(tint: Color, accent: Color) {
    val n = 4
    val w = size.width * 0.15f
    val h = size.height * 0.58f
    val totalW = n * w + (n - 1) * w * 0.32f
    val startX = (size.width - totalW) / 2f
    for (i in 0 until n) {
        drawCylinder(Offset(startX + i * w * 1.32f, size.height * 0.2f), w, h, tint, accent)
    }
    drawRoundRect(tint, Offset(startX - w * 0.2f, size.height * 0.2f + h * 0.92f), Size(totalW + w * 0.4f, size.height * 0.12f), CornerRadius(20f, 20f))
}

private fun DrawScope.drawSingle(tint: Color, accent: Color) {
    val w = size.width * 0.3f
    val h = size.height * 0.62f
    translate(left = -size.width * 0.12f) {
        drawCylinder(Offset(size.width / 2f - w / 2, size.height * 0.2f), w, h, tint, accent)
    }
    // Expansion chamber
    val p = Path().apply {
        moveTo(size.width * 0.52f, size.height * 0.62f)
        cubicTo(size.width * 0.70f, size.height * 0.80f, size.width * 0.86f, size.height * 0.70f, size.width * 0.92f, size.height * 0.46f)
    }
    drawPath(p, accent, style = Stroke(width = size.minDimension * 0.11f, cap = StrokeCap.Round))
    drawPath(p, tint.copy(alpha = 0.35f), style = Stroke(width = size.minDimension * 0.04f, cap = StrokeCap.Round))
}

/** Electric traction motor: stator ring with coils, spinning rotor and a lightning bolt. */
private fun DrawScope.drawElectric(tint: Color, accent: Color, spin: Float) {
    val r = size.minDimension / 2f * 0.92f
    val c = center
    drawCircle(tint.copy(alpha = 0.18f), r, c)
    drawCircle(tint, r, c, style = Stroke(width = r * 0.1f))
    // Stator coils
    val coils = 12
    for (i in 0 until coils) {
        rotate(360f / coils * i, c) {
            drawRoundRect(
                tint, Offset(c.x - r * 0.07f, c.y - r * 0.86f), Size(r * 0.14f, r * 0.24f),
                CornerRadius(r * 0.05f, r * 0.05f),
            )
        }
    }
    // Rotor
    rotate(spin, c) {
        drawCircle(tint.copy(alpha = 0.35f), r * 0.52f, c)
        for (i in 0 until 4) {
            rotate(90f * i, c) {
                drawLine(tint, Offset(c.x, c.y - r * 0.12f), Offset(c.x, c.y - r * 0.5f), strokeWidth = r * 0.07f, cap = StrokeCap.Round)
            }
        }
    }
    // Lightning bolt
    val b = Path().apply {
        moveTo(c.x + r * 0.08f, c.y - r * 0.38f)
        lineTo(c.x - r * 0.18f, c.y + r * 0.04f)
        lineTo(c.x - r * 0.01f, c.y + r * 0.04f)
        lineTo(c.x - r * 0.1f, c.y + r * 0.38f)
        lineTo(c.x + r * 0.2f, c.y - r * 0.06f)
        lineTo(c.x + r * 0.02f, c.y - r * 0.06f)
        close()
    }
    drawPath(b, accent)
}

private fun DrawScope.drawWave(tint: Color) {
    val bars = 7
    val bw = size.width / (bars * 2f)
    for (i in 0 until bars) {
        val hFrac = listOf(0.3f, 0.6f, 0.9f, 0.5f, 0.75f, 0.4f, 0.2f)[i]
        val h = size.height * hFrac
        drawRoundRect(tint, Offset(bw * (2 * i + 0.5f), (size.height - h) / 2f), Size(bw, h), CornerRadius(bw / 2, bw / 2))
    }
}
