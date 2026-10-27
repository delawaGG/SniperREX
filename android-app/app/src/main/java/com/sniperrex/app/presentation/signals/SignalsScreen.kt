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
import androidx.compose.foundation.lazy.items
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
import com.sniperrex.app.domain.model.Signal
import com.sniperrex.app.presentation.components.CyberBackground
import com.sniperrex.app.presentation.components.CyberTopBar
import com.sniperrex.app.presentation.components.DirectionPill
import com.sniperrex.app.presentation.components.GlowCard
import com.sniperrex.app.presentation.components.ScoreRing
import com.sniperrex.app.presentation.dashboard.DashboardViewModel
import com.sniperrex.app.presentation.theme.CyberCrimson
import com.sniperrex.app.presentation.theme.CyberLime
import com.sniperrex.app.presentation.theme.TextPrimary
import com.sniperrex.app.presentation.theme.TextSubtle

@Composable
fun SignalsScreen(
    onBack: () -> Unit,
    onOpenSignal: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val signals by viewModel.signals.collectAsStateWithLifecycle()

    CyberBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            CyberTopBar(
                title = "JOURNAL",
                subtitle = "${signals.size} signal tersimpan",
                onBack = onBack,
            )
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (signals.isEmpty()) {
                    item {
                        GlowCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Belum ada signal.", color = TextPrimary, fontSize = 13.sp)
                            Text(
                                "Setiap scan yang menghasilkan setup akan otomatis tersimpan di sini.",
                                color = TextSubtle, fontSize = 11.sp,
                            )
                        }
                    }
                }
                items(signals, key = { it.id }) { signal -> JournalCard(signal) { onOpenSignal(signal.id) } }
            }
        }
    }
}

@Composable
private fun JournalCard(signal: Signal, onClick: () -> Unit) {
    val accent = if (signal.direction.name == "LONG") CyberLime else CyberCrimson
    GlowCard(accent = accent, modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScoreRing(signal.quantScore, size = 50.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${signal.pair} ${signal.timeframe}", color = TextPrimary,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    )
                    Spacer(Modifier.width(8.dp))
                    DirectionPill(signal.direction.name)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${signal.status.name} · ${signal.coreStrategy.label}",
                    color = TextSubtle, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                )
            }
            Text("›", color = TextSubtle, fontSize = 20.sp, fontFamily = FontFamily.Monospace)
        }
    }
}
