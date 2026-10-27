package com.sniperrex.app.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sniperrex.app.presentation.components.CyberBackground
import com.sniperrex.app.presentation.components.GlowCard
import com.sniperrex.app.presentation.components.NeonButton
import com.sniperrex.app.presentation.theme.CyberCrimson
import com.sniperrex.app.presentation.theme.CyberLime
import com.sniperrex.app.presentation.theme.TextSubtle

/** Layar diagnostik: tampilkan crash terakhir agar bisa difoto/dilaporkan. */
@Composable
fun CrashScreen(trace: String, onClear: () -> Unit) {
    CyberBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "SNIPERREX CRASH", color = CyberCrimson,
                fontFamily = FontFamily.Monospace, fontSize = 18.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Kirim tangkapan layar ini ke Anto/agent supaya bisa diperbaiki.",
                color = TextSubtle, fontSize = 11.sp,
            )
            Spacer(Modifier.height(12.dp))
            GlowCard(accent = CyberCrimson, modifier = Modifier.fillMaxWidth()) {
                Text(
                    trace.take(4000), color = CyberLime, fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth()) {
                NeonButton("HAPUS LOG & COBA LAGI", onClick = onClear)
            }
        }
    }
}
