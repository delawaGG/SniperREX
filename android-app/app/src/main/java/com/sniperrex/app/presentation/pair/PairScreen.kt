package com.sniperrex.app.presentation.pair

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.sniperrex.app.domain.model.PairInfo
import com.sniperrex.app.domain.model.Pairs
import com.sniperrex.app.presentation.components.CyberBackground
import com.sniperrex.app.presentation.components.CyberTopBar
import com.sniperrex.app.presentation.components.GlowCard
import com.sniperrex.app.presentation.components.NeonButton
import com.sniperrex.app.presentation.components.SectionLabel
import com.sniperrex.app.presentation.strategy.StrategyViewModel
import com.sniperrex.app.presentation.theme.BorderGray
import com.sniperrex.app.presentation.theme.CyberAqua
import com.sniperrex.app.presentation.theme.CyberLime
import com.sniperrex.app.presentation.theme.PanelElevated
import com.sniperrex.app.presentation.theme.TextPrimary
import com.sniperrex.app.presentation.theme.TextSubtle

@Composable
fun PairScreen(
    onBack: () -> Unit,
    onContinue: (String, String) -> Unit,
    viewModel: StrategyViewModel = hiltViewModel(),
) {
    val timeframes by viewModel.timeframes.collectAsStateWithLifecycle()
    var selectedPair by remember { mutableStateOf("XAUUSD") }
    var selectedTf by remember { mutableStateOf("M15") }

    CyberBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            CyberTopBar(
                title = "PILIH MARKET",
                subtitle = "5 instrumen · M15/M30/M45",
                onBack = onBack,
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(Pairs.ALL, key = { it.symbol }) { pair ->
                    PairCard(pair, pair.symbol == selectedPair) { selectedPair = pair.symbol }
                }
            }

            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionLabel("timeframe")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    timeframes.forEach { tf ->
                        TfChip(tf, tf == selectedTf) { selectedTf = tf }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Box(Modifier.navigationBarsPadding().padding(16.dp)) {
                NeonButton("LANJUT KE STRATEGI") { onContinue(selectedPair, selectedTf) }
            }
        }
    }
}

@Composable
private fun PairCard(pair: PairInfo, selected: Boolean, onClick: () -> Unit) {
    val accent = if (selected) CyberLime else BorderGray
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(PanelElevated)
            .border(if (selected) 1.dp else 1.dp, accent, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
    ) {
        Text(
            pair.symbol, color = if (selected) CyberLime else TextPrimary,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 15.sp,
        )
        Text(pair.name, color = TextSubtle, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(6.dp))
        Text(
            pair.category.uppercase(), color = CyberAqua, fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun TfChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val accent = if (selected) CyberLime else BorderGray
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) CyberLime.copy(alpha = 0.12f) else PanelElevated)
            .border(1.dp, accent, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            label, color = if (selected) CyberLime else TextSubtle,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp,
        )
    }
}
