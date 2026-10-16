package com.blueproof.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blueproof.ui.theme.*

@Composable
fun BarChart(
    data: List<Pair<String, Float>>,
    maxValue: Float,
    barColor: Color = NavyPrimary,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty() || maxValue <= 0f) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val width = size.width
            val height = size.height
            val bottomPadding = 30f
            val chartHeight = height - bottomPadding
            val barCount = data.size
            val barWidth = (width / (barCount * 2f)).coerceAtMost(48f)
            val spacing = width / barCount

            data.forEachIndexed { index, (label, value) ->
                val barHeight = (value / maxValue) * (chartHeight - 20f)
                val x = spacing * index + (spacing - barWidth) / 2f
                val y = chartHeight - barHeight

                // Draw bar
                drawRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight)
                )

                // Draw value on top of bar
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.DKGRAY
                        textSize = 24f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    drawText(
                        value.toInt().toString(),
                        x + barWidth / 2f,
                        y - 6f,
                        paint
                    )
                    // Draw label below bar
                    drawText(
                        label,
                        x + barWidth / 2f,
                        height - 4f,
                        paint
                    )
                }
            }
        }
    }
}

@Composable
fun DonutChart(
    segments: List<Pair<String, Float>>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val total = segments.sumOf { it.second.toDouble() }.toFloat()

    if (total <= 0f) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Donut canvas
        Canvas(modifier = Modifier.size(140.dp)) {
            val strokeWidth = 32f
            val radius = (size.minDimension - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            var startAngle = -90f
            segments.forEachIndexed { index, (_, value) ->
                val sweepAngle = (value / total) * 360f
                val color = colors.getOrElse(index) { NavyPrimary }
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth)
                )
                startAngle += sweepAngle
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        // Legend
        Column(verticalArrangement = Arrangement.Center) {
            segments.forEachIndexed { index, (label, value) ->
                val color = colors.getOrElse(index) { NavyPrimary }
                val pct = if (total > 0f) (value / total * 100f).toInt() else 0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawCircle(color = color)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$label: ${value.toInt()} ($pct%)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun LineChart(
    data: List<Pair<String, Float>>,
    lineColor: Color = TealAccent,
    modifier: Modifier = Modifier
) {
    if (data.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Need at least 2 sessions for trend",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxVal = (data.maxOfOrNull { it.second } ?: 100f).coerceAtLeast(10f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        val width = size.width
        val height = size.height
        val bottomPad = 24f
        val chartHeight = height - bottomPad
        val spacing = width / (data.size - 1)

        val points = data.mapIndexed { index, (_, value) ->
            val x = spacing * index
            val y = chartHeight - (value / maxVal) * (chartHeight - 20f)
            Offset(x, y)
        }

        // Draw line segments
        for (i in 0 until points.size - 1) {
            drawLine(
                color = lineColor,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }

        // Draw points and labels
        points.forEachIndexed { index, point ->
            drawCircle(color = lineColor, radius = 6f, center = point)
            drawCircle(color = SurfaceWhite, radius = 3f, center = point)

            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = 22f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                drawText("${data[index].second.toInt()}%", point.x, point.y - 10f, paint)
                drawText(data[index].first, point.x, height, paint)
            }
        }
    }
}
