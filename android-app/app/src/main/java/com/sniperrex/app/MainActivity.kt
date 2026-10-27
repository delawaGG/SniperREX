package com.sniperrex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sniperrex.app.presentation.navigation.AppNavHost
import com.sniperrex.app.presentation.theme.SniperRexTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SniperRexTheme {
                AppNavHost()
            }
        }
    }
}
