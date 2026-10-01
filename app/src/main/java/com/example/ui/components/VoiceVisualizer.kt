package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.ShivaiState
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonViolet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HolographicVoiceVisualizer(
    amplitude: Float,
    state: ShivaiState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hologram_rotation")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val smoothedAmp = remember { Animatable(0f) }
    LaunchedEffect(amplitude) {
        smoothedAmp.animateTo(
            targetValue = amplitude,
            animationSpec = tween(durationMillis = 60, easing = LinearEasing)
        )
    }

    val primaryColor = when (state) {
        ShivaiState.LISTENING -> NeonCyan
        ShivaiState.THINKING -> NeonPurple
        ShivaiState.SPEAKING -> NeonGreen
        ShivaiState.INTERRUPTED -> NeonRed
        ShivaiState.CONNECTING -> NeonViolet
        ShivaiState.CONNECTED -> NeonCyan
        ShivaiState.ERROR -> NeonRed
        ShivaiState.STANDBY -> NeonCyan
    }

    Box(
        modifier = modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = (size.minDimension / 2) * 0.7f * pulseScale
            val ampOffset = smoothedAmp.value * 35f

            // Outer holographic glow rings
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent),
                    center = center,
                    radius = baseRadius + ampOffset + 25f
                ),
                radius = baseRadius + ampOffset + 25f,
                center = center
            )

            // Outer tech orbit ring
            drawCircle(
                color = primaryColor.copy(alpha = 0.35f),
                radius = baseRadius + ampOffset,
                center = center,
                style = Stroke(width = 2f)
            )

            // Inner orbit ring
            drawCircle(
                color = NeonViolet.copy(alpha = 0.5f),
                radius = (baseRadius * 0.75f) + (ampOffset * 0.5f),
                center = center,
                style = Stroke(width = 1.5f)
            )

            // Orbital energy nodes / ticks
            val tickCount = 28
            for (i in 0 until tickCount) {
                val angleDeg = (i * (360f / tickCount)) + rotationAngle
                val angleRad = (angleDeg * PI / 180f).toFloat()

                val waveAmp = if (state == ShivaiState.LISTENING || state == ShivaiState.SPEAKING) {
                    sin(angleDeg * 0.1f + rotationAngle * 0.05f) * (smoothedAmp.value * 18f)
                } else 0f

                val startR = baseRadius - 6f + waveAmp
                val endR = baseRadius + 8f + waveAmp + (smoothedAmp.value * 12f)

                val startX = center.x + startR * cos(angleRad)
                val startY = center.y + startR * sin(angleRad)
                val endX = center.x + endR * cos(angleRad)
                val endY = center.y + endR * sin(angleRad)

                drawLine(
                    color = if (i % 2 == 0) primaryColor else NeonViolet,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (i % 4 == 0) 3.5f else 1.8f,
                    cap = StrokeCap.Round
                )
            }

            // Core glowing sphere
            val coreRadius = (baseRadius * 0.45f) + (smoothedAmp.value * 15f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        primaryColor,
                        NeonViolet.copy(alpha = 0.8f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = center
            )
        }
    }
}
