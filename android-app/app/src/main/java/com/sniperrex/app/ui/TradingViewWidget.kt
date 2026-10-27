package com.sniperrex.app.ui

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Widget TradingView gratis — tanpa API key (spec §12.7).
 * symbol format: OANDA:XAUUSD
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TradingViewWidget(
    symbol: String,
    intervalMinutes: Int = 15,
    modifier: Modifier = Modifier,
) {
    val html = """
        <html><body style="margin:0;background:transparent">
        <div id="tv" style="height:100%"></div>
        <script src="https://s3.tradingview.com/tv.js"></script>
        <script>
          new TradingView.widget({
            container_id: "tv", autosize: true, symbol: "$symbol",
            interval: "$intervalMinutes", timezone: "Asia/Jakarta",
            theme: "dark", style: "1", hide_side_toolbar: true,
            allow_symbol_change: false
          });
        </script></body></html>
    """.trimIndent()

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val web = runCatching { WebView(context) }.getOrNull()
                ?: return@AndroidView android.view.View(context)
            web.apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                loadDataWithBaseURL("https://www.tradingview.com", html, "text/html", "utf-8", null)
            }
        },
    )
}
