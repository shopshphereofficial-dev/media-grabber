package com.media.grabber

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var etUrl: EditText
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        etUrl = findViewById(R.id.etUrl)
        tvStatus = findViewById(R.id.tvStatus)

        findViewById<Button>(R.id.btnPaste).setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val text = cm.primaryClip?.getItemAt(0)?.text?.toString()
            if (!text.isNullOrBlank()) {
                etUrl.setText(text.trim())
            } else {
                toast("Clipboard is empty")
            }
        }

        findViewById<Button>(R.id.btnBrowse).setOnClickListener {
            startActivity(Intent(this, BrowserActivity::class.java))
        }

        findViewById<Button>(R.id.btnDownload).setOnClickListener {
            val input = etUrl.text.toString().trim()
            when {
                input.isEmpty() -> toast("Paste a link or type what you want to download")
                input.startsWith("http") -> GrabHelper.showFormatDialog(this, input)
                else -> {
                    // not a link - treat it as a search (opens the built-in browser)
                    val search = Intent(this, BrowserActivity::class.java)
                        .putExtra(
                            "url",
                            "https://www.youtube.com/results?search_query=" + android.net.Uri.encode(input)
                        )
                    startActivity(search)
                }
            }
        }

        findViewById<Button>(R.id.btnNavHome).setOnClickListener { }
        findViewById<Button>(R.id.btnNavFiles).setOnClickListener {
            startActivity(Intent(this, LibraryActivity::class.java))
        }
        findViewById<Button>(R.id.btnNavSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        Thread {
            var tries = 0
            while (!GrabApp.engineReady && tries < 120) {
                Thread.sleep(500)
                tries++
            }
            runOnUiThread {
                tvStatus.text = if (GrabApp.engineReady) {
                    "Engine ready \u2713"
                } else {
                    "Engine failed - try reopening the app"
                }
            }
        }.start()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
