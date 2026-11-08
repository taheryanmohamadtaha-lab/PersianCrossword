package com.marzieh.crossword

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

/** شخصیت سخنگو: تصویر + انیمیشن‌های کد (نفس‌کشیدن، پلک، پرش، لرزش، حرف زدن) + لوازم جانبی. */
@Composable
fun CharacterView(app: App, size: Dp, modifier: Modifier = Modifier) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val inf = rememberInfiniteTransition(label = "char")
        val bob by inf.animateFloat(-1f, 1f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
        val talk by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(210, easing = LinearEasing), RepeatMode.Reverse), label = "talk")
        val jump by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(360, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "jump")
        val wig by inf.animateFloat(-1f, 1f, infiniteRepeatable(tween(90, easing = LinearEasing), RepeatMode.Reverse), label = "wig")
        var blink by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(2400L + Random.nextLong(2400))
                blink = true; delay(130); blink = false
            }
        }
        val mood = app.mood
        val s = size.value
        val dy = when (mood) {
            Mood.Happy, Mood.Cheer -> -jump * s * 0.10f
            else -> bob * s * 0.012f
        }
        val rot = when (mood) {
            Mood.Wrong -> wig * 5f
            Mood.Think -> -7f
            Mood.Cheer -> wig * 4f
            else -> 0f
        }
        val sy = if (app.talking) 1f + talk * 0.03f else 1f + bob * 0.005f
        Box(
            modifier.size(size).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { app.repeatLine() }
        ) {
            Box(
                Modifier.size(size).graphicsLayer {
                    translationY = dy * density
                    rotationZ = rot
                    scaleY = sy
                    scaleX = 1f + (if (app.talking) -talk * 0.012f else 0f)
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
            ) {
                Image(painterResource(R.drawable.character), contentDescription = null, modifier = Modifier.size(size))
                Canvas(Modifier.size(size)) {
                    if (blink) {
                        val w = this.size.width
                        val skin = Color(0xFFFDE9DC)
                        for (cx in listOf(0.355f, 0.575f)) {
                            val rx = 0.062f * w; val cy = 0.373f * w
                            drawOval(skin, Offset(cx * w - rx, cy - rx), Size(rx * 2, rx * 2))
                            drawArc(
                                Color(0xFF46302B), 10f, 160f, false,
                                Offset(cx * w - rx * 0.8f, cy - rx * 0.5f), Size(rx * 1.6f, rx),
                                style = Stroke(width = w * 0.007f, cap = StrokeCap.Round)
                            )
                        }
                    }
                }
                val acc = Accessories.getOrNull(app.store.outfit)?.second ?: "—"
                if (acc != "—") {
                    val (fx, fy, fs) = when (app.store.outfit) {
                        1 -> Triple(0.34f, -0.04f, 0.30f)
                        2 -> Triple(0.60f, 0.07f, 0.20f)
                        3 -> Triple(0.27f, 0.27f, 0.46f)
                        4 -> Triple(0.34f, -0.06f, 0.30f)
                        5 -> Triple(0.20f, 0.07f, 0.20f)
                        else -> Triple(0.70f, 0.18f, 0.20f)
                    }
                    androidx.compose.material3.Text(
                        acc, fontSize = (s * fs).sp,
                        modifier = Modifier.align(Alignment.TopStart).absoluteOffset(x = size * fx, y = size * fy)
                    )
                }
            }
            val badge = when (mood) { Mood.Happy -> "✨"; Mood.Wrong -> "❗"; Mood.Think -> "💭"; Mood.Cheer -> "🎉"; else -> "" }
            if (badge.isNotEmpty()) {
                androidx.compose.material3.Text(
                    badge, fontSize = (s * 0.18f).sp,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }
    }
}
