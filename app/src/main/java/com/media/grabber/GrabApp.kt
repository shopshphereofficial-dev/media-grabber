package com.media.grabber

import android.app.Application
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL

class GrabApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Thread {
            try {
                YoutubeDL.getInstance().init(this)
                FFmpeg.getInstance().init(this)
                engineReady = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    companion object {
        @Volatile
        var engineReady = false
    }
}
