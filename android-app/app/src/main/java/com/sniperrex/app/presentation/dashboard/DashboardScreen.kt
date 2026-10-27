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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.sniperrex.app.presentation.theme.CyberAqua
import com.sniperrex.app.presentation.theme.CyberCrimson
import com.sniperrex.app.presentation.theme.CyberLime
import com.sniperrex.app.presentation.theme.TextSubtle
import com.sniperrex.app.ui.TradingViewWidget

@Composable
fun DashboardScreen(
    onOpenSettings: () -> Unit,
    onOpenSignals: () -> Unit,
    onScan: () -> Unit,
    onOpenSignal: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val signals by viewModel.signals.collectAsStateWithLifecycle()
    val longs = signals.count { it.direction.name == "LONG" }
    val avgScore = if (signals.isEmpty()) 0 else signals.sumOf { it.quantScore } / signals.size

    CyberBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            CyberTopBar(
                title = "SNIPERREX",
                subtitle = "smc engine · on-device",
                actions = {
                    GhostButton("JOURNAL", accent = CyberAqua) { onOpenSignals() }
                    Spacer(Modifier.width(8.dp))
                    GhostButton("SET", accent = CyberLime) { onOpenSettings() }
                },
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("SIGNAL", signals.size.toString(), CyberLime, Modifier.weight(1f))
                        StatTile("LONG", longs.toString(), CyberAqua, Modifier.weight(1f))
                        StatTile("AVG SCORE", avgScore.toString(), CyberLime, Modifier.weight(1f))
                    }
                }

                item {
                    GlowCard(accent = CyberLime, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "XAUUSD · M15", color = CyberLime,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            )
                            Text("LIVE CHART", color = TextSubtle, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(Modifier.height(10.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(230.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, CyberLime.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                        ) {
                            TradingViewWidget(symbol = "OANDA:XAUUSD", modifier = Modifier.fillMaxSize())
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
                                "1) Isi Twelve Data key di SET.  2) Tekan SCAN MARKET.  " +
                                "3) Engine mengevaluasi bar tertutup terakhir — kalau tidak ada setup, " +
                                "hasilnya kosong dan itu benar.",
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

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            NeonButton("SCAN MARKET", onClick = onScan)
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
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
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
