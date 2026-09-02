package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceBorder
import com.example.ui.theme.MonospaceStyle
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.NeonVioletLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SystemMetricCircularCard(
    title: String,
    currentValue: Float,
    maxValue: Float,
    unit: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val progress = (currentValue / maxValue).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500),
        label = "gauge_progress"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CyberSurface)
            .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MonospaceStyle.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Circular Gauge
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(110.dp)
        ) {
            Canvas(modifier = Modifier.size(100.dp)) {
                val strokeWidth = 9.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                val arcSize = Size(diameter, diameter)

                // Track arc (240 degrees)
                drawArc(
                    color = Color(0xFF1E293B),
                    startAngle = 150f,
                    sweepAngle = 240f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Filled arc
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to accentColor.copy(alpha = 0.6f),
                        1.0f to accentColor
                    ),
                    startAngle = 150f,
                    sweepAngle = 240f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.1f", currentValue),
                    style = MonospaceStyle.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = unit,
                    style = MonospaceStyle.copy(
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = subtitle,
            style = MonospaceStyle.copy(
                fontSize = 11.sp,
                color = TextSecondary
            )
        )
    }
}

@Composable
fun CpuHistorySparkline(
    history: List<Float>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberSurface)
            .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CPU UTILIZATION TIMELINE",
                style = MonospaceStyle.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            val current = history.lastOrNull() ?: 0f
            val statusColor = when {
                current > 75f -> NeonRed
                current > 45f -> NeonAmber
                else -> NeonGreen
            }
            Text(
                text = "${String.format("%.1f", current)}% LIVE",
                style = MonospaceStyle.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Canvas Sparkline
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .testTag("cpu_sparkline")
        ) {
            if (history.isEmpty()) return@Canvas

            val maxVal = 100f
            val widthStep = size.width / (history.size - 1).coerceAtLeast(1)

            // Draw horizontal grid lines
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(0f, size.height * 0.25f),
                end = Offset(size.width, size.height * 0.25f),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(0f, size.height * 0.75f),
                end = Offset(size.width, size.height * 0.75f),
                strokeWidth = 1.dp.toPx()
            )

            val linePath = Path()
            val fillPath = Path()

            history.forEachIndexed { index, value ->
                val x = index * widthStep
                val normalizedY = size.height - (value / maxVal) * size.height
                if (index == 0) {
                    linePath.moveTo(x, normalizedY)
                    fillPath.moveTo(x, size.height)
                    fillPath.lineTo(x, normalizedY)
                } else {
                    linePath.lineTo(x, normalizedY)
                    fillPath.lineTo(x, normalizedY)
                }
            }

            fillPath.lineTo((history.size - 1) * widthStep, size.height)
            fillPath.close()

            // Draw gradient area under line
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        NeonViolet.copy(alpha = 0.35f),
                        NeonViolet.copy(alpha = 0.02f)
                    )
                )
            )

            // Draw stroke line
            drawPath(
                path = linePath,
                color = NeonVioletLight,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw current point pulse circle
            val lastX = (history.size - 1) * widthStep
            val lastY = size.height - (history.last() / maxVal) * size.height
            drawCircle(
                color = NeonCyan,
                radius = 4.dp.toPx(),
                center = Offset(lastX, lastY)
            )
        }
    }
}
