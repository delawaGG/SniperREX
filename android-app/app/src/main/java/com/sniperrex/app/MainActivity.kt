package com.sniperrex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sniperrex.app.presentation.common.CrashScreen
import com.sniperrex.app.presentation.navigation.AppNavHost
import com.sniperrex.app.presentation.theme.SniperRexTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val lastCrash = CrashGuard.read(this, BuildConfig.VERSION_CODE)
        setContent {
            SniperRexTheme {
                var crash by remember { mutableStateOf(lastCrash) }
                if (crash != null) {
                    CrashScreen(trace = crash!!) {
                        CrashGuard.clear(this)
                        crash = null
                    }
                } else {
                    AppNavHost()
                }
            }
        }
    }
}
