package com.sniperrex.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Bull = Color(0xFF1B9C62)
private val Bear = Color(0xFFC0392B)

private val Light = lightColorScheme(primary = Color(0xFF11293B), secondary = Bull, error = Bear)
private val Dark = darkColorScheme(primary = Color(0xFF9EC5DE), secondary = Bull, error = Bear)

@Composable
fun SniperRexTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}

val DirectionColor: Color @Composable get() = MaterialTheme.colorScheme.secondary
