package com.sniperrex.app.presentation.signals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.sniperrex.app.presentation.dashboard.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignalsScreen(
    onBack: () -> Unit,
    onOpenSignal: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val signals by viewModel.signals.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Signal Journal") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Kembali") } },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (signals.isEmpty()) {
                item { Text("Belum ada signal tersimpan.") }
            }
            items(signals, key = { it.id }) { s ->
                Card(onClick = { onOpenSignal(s.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${s.pair} · ${s.timeframe} · ${s.direction.name} · ${s.status.name}")
                        Text("Entry ${s.entry} / SL ${s.stopLoss} / RR ${s.riskReward}")
                        Text("Score ${s.quantScore} · ${s.coreStrategy.label}")
                    }
                }
            }
        }
    }
}
