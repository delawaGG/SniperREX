package com.sniperrex.app.ui

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Widget TradingView gratis — tanpa API key (spec §12.7).
 * symbol format: OANDA:XAUUSD
 *
 * Catatan penting: widget tv.js mengukur tinggi container-nya. Kalau
 * html/body tidak diberi tinggi 100%, container berukuran 0 px dan chart
 * tampil kosong. Karena itu tinggi dipaksa lewat html, body, dan #tv.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TradingViewWidget(
    symbol: String,
    intervalMinutes: Int = 15,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            runCatching { WebView(context) }.getOrNull()?.apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                webViewClient = WebViewClient()
                tag = symbol
                loadDataWithBaseURL(BASE_URL, html(symbol, intervalMinutes), "text/html", "utf-8", null)
            } ?: android.view.View(context)
        },
        update = { view ->
            val web = view as? WebView ?: return@AndroidView
            // Muat ulang hanya kalau simbol benar-benar berganti.
            if (web.tag != symbol) {
                web.tag = symbol
                web.loadDataWithBaseURL(BASE_URL, html(symbol, intervalMinutes), "text/html", "utf-8", null)
            }
        },
    )
}

private const val BASE_URL = "https://www.tradingview.com"

private fun html(symbol: String, intervalMinutes: Int) = """
    <!DOCTYPE html>
    <html>
    <head>
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <style>
        html, body { margin:0; padding:0; height:100%; width:100%; background:#050607; overflow:hidden; }
        #tv { height:100%; width:100%; }
      </style>
    </head>
    <body>
      <div id="tv"></div>
      <script src="https://s3.tradingview.com/tv.js"></script>
      <script>
        function mount() {
          if (typeof TradingView === 'undefined') { setTimeout(mount, 250); return; }
          new TradingView.widget({
            container_id: "tv",
            autosize: true,
            symbol: "$symbol",
            interval: "$intervalMinutes",
            timezone: "Asia/Jakarta",
            theme: "dark",
            style: "1",
            locale: "id",
            hide_side_toolbar: true,
            hide_top_toolbar: false,
            allow_symbol_change: true,
            save_image: false,
            backgroundColor: "#050607",
            gridColor: "rgba(255,255,255,0.05)"
          });
        }
        mount();
      </script>
    </body>
    </html>
""".trimIndent()
