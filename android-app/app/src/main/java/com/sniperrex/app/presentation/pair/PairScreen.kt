package com.sniperrex.app.presentation.pair

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sniperrex.app.presentation.strategy.StrategyViewModel

private data class PairOption(val symbol: String, val name: String, val category: String)

private val PAIRS = listOf(
    PairOption("XAUUSD", "Gold / USD", "Metal"),
    PairOption("EURUSD", "Euro / USD", "Forex"),
    PairOption("GBPUSD", "Pound / USD", "Forex"),
    PairOption("USDJPY", "USD / Yen", "Forex"),
    PairOption("BTCUSD", "Bitcoin / USD", "Crypto"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairScreen(
    onBack: () -> Unit,
    onContinue: (String, String) -> Unit,
    viewModel: StrategyViewModel = hiltViewModel(),
) {
    val timeframes by viewModel.timeframes.collectAsStateWithLifecycle()
    var selectedPair by remember { mutableStateOf("XAUUSD") }
    var selectedTf by remember { mutableStateOf("M15") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pilih Pair") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Kembali") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(12.dp)) {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PAIRS) { pair ->
                    Card(onClick = { selectedPair = pair.symbol }, modifier = Modifier.fillMaxSize()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${pair.symbol} — ${pair.name}")
                            Text(pair.category)
                        }
                    }
                }
            }
            Text("Timeframe (FREE: M15/M30/M45)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                timeframes.forEach { tf ->
                    FilterChip(
                        selected = selectedTf == tf,
                        onClick = { selectedTf = tf },
                        label = { Text(tf) },
                    )
                }
            }
            TextButton(onClick = { onContinue(selectedPair, selectedTf) }) {
                Text("Continue to Strategy →")
            }
        }
    }
}
