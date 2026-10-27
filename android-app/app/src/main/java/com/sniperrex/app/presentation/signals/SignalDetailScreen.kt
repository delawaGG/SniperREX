package com.sniperrex.app.presentation.signals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sniperrex.app.presentation.components.CyberBackground
import com.sniperrex.app.presentation.components.CyberTopBar
import com.sniperrex.app.presentation.components.DirectionPill
import com.sniperrex.app.presentation.components.GhostButton
import com.sniperrex.app.presentation.components.GlowCard
import com.sniperrex.app.presentation.components.LevelRow
import com.sniperrex.app.presentation.components.NeonButton
import com.sniperrex.app.presentation.components.ScoreRing
import com.sniperrex.app.presentation.components.SectionLabel
import com.sniperrex.app.presentation.theme.CyberAqua
import com.sniperrex.app.presentation.theme.CyberCrimson
import com.sniperrex.app.presentation.theme.CyberLime
import com.sniperrex.app.presentation.theme.TextPrimary
import com.sniperrex.app.presentation.theme.TextSubtle

@Composable
fun SignalDetailScreen(
    signalId: String,
    onBack: () -> Unit,
    viewModel: SignalDetailViewModel = hiltViewModel(),
) {
    val signal by viewModel.signal(signalId).collectAsStateWithLifecycle()
    val analyzing by viewModel.analyzing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    CyberBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            CyberTopBar(title = "SIGNAL DETAIL", subtitle = "read-only · tidak ada eksekusi", onBack = onBack)

            val s = signal
            if (s == null) {
                Text("Memuat...", color = TextSubtle, modifier = Modifier.padding(16.dp))
                return@Column
            }

            val accent = if (s.direction.name == "LONG") CyberLime else CyberCrimson

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    GlowCard(accent = accent, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${s.pair} · ${s.timeframe}", color = TextPrimary,
                                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                )
                                Spacer(Modifier.height(6.dp))
                                DirectionPill(s.direction.name)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "${s.coreStrategy.label} · ${s.structureEvent}",
                                    color = TextSubtle, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                                )
                            }
                            ScoreRing(s.quantScore, size = 74.dp)
                        }
                    }
                }

                item { SectionLabel("level") }
                item {
                    GlowCard(modifier = Modifier.fillMaxWidth()) {
                        LevelRow("Entry", s.entry.toString(), CyberLime)
                        LevelRow("Stop loss", s.stopLoss.toString(), CyberCrimson)
                        s.takeProfits.forEachIndexed { index, tp ->
                            LevelRow("TP${index + 1}", tp.toString(), CyberAqua)
                        }
                        LevelRow("RR (TP1)", s.riskReward.toString(), TextPrimary)
                    }
                }

                item { SectionLabel("konteks") }
                item {
                    GlowCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "konfluensi ${s.totalConfluence}/${s.threshold}",
                            color = CyberLime, fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "trend ${s.trend} · RSI ${"%.1f".format(s.rsi)} · ATR ${"%.5f".format(s.atr)} · ${s.session}",
                            color = TextSubtle, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                        )
                        if (s.confirmations.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                s.confirmations.forEach { name ->
                                    Text(
                                        name, color = CyberLime, fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }

                item { SectionLabel("ai second opinion") }
                item {
                    GlowCard(accent = CyberAqua, modifier = Modifier.fillMaxWidth()) {
                        val ai = s.aiVerdict
                        if (ai == null) {
                            Text(
                                "Belum dianalisa. AI hanya pendapat kedua — status final tetap " +
                                "ditentukan skor kuantitatif.",
                                color = TextSubtle, fontSize = 11.sp,
                            )
                        } else {
                            Text(
                                "${ai.verdict.name} · score ${ai.aiScore}",
                                color = CyberAqua, fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                            )
                            ai.finalDecision?.let {
                                Text(
                                    "keputusan final: ${it.name}",
                                    color = TextPrimary, fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(ai.reasoning, color = TextPrimary, fontSize = 12.sp)
                            ai.risks.forEach {
                                Text("• $it", color = TextSubtle, fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        NeonButton(
                            text = if (analyzing) "MENGANALISA..." else "ANALYZE WITH AI",
                            enabled = !analyzing,
                            accent = CyberAqua,
                        ) { viewModel.analyze(signalId) }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GhostButton("SKIP", accent = CyberCrimson) { viewModel.skip(signalId) }
                        GhostButton("ARSIP", accent = CyberAqua) {}
                    }
                }

                error?.let {
                    item {
                        Text(
                            it, color = CyberCrimson, fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }

                item {
                    Text(
                        "Signal only. Not financial advice. Tidak ada tombol eksekusi order.",
                        color = TextSubtle, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}
