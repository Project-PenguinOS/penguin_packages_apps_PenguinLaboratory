/*
 * Copyright (C) 2026 The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.penguin.laboratory.ui

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// A droplet of glass: magnifies what's under it, pulls the rim in like the edge of a lens, and
// splits the colours slightly there.
private const val BUBBLE =
    """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float depth;
uniform float zoom;
uniform float chroma;
half4 main(float2 p) {
    float2 c = size * 0.5;
    float2 q = abs(p - c) - (c - radius);
    float d = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
    float t = clamp(1.0 + d / depth, 0.0, 1.0);
    t = t * t;
    float2 dir = p - c;
    dir = dir / max(length(dir), 1.0);
    float2 s = c + (p - c) / zoom - dir * t * depth * 0.9;
    if (chroma <= 0.0) {
        return content.eval(s);
    }
    float2 ca = dir * t * chroma;
    half4 g = content.eval(s);
    return half4(content.eval(s + ca).r, g.g, content.eval(s - ca).b, g.a);
}
"""

private val Pill = RoundedCornerShape(percent = 50)

/** Tabs with a lens under the chosen one that can be dragged between tabs. */
@Composable
internal fun GlassTabBar(
    items: List<TabItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    // The gesture below outlives recompositions, so it reads these through state.
    val current by rememberUpdatedState(selected)
    val select by rememberUpdatedState(onSelect)
    var pressing by remember { mutableStateOf(false) }
    var velocity by remember { mutableFloatStateOf(0f) }
    var rowOrigin by remember { mutableStateOf(Offset.Zero) }
    val lens = remember { Animatable(Float.NaN) }
    val press by
        animateFloatAsState(
            if (pressing) 1f else 0f,
            spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
            label = "press",
        )
    val stretch by
        animateFloatAsState(
            (abs(velocity) / 5000f).coerceAtMost(0.28f),
            spring(stiffness = Spring.StiffnessMedium),
            label = "stretch",
        )
    val backShader = remember { RuntimeShader(BUBBLE) }
    val frontShader = remember { RuntimeShader(BUBBLE) }

    GlassSurface(true, backdrop, modifier.fillMaxWidth().height(BarHeight)) {
        BoxWithConstraints(
            Modifier.fillMaxSize().padding(4.dp).onGloballyPositioned {
                rowOrigin = it.positionInRoot()
            }
        ) {
            val itemWidth = maxWidth / items.size
            val itemPx = with(density) { itemWidth.toPx() }
            val rowPx = with(density) { maxHeight.toPx() }
            val totalPx = itemPx * items.size

            LaunchedEffect(selected, itemPx) {
                if (lens.value.isNaN()) lens.snapTo(itemPx * selected)
                else if (!pressing) {
                    lens.animateTo(
                        itemPx * selected,
                        spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow),
                    )
                }
            }
            val left = if (lens.value.isNaN()) itemPx * selected else lens.value

            TabRow(items, selected = -1, itemWidth, tint = colors.onSurfaceVariant)

            // The lens, centred on its tab and grown by the press and the drag speed.
            val w = itemPx * (1f + 0.34f * press) * (1f + stretch)
            val h = rowPx * (1f + 0.42f * press) * (1f - stretch * 0.35f)
            val x = left + itemPx / 2f - w / 2f
            val y = rowPx / 2f - h / 2f
            val depth = with(density) { (7.dp + 7.dp * press).toPx() }
            val zoom = 1f + 0.2f * press
            val chroma = with(density) { 2.5.dp.toPx() } * press
            // Frosted at rest; while pressed a lighter frost, so colours read through but text doesn't.
            val blur = with(density) { (10.dp * (1f - press) + 4.dp * press).toPx() }
            fun RuntimeShader.configure() {
                setFloatUniform("size", w, h)
                setFloatUniform("radius", h / 2f)
                setFloatUniform("depth", depth)
                setFloatUniform("zoom", zoom)
                setFloatUniform("chroma", chroma)
            }
            Box(
                Modifier.placeAt(x, y, w, h)
                    .graphicsLayer {
                        shadowElevation = 10.dp.toPx() * press
                        shape = Pill
                        clip = false
                    }
                    .drawWithContent {
                        drawContent()
                        val outline = Pill.createOutline(size, layoutDirection, this)
                        val rim =
                            Brush.verticalGradient(
                                0f to Color.White.copy(alpha = if (dark) 0.5f else 0.95f),
                                0.5f to Color.White.copy(alpha = 0.15f + 0.2f * press),
                                1f to Color.White.copy(alpha = if (dark) 0.3f else 0.6f),
                            )
                        drawOutline(outline, rim, style = Stroke((1f + 0.6f * press).dp.toPx()))
                    }
                    .clip(Pill)
            ) {
                // What's behind the bar: frosted at rest, clear and magnified while pressed.
                Box(
                    Modifier.fillMaxSize()
                        .graphicsLayer {
                            backShader.configure()
                            val bubble = RenderEffect.createRuntimeShaderEffect(backShader, "content")
                            renderEffect =
                                (if (blur > 0.5f) {
                                        RenderEffect.createChainEffect(
                                            bubble,
                                            RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.CLAMP),
                                        )
                                    } else bubble)
                                    .asComposeRenderEffect()
                        }
                        .drawBehind {
                            drawRect(colors.surfaceContainer)
                            translate(backdrop.origin.x - rowOrigin.x - x, backdrop.origin.y - rowOrigin.y - y) {
                                drawLayer(backdrop.layer)
                            }
                        }
                )
                // The frost and the accent tint fade as the droplet clears.
                Box(
                    Modifier.fillMaxSize()
                        .background(
                            (if (dark) colors.surfaceContainerHigh else Color.White)
                                .copy(alpha = (if (dark) 0.5f else 0.55f) * (1f - press) + 0.22f * press)
                        )
                        .background(colors.primary.copy(alpha = 0.10f * (1f - press)))
                )
                // The tabs again, in the accent colour, magnified with the backdrop.
                Box(
                    Modifier.fillMaxSize().graphicsLayer {
                        frontShader.configure()
                        renderEffect =
                            RenderEffect.createRuntimeShaderEffect(frontShader, "content")
                                .asComposeRenderEffect()
                    }
                ) {
                    Box(Modifier.placeAt(-x, -y, totalPx, rowPx)) {
                        TabRow(items, selected = selected, itemWidth, tint = colors.primary)
                    }
                }
            }

            // Touch: press, slide, release.
            Box(
                Modifier.fillMaxSize().pointerInput(items.size, itemPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        pressing = true
                        val tracker = VelocityTracker()
                        tracker.addPosition(down.uptimeMillis, down.position)
                        var fingerX = down.position.x
                        var over = (fingerX / itemPx).toInt().coerceIn(0, items.lastIndex)
                        val slack = itemPx * 0.2f
                        fun target(fx: Float) =
                            (fx - itemPx / 2f).coerceIn(-slack, itemPx * items.lastIndex + slack)
                        scope.launch {
                            lens.animateTo(
                                target(fingerX),
                                spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
                            )
                        }
                        drag(down.id) { change ->
                            fingerX = change.position.x
                            tracker.addPosition(change.uptimeMillis, change.position)
                            velocity = tracker.calculateVelocity().x
                            scope.launch { lens.snapTo(target(fingerX)) }
                            val now = (fingerX / itemPx).toInt().coerceIn(0, items.lastIndex)
                            if (now != over) {
                                over = now
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            }
                            change.consume()
                        }
                        pressing = false
                        velocity = 0f
                        val chosen = (fingerX / itemPx).toInt().coerceIn(0, items.lastIndex)
                        scope.launch {
                            lens.animateTo(
                                itemPx * chosen,
                                spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
                            )
                        }
                        if (chosen != current) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        }
                        select(chosen)
                    }
                }
            )
        }
    }
}

@Composable
private fun TabRow(items: List<TabItem>, selected: Int, itemWidth: Dp, tint: Color) {
    Row(Modifier.fillMaxSize()) {
        items.forEachIndexed { index, item ->
            Column(
                Modifier.width(itemWidth).fillMaxHeight().semantics {
                    role = Role.Tab
                    this.selected = index == selected
                },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painterResource(item.icon),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    stringResource(item.label),
                    color = tint,
                    fontSize = 11.sp,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

/** Places this at ([x], [y]) with size [w] x [h], taking no space in the parent. */
private fun Modifier.placeAt(x: Float, y: Float, w: Float, h: Float): Modifier = layout { m, _ ->
    val p = m.measure(Constraints.fixed(w.roundToInt().coerceAtLeast(1), h.roundToInt().coerceAtLeast(1)))
    layout(0, 0) { p.place(x.roundToInt(), y.roundToInt()) }
}
