package com.sniperrex.app.presentation.strategy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import com.sniperrex.app.domain.model.CoreStrategy

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$pair · $timeframe") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Kembali") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Preset rekomendasi: FVG + RSI/ATR/OB di M15")
                    OutlinedButton(onClick = {
                        strategy = CoreStrategy.FVG
                        rsi = true; atr = true; ob = true
                    }) { Text("Pakai Preset SMC Combo") }
                }
            }

            CoreStrategy.entries.forEach { item ->
                Row {
                    RadioButton(selected = strategy == item, onClick = { strategy = item })
                    Text("${item.label} (${item.name})")
                }
            }

            Text("Konfirmasi")
            listOf(
                Triple("RSI", rsi) { v: Boolean -> rsi = v },
                Triple("ATR (volatilitas)", atr) { v: Boolean -> atr = v },
                Triple("Order Block proximity", ob) { v: Boolean -> ob = v },
            ).forEach { (label, checked, setter) ->
                Row {
                    Checkbox(checked = checked, onCheckedChange = { setter(it ?: false) })
                    Text(label)
                }
            }

            Button(
                onClick = {
                    val confirmations = buildList {
                        if (rsi) add("RSI")
                        if (atr) add("ATR")
                        if (ob) add("OB")
                    }
                    viewModel.scan(pair, timeframe, strategy, confirmations)
                },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (state.busy) "Scanning..." else "SCAN MARKET") }

            state.message?.let { Text(it) }
            if (state.warnings.isNotEmpty()) {
                state.warnings.take(6).forEach { Text("• $it") }
            }
            state.signal?.let { s ->
                Button(onClick = { onScanned(s.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Buka detail signal")
                }
            }
        }
    }
}
