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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sniperrex.app.domain.model.Pairs
import com.sniperrex.app.domain.model.Signal
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
import com.sniperrex.app.ui.TradingViewWidget

/** Simbol TradingView untuk tiap pair internal. */
private val TV_SYMBOL = mapOf(
    "XAUUSD" to "OANDA:XAUUSD",
    "EURUSD" to "OANDA:EURUSD",
    "GBPUSD" to "OANDA:GBPUSD",
    "USDJPY" to "OANDA:USDJPY",
    "BTCUSD" to "BITSTAMP:BTCUSD",
)

@Composable
fun DashboardScreen(
    onOpenSettings: () -> Unit,
    onOpenSignals: () -> Unit,
    onScan: (String) -> Unit,
    onOpenSignal: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val signals by viewModel.signals.collectAsStateWithLifecycle()
    var selectedPair by remember { mutableStateOf("XAUUSD") }
    var selectedTf by remember { mutableStateOf("M15") }

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
                            val active = info.symbol == selectedPair
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active) CyberLime.copy(alpha = 0.14f) else PanelDark)
                                    .border(
                                        1.dp,
                                        if (active) CyberLime else BorderGray,
                                        RoundedCornerShape(10.dp),
                                    )
                                    .clickable { selectedPair = info.symbol }
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

                // --- chart ---
                item {
                    GlowCard(accent = CyberLime, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "$selectedPair · $selectedTf", color = CyberLime,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(listOf("M15", "M30", "H1", "H4")) { tf ->
                                    val active = tf == selectedTf
                                    Text(
                                        tf,
                                        color = if (active) CyberLime else TextSubtle,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (active) CosmicDark else PanelDark)
                                            .clickable { selectedTf = tf }
                                            .padding(horizontal = 6.dp, vertical = 3.dp),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, CyberLime.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                        ) {
                            TradingViewWidget(
                                symbol = TV_SYMBOL[selectedPair] ?: "OANDA:XAUUSD",
                                intervalMinutes = when (selectedTf) {
                                    "M30" -> 30
                                    "H1" -> 60
                                    "H4" -> 240
                                    else -> 15
                                },
                                modifier = Modifier.fillMaxSize(),
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
                                "1) Isi Twelve Data key di SET.  2) Tekan SCAN.  " +
                                "3) Engine hanya mengevaluasi bar tertutup terakhir — kalau tidak ada setup, " +
                                "hasil kosong dan itu benar.",
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

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GhostButton("JOURNAL", accent = CyberAqua, modifier = Modifier.height(50.dp)) { onOpenSignals() }
            Box(Modifier.weight(1f)) {
                NeonButton("SCAN $selectedPair") { onScan(selectedPair) }
            }
        }
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
                        "E ${signal.entry}", color = accent, fontSize = 12.sp,
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
