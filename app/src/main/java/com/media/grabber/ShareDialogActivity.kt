package com.media.grabber

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ShareDialogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_dialog)

        val text = intent?.getStringExtra(Intent.EXTRA_TEXT) ?: run { finish(); return }
        val url = Regex("https?://\\S+").find(text)?.value ?: run {
            Toast.makeText(this, "No link found in the shared text", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<TextView>(R.id.tvDialogUrl).text = url

        val qualityButtons = listOf(
            R.id.btnQ0, R.id.btnQ1, R.id.btnQ2, R.id.btnQ3,
            R.id.btnQ4, R.id.btnQ5, R.id.btnQ6
        )
        for ((index, id) in qualityButtons.withIndex()) {
            findViewById<Button>(id).setOnClickListener {
                if (!GrabApp.engineReady) {
                    Toast.makeText(this, "Engine is loading - try again in a few seconds", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                GrabHelper.askNotificationPermission(this)
                DownloadService.start(this, url, index)
                Toast.makeText(this, "Download started - see notification", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        findViewById<Button>(R.id.btnCancel).setOnClickListener { finish() }
    }
}
