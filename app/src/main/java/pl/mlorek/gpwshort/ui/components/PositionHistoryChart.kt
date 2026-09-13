package pl.mlorek.gpwshort.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.mlorek.gpwshort.R
import pl.mlorek.gpwshort.domain.model.PositionHistoryPoint
import pl.mlorek.gpwshort.util.formatDate

/**
 * Prosty wykres liniowy narysowany na [Canvas] Compose, zamiast biblioteki Vico/MPAndroidChart.
 *
 * Decyzja: dokładne API Vico (generics CartesianChart/rememberLineCartesianLayer vs. starsze
 * Chart/chartEntryModelProducer) różni się mocno między wersjami 1.x i 2.x, a w środowisku,
 * w którym powstał ten kod, dostęp do Maven Central/repozytorium Vico był zablokowany, więc
 * nie dało się zweryfikować dokładnej sygnatury dla konkretnej wersji. Żeby nie ryzykować
 * niekompilującego się kodu opartego na zgadywanej sygnaturze zewnętrznej biblioteki, wykres
 * zaimplementowano samodzielnie na w pełni udokumentowanym, stabilnym Compose Canvas API.
 */
@Composable
fun PositionHistoryChart(points: List<PositionHistoryPoint>, modifier: Modifier = Modifier) {
    if (points.size < 2) {
        Box(modifier = modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.detail_no_history), style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val fillBrush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0f)))

    val minValue = points.minOf { it.totalShortPercent }
    val maxValue = points.maxOf { it.totalShortPercent }
    val valueRange = (maxValue - minValue).let { if (it > 0.0001) it else 1.0 }

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(200.dp).padding(vertical = 8.dp)) {
            val stepCount = (points.size - 1).coerceAtLeast(1)
            val widthStep = size.width / stepCount

            fun yFor(value: Double): Float =
                size.height - ((value - minValue) / valueRange * size.height).toFloat()

            val gridLines = 4
            repeat(gridLines + 1) { i ->
                val y = size.height / gridLines * i
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
            }

            val linePath = Path()
            val fillPath = Path()
            points.forEachIndexed { index, point ->
                val x = widthStep * index
                val y = yFor(point.totalShortPercent)
                if (index == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, size.height)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo(widthStep * stepCount, size.height)
            fillPath.close()

            drawPath(path = fillPath, brush = fillBrush)
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )

            val lastX = widthStep * stepCount
            val lastY = yFor(points.last().totalShortPercent)
            drawCircle(color = lineColor, radius = 6f, center = Offset(lastX, lastY))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDate(points.first().date), style = MaterialTheme.typography.labelSmall)
            Text(formatDate(points.last().date), style = MaterialTheme.typography.labelSmall)
        }
    }
}
