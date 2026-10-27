package com.sniperrex.app.presentation.signals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignalDetailScreen(
    signalId: String,
    onBack: () -> Unit,
    viewModel: SignalDetailViewModel = hiltViewModel(),
) {
    val signal by viewModel.signal(signalId).collectAsStateWithLifecycle()
    val analyzing by viewModel.analyzing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Signal") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Kembali") } },
            )
        },
    ) { padding ->
        val s = signal
        if (s == null) {
            Text("Memuat...", Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        Column(
            Modifier.padding(padding).padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("${s.pair} · ${s.timeframe} · ${s.direction.name}")
            Text("${s.coreStrategy.label} · event ${s.structureEvent}")

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Levels")
                    Text("Entry: ${s.entry}")
                    Text("SL: ${s.stopLoss}  (risk ${"%.2f".format(s.riskPerUnit)})")
                    s.takeProfits.forEachIndexed { i, tp -> Text("TP${i + 1}: $tp") }
                    Text("RR (TP1): ${s.riskReward}")
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Konfluensi ${s.totalConfluence}/${s.threshold}")
                    Text("Trend ${s.trend} · RSI ${s.rsi} · ATR ${s.atr} · ${s.session}")
                    Text("Quant score ${s.quantScore}")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.analyze(signalId) }, enabled = !analyzing) {
                    Text(if (analyzing) "Menganalisa..." else "ANALYZE WITH AI")
                }
                OutlinedButton(onClick = { viewModel.skip(signalId) }) { Text("SKIP") }
            }

            s.aiVerdict?.let { ai ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("AI: ${ai.verdict.name} · score ${ai.aiScore}")
                        ai.finalDecision?.let { Text("Keputusan final: ${it.name}") }
                        Text(ai.reasoning)
                        ai.risks.forEach { Text("• $it") }
                    }
                }
            }

            error?.let { Text("Error: $it") }
            Text("Signal only. Not financial advice. Tidak ada tombol eksekusi.")
        }
    }
}
