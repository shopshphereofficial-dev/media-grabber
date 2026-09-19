package com.media.grabber

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.yausername.youtubedl_android.YoutubeDL

class SettingsActivity : AppCompatActivity() {

    private val qualityNames = arrayOf(
        "Best available", "1080p HD", "720p HD", "480p", "360p",
        "MP3 320kbps", "MP3 128kbps"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)

        findViewById<Button>(R.id.btnNavHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<Button>(R.id.btnNavFiles).setOnClickListener {
            startActivity(Intent(this, LibraryActivity::class.java))
        }
        findViewById<Button>(R.id.btnNavSettings).setOnClickListener { }

        val btnAsk = findViewById<Button>(R.id.btnAskQuality)
        fun refreshAsk() {
            btnAsk.text = if (prefs.getBoolean("ask_quality", true)) {
                "Ask quality before download: ON"
            } else {
                "Ask quality before download: OFF"
            }
        }
        refreshAsk()
        btnAsk.setOnClickListener {
            prefs.edit().putBoolean("ask_quality", !prefs.getBoolean("ask_quality", true)).apply()
            refreshAsk()
        }

        val btnDefault = findViewById<Button>(R.id.btnDefaultQuality)
        fun refreshDefault() {
            btnDefault.text = "Default quality: " + qualityNames[prefs.getInt("default_quality", 0)]
        }
        refreshDefault()
        btnDefault.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Default quality")
                .setItems(qualityNames) { _, which ->
                    prefs.edit().putInt("default_quality", which).apply()
                    refreshDefault()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        findViewById<Button>(R.id.btnUpdateEngine).setOnClickListener {
            Toast.makeText(this, "Updating engine - check back in a minute", Toast.LENGTH_SHORT).show()
            Thread {
                try {
                    YoutubeDL.getInstance().updateYoutubeDL(this@SettingsActivity, YoutubeDL.UpdateChannel.STABLE)
                    runOnUiThread {
                        Toast.makeText(this@SettingsActivity, "Engine updated \u2713", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@SettingsActivity, "Update failed - try again later", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }

        findViewById<Button>(R.id.btnClearHistory).setOnClickListener {
            History.clear(this)
            Toast.makeText(this, "Download history cleared", Toast.LENGTH_SHORT).show()
        }
    }
}
