package com.sniperrex.app.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sniperrex.app.domain.model.MarketPulse
import com.sniperrex.app.domain.model.Pairs
import com.sniperrex.app.domain.model.PulseState
import com.sniperrex.app.domain.model.Signal
import com.sniperrex.app.presentation.components.CandleChart
import com.sniperrex.app.presentation.components.CyberBackground
import com.sniperrex.app.presentation.components.CyberTopBar
import com.sniperrex.app.presentation.components.DirectionPill
import com.sniperrex.app.presentation.components.GhostButton
import com.sniperrex.app.presentation.components.GlowCard
import com.sniperrex.app.presentation.components.NeonButton
import com.sniperrex.app.presentation.components.ScoreRing
import com.sniperrex.app.presentation.components.SectionLabel
import com.sniperrex.app.presentation.components.StatTile
import com.sniperrex.app.presentation.theme.BorderGray
import com.sniperrex.app.presentation.theme.CyberAqua
import com.sniperrex.app.presentation.theme.CyberCrimson
import com.sniperrex.app.presentation.theme.CyberLime
import com.sniperrex.app.presentation.theme.CosmicDark
import com.sniperrex.app.presentation.theme.PanelDark
import com.sniperrex.app.presentation.theme.TextPrimary
import com.sniperrex.app.presentation.theme.TextSubtle

private val TIMEFRAMES = listOf("M15", "M30", "H1", "H4")
private val AmberGlow = Color(0xFFFFB020)

