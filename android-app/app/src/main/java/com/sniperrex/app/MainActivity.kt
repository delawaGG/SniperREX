package com.sniperrex.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.sniperrex.app.presentation.common.CrashScreen
import com.sniperrex.app.presentation.navigation.AppNavHost
import com.sniperrex.app.presentation.theme.SniperRexTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* hasil dipakai saat scan */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Android 13+ : izin notifikasi default DITOLAK. Tanpa permintaan
        // eksplisit, semua notifikasi signal dibatalkan diam-diam.
        askNotificationPermission()

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

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
