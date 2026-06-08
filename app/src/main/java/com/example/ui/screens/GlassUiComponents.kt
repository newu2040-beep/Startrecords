package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    borderWidth: Dp = 1.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background.red > 0.5f
    // Frosted layer color: highly transparent white or highly transparent dark matching Professional Polish theme
    val glassColor = if (isLight) Color(0xD9FFFFFF) else Color(0x12FFFFFF)
    val borderColor = if (isLight) Color(0x33000000) else Color(0x1DFFFFFF)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(glassColor)
            .border(
                width = borderWidth,
                brush = Brush.radialGradient(
                    colors = listOf(borderColor, borderColor.copy(alpha = 0.05f)),
                    center = Offset(0f, 0f)
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(16.dp),
        content = content
    )
}

@Composable
fun CircularStorageGauge(
    percentage: Float,
    storageUsedStr: String,
    storageTotalStr: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val animatedProgress by animateFloatAsState(
        targetValue = percentage,
        animationSpec = twinSpring(),
        label = "gauge"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val tracksColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        Canvas(modifier = Modifier.size(140.dp)) {
            val strokeWidth = 14.dp.toPx()
            
            // Draw background track
            drawArc(
                color = tracksColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Draw active progress
            drawArc(
                brush = Brush.linearGradient(
                    colors = listOf(accentColor, accentColor.copy(alpha = 0.4f))
                ),
                startAngle = 135f,
                sweepAngle = 270f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(percentage * 100).toInt()}%",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$storageUsedStr / $storageTotalStr",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = "Studio Space Used",
                fontSize = 10.sp,
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun OscillatingWaveform(
    amplitude: Float,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    neonColor: Color = MaterialTheme.colorScheme.primary
) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phaseOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val ampScale by animateFloatAsState(
        targetValue = if (isRecording) amplitude else 0.02f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ampScale"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val midY = height / 2

        val path1 = Path()
        val path2 = Path()

        path1.moveTo(0f, midY)
        path2.moveTo(0f, midY)

        val pointsCount = 100
        val waveFrequency = 4f

        for (i in 0..pointsCount) {
            val x = (i.toFloat() / pointsCount.toFloat()) * width
            // Normalizing Gaussian bell envelope: max wave altitude in middle, tapering at screen borders
            val envelope = sin((i.toFloat() / pointsCount.toFloat()) * Math.PI).toFloat()

            // Main Sine Wave
            val yAction1 = midY + sin((i.toFloat() / pointsCount.toFloat()) * Math.PI * waveFrequency + phaseOffset).toFloat() * 120.dp.toPx() * ampScale * envelope
            // Modulated Harmony Wave
            val yAction2 = midY + sin((i.toFloat() / pointsCount.toFloat()) * Math.PI * waveFrequency * 1.5 + phaseOffset * 1.2).toFloat() * 80.dp.toPx() * ampScale * envelope * 0.8f

            if (i == 0) {
                path1.moveTo(x, yAction1)
                path2.moveTo(x, yAction2)
            } else {
                path1.lineTo(x, yAction1)
                path2.lineTo(x, yAction2)
            }
        }

        // Draw multiple glowing lines
        drawPath(
            path = path1,
            color = neonColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        drawPath(
            path = path2,
            color = neonColor.copy(alpha = 0.5f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

fun <T> twinSpring(): SpringSpec<T> {
    return spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
}
