package com.media.grabber

import android.app.Application
import android.content.Context
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL

class GrabApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Thread {
            try {
                YoutubeDL.getInstance().init(this)
                FFmpeg.getInstance().init(this)
                Aria2c.getInstance().init(this)
                engineReady = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
            updateEngineIfStale(this)
        }.start()
    }

    // Keeps the yt-dlp engine fresh (once per day) so site changes don't break
    // downloads - YouTube especially changes its internals every few weeks.
    private fun updateEngineIfStale(context: Context) {
        try {
            val prefs = context.getSharedPreferences("engine", Context.MODE_PRIVATE)
            val last = prefs.getLong("lastEngineUpdate", 0L)
            val now = System.currentTimeMillis()
            if (now - last < 24L * 60 * 60 * 1000) return
            YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)
            prefs.edit().putLong("lastEngineUpdate", now).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        @Volatile
        var engineReady = false
    }
}
