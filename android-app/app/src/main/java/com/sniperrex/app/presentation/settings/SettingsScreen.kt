package com.sniperrex.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Kembali") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Twelve Data")
            OutlinedTextField(
                value = state.twelveKey,
                onValueChange = viewModel::onTwelveKey,
                label = { Text("API key (milikmu sendiri)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = viewModel::testTwelve, enabled = !state.busy) { Text("Test Twelve") }
                Button(onClick = viewModel::save, enabled = !state.busy) { Text("Simpan") }
            }
            state.twelveStatus?.let { Text(it) }

            Divider()
            Text("AI second opinion (opsional)")
            OutlinedTextField(
                value = state.aiKey,
                onValueChange = viewModel::onAiKey,
                label = { Text("AI API key") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.aiBaseUrl,
                onValueChange = viewModel::onAiBase,
                label = { Text("Base URL (OpenAI-compatible)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.aiModel,
                onValueChange = viewModel::onAiModel,
                label = { Text("Model") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedButton(onClick = viewModel::testAi, enabled = !state.busy) { Text("Test AI") }
            state.aiStatus?.let { Text(it) }

            Divider()
            Text("Scan latar belakang")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Auto-scan tiap 15 menit (4 pair, M15, FVG)")
                Switch(checked = state.autoScan, onCheckedChange = viewModel::setAutoScan)
            }
            OutlinedButton(onClick = { viewModel.scanNow() }) { Text("Scan sekarang") }
            Text("Butuh izin notifikasi untuk memberi tahu signal baru.")

            Divider()
            Text("Key disimpan di perangkat (DataStore) dan dikirim sebagai header. Server tidak menyimpan key.")
            Text("Signal only. Not financial advice.")
        }
    }
}
