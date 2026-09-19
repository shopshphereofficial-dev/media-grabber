package com.media.grabber

import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class BrowserActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)
        webView = findViewById(R.id.webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.webViewClient = WebViewClient()

        findViewById<Button>(R.id.btnYt).setOnClickListener { load("https://www.youtube.com") }
        findViewById<Button>(R.id.btnIg).setOnClickListener { load("https://www.instagram.com") }
        findViewById<Button>(R.id.btnFb).setOnClickListener { load("https://www.facebook.com") }
        findViewById<Button>(R.id.btnTikTok).setOnClickListener { load("https://www.tiktok.com") }
        findViewById<Button>(R.id.btnTwitter).setOnClickListener { load("https://x.com") }
        findViewById<Button>(R.id.btnSoundCloud).setOnClickListener { load("https://soundcloud.com") }
        findViewById<Button>(R.id.btnDailymotion).setOnClickListener { load("https://www.dailymotion.com") }
        findViewById<Button>(R.id.btnWhatsapp).setOnClickListener { load("https://web.whatsapp.com") }

        findViewById<Button>(R.id.btnGrab).setOnClickListener {
            val url = webView.url
            if (url == null || !url.startsWith("http")) {
                Toast.makeText(this, "Open a video page first", Toast.LENGTH_SHORT).show()
            } else {
                GrabHelper.showFormatDialog(this, url)
            }
        }

        load(intent.getStringExtra("url") ?: "https://www.youtube.com")
    }

    private fun load(url: String) {
        webView.loadUrl(url)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
