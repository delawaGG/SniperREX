package com.sniperrex.app.presentation.settings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import com.sniperrex.app.presentation.theme.CyberCrimson
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
    val keyboard = LocalSoftwareKeyboardController.current

    CyberBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            CyberTopBar(
                title = "SETTINGS",
                subtitle = "kunci milikmu, disimpan di perangkat",
                onBack = onBack,
            )

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp),
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
                            "1) Tempel Twelve Data key.  2) TEST KONEKSI.  3) SIMPAN.",
                            color = TextSubtle, fontSize = 12.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Engine berjalan di HP ini; data diambil langsung dari Twelve Data " +
                            "memakai key kamu lalu dihitung lokal.",
                            color = TextSubtle, fontSize = 11.sp,
                        )
                    }
                }

                item { SectionLabel("twelve data") }
                item {
                    GlowCard(modifier = Modifier.fillMaxWidth()) {
                        Field(
                            label = "API key (gratis di twelvedata.com)",
                            value = state.twelveKey,
                            secret = !state.revealTwelve,
                            onToggleReveal = viewModel::toggleRevealTwelve,
                            imeAction = ImeAction.Done,
                            onImeDone = { keyboard?.hide() },
                            onChange = viewModel::onTwelveKey,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            state.savedHint,
                            color = if (state.twelveKey.isBlank()) TextSubtle else CyberAqua,
                            fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GhostButton("TEST KONEKSI", accent = CyberAqua) { viewModel.testTwelve() }
                            GhostButton("SIMPAN", accent = CyberLime) { viewModel.save() }
                        }
                        state.twelveStatus?.let { StatusLine(it, state.twelveOk) }
                    }
                }

                item { SectionLabel("ai second opinion (opsional)") }
                item {
                    GlowCard(modifier = Modifier.fillMaxWidth()) {
                        Field(
                            label = "AI API key",
                            value = state.aiKey,
                            secret = !state.revealAi,
                            onToggleReveal = viewModel::toggleRevealAi,
                            onChange = viewModel::onAiKey,
                        )
                        Spacer(Modifier.height(8.dp))
                        Field(
                            label = "Base URL (OpenAI-compatible)",
                            value = state.aiBaseUrl,
                            keyboardType = KeyboardType.Uri,
                            onChange = viewModel::onAiBase,
                        )
                        Spacer(Modifier.height(8.dp))
                        Field("Model", state.aiModel, onChange = viewModel::onAiModel)
                        Spacer(Modifier.height(10.dp))
                        GhostButton("TEST AI", accent = CyberAqua) { viewModel.testAi() }
                        state.aiStatus?.let { StatusLine(it, state.aiOk) }
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
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            GhostButton(
                                text = "SCAN SEKARANG",
                                modifier = Modifier.weight(1f),
                                accent = CyberLime,
                            ) { viewModel.scanNow() }
                            GhostButton(
                                text = "TES NOTIFIKASI",
                                modifier = Modifier.weight(1f),
                                accent = CyberAqua,
                            ) { viewModel.testNotification() }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Notifikasi muncul saat scan menemukan setup. " +
                                "Jika tidak ada, aktifkan izin notifikasi SniperRex di pengaturan Android.",
                            color = TextSubtle, fontSize = 9.sp, fontFamily = FontFamily.Monospace,
                        )
                    }
                }

                item {
                    NeonButton(
                        text = if (state.busy) "MEMPROSES..." else "SIMPAN PENGATURAN",
                        enabled = !state.busy,
                    ) { viewModel.save() }
                }

                state.saveStatus?.let { msg ->
                    item {
                        Text(
                            msg,
                            color = if (msg.startsWith("Tersimpan") || msg.startsWith("Key tersimpan")) CyberLime else CyberCrimson,
                            fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                        )
                    }
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
private fun StatusLine(text: String, ok: Boolean?) {
    Spacer(Modifier.height(8.dp))
    Text(
        text,
        color = when (ok) {
            true -> CyberLime
            false -> CyberCrimson
            null -> TextSubtle
        },
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
    )
}

@Composable
private fun Field(
    label: String,
    value: String,
    secret: Boolean = false,
    onToggleReveal: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeDone: (() -> Unit)? = null,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 11.sp) },
        singleLine = true,
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onImeDone?.invoke() }),
        trailingIcon = if (onToggleReveal != null) {
            {
                Text(
                    if (secret) "LIHAT" else "SEMBUNYI",
                    color = CyberAqua,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onToggleReveal() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        } else null,
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
