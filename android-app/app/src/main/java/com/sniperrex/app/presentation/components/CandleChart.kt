package com.sniperrex.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.sniperrex.app.domain.engine.Candle
import com.sniperrex.app.domain.engine.Indicators
import com.sniperrex.app.presentation.theme.CyberCrimson
import com.sniperrex.app.presentation.theme.CyberLime
import com.sniperrex.app.presentation.theme.TextSubtle

/**
 * Chart candlestick native Compose.
 *
 * Dipakai menggantikan WebView TradingView: WebView tidak bisa dijamin hidup
 * di semua perangkat (Xiaomi memblokir sebagian konten), sedangkan Canvas
 * selalu jalan dan bisa menampilkan level signal kita sendiri.
 */
@Composable
fun CandleChart(
    candles: List<Candle>,
    modifier: Modifier = Modifier,
    showEma: Boolean = true,
) {
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        color = TextSubtle,
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
    )

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            if (candles.isEmpty()) return@Canvas

            val visible = candles.takeLast(60)
            val padTop = 26f
            val padBottom = 18f
            val axisWidth = 52f

            val hi = visible.maxOf { it.high }
            val lo = visible.minOf { it.low }
            val span = (hi - lo).takeIf { it > 0.0 } ?: 1.0
            val plotW = size.width - axisWidth
            val plotH = size.height - padTop - padBottom
            val step = plotW / visible.size
            val bodyW = (step * 0.62f).coerceAtLeast(1.6f)

            fun yOf(price: Double): Float =
                (padTop + plotH - ((price - lo) / span * plotH)).toFloat()

            // grid horizontal + label harga
            val gridColor = Color.White.copy(alpha = 0.06f)
            for (i in 0..3) {
                val y = padTop + plotH * i / 3f
                drawLine(gridColor, Offset(0f, y), Offset(plotW, y), strokeWidth = 1f)
                val price = hi - span * i / 3.0
                drawText(
                    textMeasurer = measurer,
                    text = formatPrice(price),
                    topLeft = Offset(plotW + 6f, y - 6f),
                    style = labelStyle,
                )
            }

            // candle
            visible.forEachIndexed { index, candle ->
                val cx = index * step + step / 2f
                val up = candle.close >= candle.open
                val color = if (up) CyberLime else CyberCrimson
                drawLine(
                    color = color.copy(alpha = 0.85f),
                    start = Offset(cx, yOf(candle.high)),
                    end = Offset(cx, yOf(candle.low)),
                    strokeWidth = 1.2f,
                )
                val openY = yOf(candle.open)
                val closeY = yOf(candle.close)
                val top = minOf(openY, closeY)
                val height = kotlin.math.abs(closeY - openY).coerceAtLeast(1.2f)
                drawRect(
                    color = color,
                    topLeft = Offset(cx - bodyW / 2f, top),
                    size = androidx.compose.ui.geometry.Size(bodyW, height),
                )
            }

            // EMA 21 / 55
            if (showEma && visible.size > 55) {
                val closes = DoubleArray(visible.size) { visible[it].close }
                listOf(21 to Color(0xFF00E5FF), 55 to Color(0xFF9B7BFF)).forEach { (period, color) ->
                    val serie = Indicators.ema(closes, period)
                    val path = Path()
                    var started = false
                    serie.forEachIndexed { i, value ->
                        if (value.isNaN()) return@forEachIndexed
                        val x = i * step + step / 2f
                        val y = yOf(value)
                        if (!started) {
                            path.moveTo(x, y); started = true
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    if (started) {
                        drawPath(path, color.copy(alpha = 0.75f), style = Stroke(width = 1.6f))
                    }
                }
            }

            // harga terakhir
            val last = visible.last().close
            val lastY = yOf(last)
            val lastColor = if (visible.last().close >= visible.last().open) CyberLime else CyberCrimson
            drawLine(
                lastColor.copy(alpha = 0.5f),
                Offset(0f, lastY), Offset(plotW, lastY),
                strokeWidth = 1f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
            )
            drawText(
                textMeasurer = measurer,
                text = formatPrice(last),
                topLeft = Offset(size.width - axisWidth + 6f, lastY - 6f),
                style = labelStyle.copy(color = lastColor),
            )
        }
    }
}

private fun formatPrice(price: Double): String = when {
    price >= 1000 -> String.format("%.2f", price)
    price >= 100 -> String.format("%.3f", price)
    price >= 1 -> String.format("%.5f", price)
    else -> String.format("%.5f", price)
}
