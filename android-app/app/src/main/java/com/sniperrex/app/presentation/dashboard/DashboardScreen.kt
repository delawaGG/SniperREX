package com.sniperrex.app.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sniperrex.app.domain.model.Signal
import com.sniperrex.app.ui.TradingViewWidget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenSettings: () -> Unit,
    onOpenSignals: () -> Unit,
    onScan: () -> Unit,
    onOpenSignal: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val signals by viewModel.signals.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SniperRex") },
                actions = {
                    IconButton(onClick = onOpenSignals) { Text("Signals") }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onScan,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Scan Market") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    TradingViewWidget(
                        symbol = "OANDA:XAUUSD",
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                    )
                }
            }
            item {
                Text("Signal terbaru", style = MaterialTheme.typography.titleMedium)
            }
            if (signals.isEmpty()) {
                item { Text("Belum ada signal. Tekan Scan Market.") }
            }
            items(signals, key = { it.id }) { signal ->
                SignalRow(signal) { onOpenSignal(signal.id) }
            }
            item {
                Spacer(Modifier.height(72.dp))
                Text(
                    "Signal only. Not financial advice. Trade at your own risk.",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun SignalRow(signal: Signal, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${signal.pair} · ${signal.timeframe}", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                Text(signal.direction.name, style = MaterialTheme.typography.labelLarge)
            }
            Text("Entry ${signal.entry} | RR ${signal.riskReward} | score ${signal.quantScore}")
            Text("${signal.coreStrategy.label} · konfluensi ${signal.totalConfluence}/${signal.threshold}")
        }
    }
}
