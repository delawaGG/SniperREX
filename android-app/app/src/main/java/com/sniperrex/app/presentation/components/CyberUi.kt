package com.sniperrex.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sniperrex.app.presentation.theme.BorderGray
import com.sniperrex.app.presentation.theme.CosmicDark
import com.sniperrex.app.presentation.theme.CyberAqua
import com.sniperrex.app.presentation.theme.CyberCrimson
import com.sniperrex.app.presentation.theme.CyberLime
import com.sniperrex.app.presentation.theme.GridLine
import com.sniperrex.app.presentation.theme.PanelDark
import com.sniperrex.app.presentation.theme.PanelElevated
import com.sniperrex.app.presentation.theme.TextPrimary
import com.sniperrex.app.presentation.theme.TextSubtle

/** Latar: gradien gelap + grid tipis + glow neon di atas. */
@Composable
fun CyberBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF070A0A), CosmicDark, Color(0xFF040505)),
                )
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val step = 56f
            var x = 0f
            while (x < size.width) {
                drawLine(GridLine, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(GridLine, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += step
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x2AA3E635), Color.Transparent),
                    center = Offset(size.width * 0.75f, 0f),
                    radius = size.width * 0.9f,
                ),
                radius = size.width * 0.9f,
                center = Offset(size.width * 0.75f, 0f),
            )
        }
        content()
    }
}

/** Kartu panel dengan border tipis + aksen. */
@Composable
fun GlowCard(
    modifier: Modifier = Modifier,
    accent: Color = BorderGray,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(PanelElevated, PanelDark)))
            .border(1.dp, accent.copy(alpha = 0.55f), shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(14.dp),
        content = content,
    )
}

@Composable
fun CyberTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Text(
                text = "‹",
                fontFamily = FontFamily.Monospace,
                fontSize = 26.sp,
                color = CyberLime,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onBack() }
                    .padding(horizontal = 10.dp),
            )
        } else {
            LogoMark(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title, color = TextPrimary, fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 1.sp,
            )
            if (subtitle != null) {
                Text(subtitle, color = TextSubtle, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
        actions()
    }
}

/** Ring skor 0-100 dengan warna berjenjang. */
@Composable
fun ScoreRing(score: Int, size: Dp = 58.dp, label: String = "SCORE") {
    val accent = when {
        score >= 70 -> CyberLime
        score >= 50 -> CyberAqua
        else -> CyberCrimson
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 5.dp.toPx()
            drawArc(
                color = BorderGray, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(accent.copy(alpha = 0.35f), accent)),
                startAngle = -90f, sweepAngle = 360f * (score.coerceIn(0, 100) / 100f),
                useCenter = false, style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                score.toString(), color = TextPrimary, fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold, fontSize = 17.sp,
            )
            Text(label, color = TextSubtle, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun DirectionPill(direction: String) {
    val isLong = direction.uppercase() == "LONG"
    val accent = if (isLong) CyberLime else CyberCrimson
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(accent.copy(alpha = 0.15f))
            .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = if (isLong) "▲ LONG" else "▼ SHORT",
            color = accent, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun StatTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(PanelDark)
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label, color = TextSubtle, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(2.dp))
        Text(
            value, color = accent, fontSize = 18.sp, fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun NeonButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = CyberLime,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) accent else BorderGray,
            contentColor = CosmicDark,
            disabledContainerColor = BorderGray,
            disabledContentColor = TextSubtle,
        ),
        modifier = modifier.fillMaxWidth().height(50.dp),
    ) {
        Text(text, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, accent: Color = CyberAqua, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(text, color = accent, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LevelRow(label: String, value: String, accent: Color = TextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = TextSubtle, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = accent, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = TextSubtle,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}
