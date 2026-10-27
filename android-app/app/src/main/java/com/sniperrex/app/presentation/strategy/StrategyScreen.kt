package com.sniperrex.app.presentation.strategy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.sniperrex.app.domain.model.CoreStrategy
import com.sniperrex.app.presentation.components.CyberBackground
import com.sniperrex.app.presentation.components.CyberTopBar
import com.sniperrex.app.presentation.components.GhostButton
import com.sniperrex.app.presentation.components.GlowCard
import com.sniperrex.app.presentation.components.LevelRow
import com.sniperrex.app.presentation.components.NeonButton
import com.sniperrex.app.presentation.components.SectionLabel
import com.sniperrex.app.presentation.theme.BorderGray
import com.sniperrex.app.presentation.theme.CyberAqua
import com.sniperrex.app.presentation.theme.CyberCrimson
import com.sniperrex.app.presentation.theme.CyberLime
import com.sniperrex.app.presentation.theme.PanelElevated
import com.sniperrex.app.presentation.theme.TextPrimary
import com.sniperrex.app.presentation.theme.TextSubtle

@Composable
fun StrategyScreen(
    pair: String,
    timeframe: String,
    onBack: () -> Unit,
    onScanned: (String) -> Unit,
    viewModel: StrategyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var strategy by remember { mutableStateOf(CoreStrategy.FVG) }
    var rsi by remember { mutableStateOf(true) }
    var atr by remember { mutableStateOf(true) }
    var ob by remember { mutableStateOf(true) }

    CyberBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            CyberTopBar(
                title = "$pair · $timeframe",
                subtitle = "engine lokal · tanpa server",
                onBack = onBack,
            )

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    GlowCard(accent = CyberLime, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "PRESET SMC COMBO", color = CyberLime, fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "FVG + RSI/ATR/OB di M15 — konfluensi tinggi, risiko paling terukur.",
                            color = TextSubtle, fontSize = 12.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        GhostButton("PAKAI PRESET", accent = CyberLime) {
                            strategy = CoreStrategy.FVG
                            rsi = true; atr = true; ob = true
                        }
                    }
                }

                item { SectionLabel("strategi inti") }
                items(CoreStrategy.entries.size) { index ->
                    val item = CoreStrategy.entries[index]
                    StrategyRow(item, strategy == item) { strategy = item }
                }

                item { SectionLabel("konfirmasi") }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToggleChip("RSI", rsi) { rsi = it }
                        ToggleChip("ATR", atr) { atr = it }
                        ToggleChip("OB", ob) { ob = it }
                    }
                }

                item {
                    NeonButton(
                        text = if (state.busy) "MENGANALISA..." else "SCAN MARKET",
                        enabled = !state.busy,
                    ) {
                        val confirmations = buildList {
                            if (rsi) add("RSI")
                            if (atr) add("ATR")
                            if (ob) add("OB")
                        }
                        viewModel.scan(pair, timeframe, strategy, confirmations)
                    }
                }

                state.message?.let { message ->
                    item {
                        GlowCard(
                            accent = if (state.signal != null) CyberLime else CyberAqua,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (state.signal != null) "SETUP DITEMUKAN" else "HASIL SCAN",
                                color = if (state.signal != null) CyberLime else CyberAqua,
                                fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(message, color = TextPrimary, fontSize = 12.sp)
                            state.signal?.let { s ->
                                Spacer(Modifier.height(8.dp))
                                LevelRow("Entry", s.entry.toString(), CyberLime)
                                LevelRow("SL", s.stopLoss.toString(), CyberCrimson)
                                LevelRow("TP1", s.takeProfits.firstOrNull()?.toString() ?: "-", CyberLime)
                                LevelRow("Score", s.quantScore.toString(), CyberAqua)
                                Spacer(Modifier.height(10.dp))
                                GhostButton("BUKA DETAIL") { onScanned(s.id) }
                            }
                        }
                    }
                }

                if (state.warnings.isNotEmpty()) {
                    item {
                        GlowCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "DIAGNOSTIK", color = TextSubtle, fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(6.dp))
                            state.warnings.take(8).forEach { warning ->
                                Text(
                                    "• $warning", color = TextSubtle, fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StrategyRow(strategy: CoreStrategy, selected: Boolean, onClick: () -> Unit) {
    val accent = if (selected) CyberLime else BorderGray
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) CyberLime.copy(alpha = 0.08f) else PanelElevated)
            .border(1.dp, accent, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (selected) "◉" else "○", color = accent, fontSize = 16.sp,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                strategy.label, color = if (selected) CyberLime else TextPrimary,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            )
            Text(
                strategy.name, color = TextSubtle, fontSize = 9.sp, fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun ToggleChip(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val accent = if (checked) CyberLime else BorderGray
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (checked) CyberLime.copy(alpha = 0.12f) else PanelElevated)
            .border(1.dp, accent, RoundedCornerShape(10.dp))
            .clickable { onChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (checked) "✓" else "·", color = accent, fontSize = 12.sp,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label, color = if (checked) CyberLime else TextSubtle, fontSize = 11.sp,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
        )
    }
}
