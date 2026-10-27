package com.sniperrex.app.presentation.settings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sniperrex.app.presentation.components.CyberBackground
import com.sniperrex.app.presentation.components.CyberTopBar
import com.sniperrex.app.presentation.components.GhostButton
import com.sniperrex.app.presentation.components.GlowCard
import com.sniperrex.app.presentation.components.NeonButton
import com.sniperrex.app.presentation.components.SectionLabel
import com.sniperrex.app.presentation.theme.BorderGray
import com.sniperrex.app.presentation.theme.CyberAqua
import com.sniperrex.app.presentation.theme.CyberLime
import com.sniperrex.app.presentation.theme.PanelElevated
import com.sniperrex.app.presentation.theme.TextPrimary
import com.sniperrex.app.presentation.theme.TextSubtle

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CyberBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            CyberTopBar(
                title = "SETTINGS",
                subtitle = "kunci milikmu, disimpan di perangkat",
                onBack = onBack,
            )

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    GlowCard(accent = CyberLime, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "MULAI DARI SINI", color = CyberLime, fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Tidak perlu server. Engine berjalan di HP ini: " +
                            "data diambil langsung dari Twelve Data memakai key kamu, " +
                            "lalu 5 strategi SMC dihitung lokal.",
                            color = TextSubtle, fontSize = 12.sp,
                        )
                    }
                }

                item { SectionLabel("twelve data") }
                item {
                    GlowCard(modifier = Modifier.fillMaxWidth()) {
                        Field(
                            label = "API key (gratis di twelvedata.com)",
                            value = state.twelveKey,
                            secret = true,
                            onChange = viewModel::onTwelveKey,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GhostButton("TEST KONEKSI", accent = CyberAqua) { viewModel.testTwelve() }
                        }
                        state.twelveStatus?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, color = TextSubtle, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                item { SectionLabel("ai second opinion (opsional)") }
                item {
                    GlowCard(modifier = Modifier.fillMaxWidth()) {
                        Field("AI API key", state.aiKey, secret = true, onChange = viewModel::onAiKey)
                        Spacer(Modifier.height(8.dp))
                        Field("Base URL (OpenAI-compatible)", state.aiBaseUrl, onChange = viewModel::onAiBase)
                        Spacer(Modifier.height(8.dp))
                        Field("Model", state.aiModel, onChange = viewModel::onAiModel)
                        Spacer(Modifier.height(10.dp))
                        GhostButton("TEST AI", accent = CyberAqua) { viewModel.testAi() }
                        state.aiStatus?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, color = TextSubtle, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                item { SectionLabel("otomatisasi") }
                item {
                    GlowCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Auto-scan 15 menit", color = TextPrimary, fontSize = 13.sp)
                                Text(
                                    "2 pair bergiliran · hemat kuota 800/hari",
                                    color = TextSubtle, fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                            Switch(
                                checked = state.autoScan,
                                onCheckedChange = viewModel::setAutoScan,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CyberLime,
                                    checkedTrackColor = CyberLime.copy(alpha = 0.3f),
                                    uncheckedThumbColor = TextSubtle,
                                    uncheckedTrackColor = PanelElevated,
                                ),
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        GhostButton("SCAN SEKARANG", accent = CyberLime) { viewModel.scanNow() }
                    }
                }

                item {
                    NeonButton(
                        text = if (state.busy) "MENYIMPAN..." else "SIMPAN PENGATURAN",
                        enabled = !state.busy,
                    ) { viewModel.save() }
                }

                item {
                    Text(
                        "Signal only. Not financial advice. " +
                        "Aplikasi tidak punya tombol eksekusi order dan tidak terhubung broker.",
                        color = TextSubtle, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    secret: Boolean = false,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 11.sp) },
        singleLine = true,
        visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyberLime,
            unfocusedBorderColor = BorderGray,
            focusedLabelColor = CyberLime,
            unfocusedLabelColor = TextSubtle,
            cursorColor = CyberLime,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
