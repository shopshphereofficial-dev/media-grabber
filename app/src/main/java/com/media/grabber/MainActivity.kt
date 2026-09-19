package com.media.grabber

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File

class MainActivity : AppCompatActivity() {

    @Volatile
    private var engineReady = false

    @Volatile
    private var downloading = false

    private lateinit var etUrl: EditText
    private lateinit var btnDownload: Button
    private lateinit var tvStatus: TextView
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        etUrl = findViewById(R.id.etUrl)
        btnDownload = findViewById(R.id.btnDownload)
        tvStatus = findViewById(R.id.tvStatus)
        progress = findViewById(R.id.progress)

        findViewById<Button>(R.id.btnPaste).setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val text = cm.primaryClip?.getItemAt(0)?.text?.toString()
            if (!text.isNullOrBlank()) {
                etUrl.setText(text.trim())
            } else {
                toast("Clipboard khali hai")
            }
        }

        btnDownload.setOnClickListener {
            val url = etUrl.text.toString().trim()
            when {
                url.isEmpty() -> toast("Link paste karo pehle")
                !engineReady -> toast("Engine load ho raha hai... thoda ruk ke try karo")
                downloading -> toast("Download already chal raha hai")
                else -> startDownload(url)
            }
        }

        Thread {
            try {
                YoutubeDL.getInstance().init(applicationContext)
                FFmpeg.getInstance().init(applicationContext)
                engineReady = true
                runOnUiThread { tvStatus.text = "Engine ready \u2713 (yt-dlp)" }
            } catch (e: Exception) {
                runOnUiThread { tvStatus.text = "Engine error: ${e.message}" }
            }
        }.start()
    }

    private fun startDownload(url: String) {
        downloading = true
        btnDownload.isEnabled = false
        progress.visibility = ProgressBar.VISIBLE
        tvStatus.text = "Download shuru..."
        Thread {
            try {
                val dir = File(getExternalFilesDir(null), "downloads").apply { mkdirs() }
                val request = YoutubeDLRequest(url).apply {
                    addOption("-f", "bv*+ba/b")
                    addOption("--merge-output-format", "mp4")
                    addOption("-o", dir.absolutePath + "/%(title)s.%(ext)s")
                    addOption("--no-playlist")
                    addOption("--no-mtime")
                }
                YoutubeDL.getInstance().execute(request)

                val file = dir.listFiles()?.maxByOrNull { it.lastModified() }
                    ?: throw Exception("Downloaded file nahi mili")
                saveToDownloads(file)
                val name = file.name
                runOnUiThread {
                    tvStatus.text = "\u2705 Download complete!\nSaved: $name"
                    toast("Downloads mein save ho gaya")
                }
            } catch (e: Exception) {
                runOnUiThread { tvStatus.text = "\u274C Error: ${e.message}" }
            } finally {
                downloading = false
                runOnUiThread {
                    btnDownload.isEnabled = true
                    progress.visibility = ProgressBar.GONE
                }
            }
        }.start()
    }

    private fun saveToDownloads(file: File) {
        val mime = when (file.extension.lowercase()) {
            "mp4", "m4v" -> "video/mp4"
            "webm" -> "video/webm"
            "mkv" -> "video/x-matroska"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> "application/octet-stream"
        }
        val resolver = contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, file.name)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri: Uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw Exception("Downloads folder mein save nahi kar paye")
        resolver.openOutputStream(uri)?.use { out ->
            file.inputStream().use { it.copyTo(out) }
        }
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        file.delete()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
