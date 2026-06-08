package com.demo.cryptowallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

/**
 * The single Activity of the "Crypto Demo Wallet" application.
 *
 * It hosts one [WebView] that renders a fully self-contained, offline HTML page
 * (`assets/wallet.html`). The page mimics a Phantom-style wallet UI purely for
 * demonstration purposes — there is no networking, no blockchain access and no
 * persistence anywhere in the app.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)

        webView.settings.apply {
            // JavaScript is required for the demo UI (button dialogs, the
            // hidden balance editor, etc.). The page is fully local, so this
            // is safe — there is no remote content to execute.
            javaScriptEnabled = true

            // Hard-lock the WebView to local content only. No file-system
            // traversal, no content:// access, no remote URLs.
            allowFileAccess = false
            allowContentAccess = false
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = false
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = false

            // Cache nothing; the app stores no data.
            cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
            domStorageEnabled = false
            databaseEnabled = false
        }

        // Block any navigation attempt that is not our local asset. Even though
        // the page itself never triggers navigation, this guarantees the WebView
        // can never reach the network.
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return true
                return !url.startsWith("file:///android_asset/")
            }
        }

        if (savedInstanceState == null) {
            webView.loadUrl("file:///android_asset/wallet.html")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView.restoreState(savedInstanceState)
    }
}