@Composable
fun DashboardScreen(
    onOpenSettings: () -> Unit,
    onOpenSignals: () -> Unit,
    onScan: (String) -> Unit,
    onOpenSignal: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val signals by viewModel.signals.collectAsStateWithLifecycle()
    val chart by viewModel.chart.collectAsStateWithLifecycle()
    val radar by viewModel.radar.collectAsStateWithLifecycle()
    val sweep by viewModel.sweep.collectAsStateWithLifecycle()

    val longs = signals.count { it.direction.name == "LONG" }
    val avgScore = if (signals.isEmpty()) 0 else signals.sumOf { it.quantScore } / signals.size

    CyberBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            CyberTopBar(
                title = "SNIPERREX",
                subtitle = "smc engine · on-device",
                actions = { GhostButton("SET", accent = CyberAqua) { onOpenSettings() } },
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 150.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("SIGNAL", signals.size.toString(), CyberLime, Modifier.weight(1f))
                        StatTile("LONG", longs.toString(), CyberAqua, Modifier.weight(1f))
                        StatTile("AVG SCORE", avgScore.toString(), CyberLime, Modifier.weight(1f))
                    }
                }

                // --- pemilih pair ---
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp),
                    ) {
                        items(Pairs.ALL) { info ->
                            val active = info.symbol == chart.pair
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active) CyberLime.copy(alpha = 0.14f) else PanelDark)
                                    .border(
                                        1.dp,
                                        if (active) CyberLime else BorderGray,
                                        RoundedCornerShape(10.dp),
                                    )
                                    .clickable { viewModel.select(info.symbol) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    info.symbol,
                                    color = if (active) CyberLime else TextSubtle,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                // --- chart native ---
                item {
                    GlowCard(accent = CyberLime, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        chart.pair, color = CyberLime,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    chart.last?.let { last ->
                                        Text(
                                            formatPrice(last.close),
                                            color = TextPrimary,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                        )
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "${if (chart.changePct >= 0) "+" else ""}${"%.2f".format(chart.changePct)}%",
                                        color = if (chart.changePct >= 0) CyberLime else CyberCrimson,
                                        fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                                    )
                                }
                                Text(
                                    "EMA 21 · 55 · candle native",
                                    color = TextSubtle, fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(TIMEFRAMES) { tf ->
                                    val active = tf == chart.timeframe
                                    Text(
                                        tf,
                                        color = if (active) CyberLime else TextSubtle,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (active) CosmicDark else PanelDark)
                                            .clickable { viewModel.selectTimeframe(tf) }
                                            .padding(horizontal = 6.dp, vertical = 3.dp),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CosmicDark)
                                .border(1.dp, CyberLime.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                        ) {
                            when {
                                chart.loading && chart.candles.isEmpty() -> CenterHint("memuat candle…")
                                chart.candles.isEmpty() -> CenterHint(
                                    chart.error ?: "chart kosong — isi API key di SET lalu buka lagi",
                                )
                                else -> CandleChart(
                                    candles = chart.candles,
                                    modifier = Modifier.fillMaxSize().padding(4.dp),
                                )
                            }
                        }
                        if (chart.candles.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                Modifier.fillMaxWidth().clickable { viewModel.loadCandles() },
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    "MUAT ULANG CHART",
                                    color = CyberAqua, fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                // --- radar market ---
                item {
                    GlowCard(accent = CyberAqua, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "RADAR MARKET",
                                color = CyberAqua,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            )
                            Text(
                                if (radar.loading) "memindai…" else "M15 · ketuk untuk chart",
                                color = TextSubtle, fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        if (radar.pulses.isEmpty()) {
                            Text(
                                if (radar.loading) "mengambil snapshot pair…"
                                else "radar kosong — butuh API key aktif",
                                color = TextSubtle, fontSize = 11.sp,
                            )
                        } else {
                            radar.pulses.forEach { pulse ->
                                PulseRow(pulse) {
                                    viewModel.select(pulse.pair)
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(
                            Modifier.fillMaxWidth().clickable { viewModel.refreshRadar() },
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                "SEGARKAN RADAR",
                                color = CyberAqua, fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // --- pintu masuk journal ---
                item {
                    GlowCard(accent = CyberAqua, modifier = Modifier.fillMaxWidth(), onClick = onOpenSignals) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "JOURNAL SIGNAL",
                                    color = CyberAqua,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                )
                                Text(
                                    "${signals.size} signal tersimpan · ketuk untuk membuka",
                                    color = TextSubtle, fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                            Text("›", color = CyberAqua, fontSize = 24.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                item { SectionLabel("signal terbaru") }

                if (signals.isEmpty()) {
                    item {
                        GlowCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Belum ada signal.", color = CyberLime,
                                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Tekan SCAN PASAR di bawah: satu putaran mengevaluasi 5 strategi x " +
                                "4 pair x 2 timeframe dari data yang sama. Kalau tidak ada setup lolos " +
                                "gate, hasil kosong dan itu benar.",
                                color = TextSubtle, fontSize = 12.sp,
                            )
                        }
                    }
                }

                items(signals, key = { it.id }) { signal ->
                    SignalCard(signal) { onOpenSignal(signal.id) }
                }

                item {
                    Text(
                        "Signal only. Not financial advice. Trade at your own risk.",
                        color = TextSubtle, fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            sweep.message?.let { msg ->
                Text(
                    msg,
                    color = if (sweep.newSignals > 0) CyberLime else TextSubtle,
                    fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("JOURNAL", accent = CyberAqua, modifier = Modifier.height(50.dp)) {
                    onOpenSignals()
                }
                Box(Modifier.weight(1f)) {
                    NeonButton(
                        if (sweep.running) "MEMINDAI…" else "SCAN PASAR",
                        enabled = !sweep.running,
                    ) { viewModel.runSweep() }
                }
            }
            Spacer(Modifier.height(8.dp))
            GhostButton(
                "STRATEGI · ${chart.pair}",
                accent = CyberLime,
                modifier = Modifier.fillMaxWidth().height(44.dp),
            ) { onScan(chart.pair) }
        }
    }
}

@Composable
private fun CenterHint(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text,
            color = TextSubtle, fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 18.dp),
        )
    }
}

@Composable
private fun PulseRow(pulse: MarketPulse, onClick: () -> Unit) {
    val accent = when (pulse.state) {
        PulseState.TRENDING_UP -> CyberLime
        PulseState.TRENDING_DOWN -> CyberCrimson
        PulseState.EXHAUSTED -> AmberGlow
        else -> CyberAqua
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(78.dp)) {
            Text(
                pulse.pair, color = TextPrimary, fontSize = 12.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
            )
            Text(
                "${if (pulse.changePct >= 0) "+" else ""}${"%.2f".format(pulse.changePct)}%",
                color = if (pulse.changePct >= 0) CyberLime else CyberCrimson,
                fontSize = 9.sp, fontFamily = FontFamily.Monospace,
            )
        }
        // bar tekanan RSI
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(PanelDark),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(pulse.pressure.toFloat().coerceIn(0.05f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent.copy(alpha = 0.8f)),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "RSI ${"%.0f".format(pulse.rsi)}",
            color = accent, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(52.dp),
        )
    }
}

@Composable
private fun SignalCard(signal: Signal, onClick: () -> Unit) {
    val accent = if (signal.direction.name == "LONG") CyberLime else CyberCrimson
    GlowCard(accent = accent, modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScoreRing(signal.quantScore)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${signal.pair} · ${signal.timeframe}",
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    )
                    Spacer(Modifier.width(8.dp))
                    DirectionPill(signal.direction.name)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${signal.coreStrategy.label} · konfluensi ${signal.totalConfluence}/${signal.threshold}",
                    color = TextSubtle, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "E ${formatPrice(signal.entry)}", color = accent, fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        "RR ${signal.riskReward}", color = TextSubtle, fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
            Text("›", color = TextSubtle, fontSize = 22.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

internal fun formatPrice(price: Double): String = when {
    price >= 1000 -> "%.2f".format(price)
    price >= 100 -> "%.3f".format(price)
    else -> "%.5f".format(price)
}
