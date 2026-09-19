package com.media.grabber

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class DownloadService : Service() {

    companion object {
        private const val CHANNEL_ID = "downloads"

        fun start(context: Context, url: String, quality: Int) {
            val intent = Intent(context, DownloadService::class.java)
                .putExtra("url", url)
                .putExtra("quality", quality)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    private val active = AtomicInteger(0)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra("url") ?: return START_NOT_STICKY
        val quality = intent.getIntExtra("quality", 0)
        val notifId = url.hashCode()

        val initial = buildNotification("Preparing download...", 0, true)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(notifId, initial, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(notifId, initial)
        }

        active.incrementAndGet()

        Thread {
            try {
                val cookieFile = GrabHelper.writeCookieFile(this@DownloadService, url)

                val title = try {
                    val infoRequest = YoutubeDLRequest(url)
                    if (cookieFile != null) infoRequest.addOption("--cookies", cookieFile.absolutePath)
                    YoutubeDL.getInstance().getInfo(infoRequest).title ?: url
                } catch (e: Exception) {
                    url
                }

                updateNotification(notifId, "Downloading: $title", 0, true)

                // every download gets its own folder so parallel downloads
                // never pick up each other's files
                val workDir = File(
                    File(getExternalFilesDir(null), "downloads"),
                    System.currentTimeMillis().toString()
                )
                workDir.mkdirs()

                val request = YoutubeDLRequest(url).apply {
                    when (quality) {
                        1 -> {
                            addOption("-f", "bv*[height<=1080]+ba/b[height<=1080]")
                            addOption("--merge-output-format", "mp4")
                        }
                        2 -> {
                            addOption("-f", "bv*[height<=720]+ba/b[height<=720]")
                            addOption("--merge-output-format", "mp4")
                        }
                        3 -> {
                            addOption("-f", "bv*[height<=480]+ba/b[height<=480]")
                            addOption("--merge-output-format", "mp4")
                        }
                        4 -> {
                            addOption("-f", "bv*[height<=360]+ba/b[height<=360]")
                            addOption("--merge-output-format", "mp4")
                        }
                        5 -> {
                            addOption("-f", "ba/b")
                            addOption("-x")
                            addOption("--audio-format", "mp3")
                            addOption("--audio-quality", "320K")
                        }
                        6 -> {
                            addOption("-f", "ba/b")
                            addOption("-x")
                            addOption("--audio-format", "mp3")
                            addOption("--audio-quality", "128K")
                        }
                        else -> {
                            addOption("-f", "bv*+ba/b")
                            addOption("--merge-output-format", "mp4")
                        }
                    }
                    addOption("-o", workDir.absolutePath + "/%(title)s.%(ext)s")
                    addOption("--no-playlist")
                    addOption("--no-mtime")
                    addOption("--no-warnings")
                    addOption("--no-update")
                    // fast download: aria2c engine with 16 parallel connections
                    addOption("--downloader", "libaria2c.so")
                    if (cookieFile != null) {
                        addOption("--cookies", cookieFile.absolutePath)
                    }
                }

                YoutubeDL.getInstance().execute(request) { progress, _, _ ->
                    updateNotification(notifId, "Downloading: $title", progress.toInt(), true)
                }

                val file = workDir.listFiles()?.firstOrNull { it.isFile }
                    ?: throw Exception("downloaded file not found")
                saveToDownloads(file)
                workDir.deleteRecursively()
                finishWith(notifId, "Done: ${file.name}", true)
            } catch (e: Exception) {
                val msg = e.message ?: "unknown error"
                finishWith(notifId, "Failed: $msg", false)
            } finally {
                if (active.decrementAndGet() == 0) stopSelf()
            }
        }.start()

        return START_NOT_STICKY
    }

    private fun buildNotification(text: String, progress: Int, ongoing: Boolean): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Media Grabber")
            .setContentText(text)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, progress <= 0)
            .build()
    }

    private fun updateNotification(notifId: Int, text: String, progress: Int, ongoing: Boolean) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notifId, buildNotification(text, progress, ongoing))
    }

    private fun finishWith(notifId: Int, text: String, ok: Boolean) {
        stopForeground(STOP_FOREGROUND_REMOVE)
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(
                if (ok) android.R.drawable.stat_sys_download_done
                else android.R.drawable.stat_notify_error
            )
            .setContentTitle("Media Grabber")
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        nm.notify(notifId, n)
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
            ?: throw Exception("could not save to Downloads")
        resolver.openOutputStream(uri)?.use { out ->
            file.inputStream().use { it.copyTo(out) }
        }
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }
}
