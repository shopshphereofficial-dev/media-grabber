package com.media.grabber

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.CookieManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import java.io.File

object GrabHelper {

    fun showFormatDialog(activity: Activity, url: String) {
        if (!GrabApp.engineReady) {
            Toast.makeText(activity, "Engine is loading - try again in a few seconds", Toast.LENGTH_SHORT).show()
            return
        }
        val options = arrayOf(
            "\uD83C\uDFAC  Video \u2014 Best available",
            "\uD83C\uDFAC  Video \u2014 1080p HD",
            "\uD83C\uDFAC  Video \u2014 720p HD",
            "\uD83C\uDFAC  Video \u2014 480p",
            "\uD83C\uDFAC  Video \u2014 360p (small)",
            "\uD83C\uDFB5  Audio \u2014 MP3 320kbps",
            "\uD83C\uDFB5  Audio \u2014 MP3 128kbps (small)"
        )
        AlertDialog.Builder(activity)
            .setTitle("Choose quality")
            .setItems(options) { _, which ->
                askNotificationPermission(activity)
                DownloadService.start(activity, url, which)
                Toast.makeText(activity, "Download started - see notification", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun askNotificationPermission(activity: Activity) {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1
            )
        }
    }

    // Exports WebView cookies (from logins made in the in-app browser) into a
    // Netscape cookie file that yt-dlp uses via --cookies. This is what makes
    // account-only / private content downloadable.
    fun writeCookieFile(context: Context, url: String): File? {
        return try {
            CookieManager.getInstance().flush()
            val host = Uri.parse(url).host ?: return null
            val domain = host.removePrefix("www.").removePrefix("m.")
            val cookieStr = CookieManager.getInstance().getCookie("https://$domain")
                ?: CookieManager.getInstance().getCookie("https://$host")
                ?: return null
            val sb = StringBuilder("# Netscape HTTP Cookie File\n")
            val expiry = System.currentTimeMillis() / 1000 + 60L * 60 * 24 * 365
            for (pair in cookieStr.split(";")) {
                val kv = pair.trim().split("=", limit = 2)
                if (kv.size != 2) continue
                sb.append(".").append(domain).append("\tTRUE\t/\tTRUE\t")
                    .append(expiry).append("\t").append(kv[0]).append("\t")
                    .append(kv[1]).append("\n")
            }
            if (sb.toString().lines().size <= 1) return null
            val f = File(context.cacheDir, "cookies.txt")
            f.writeText(sb.toString())
            f
        } catch (e: Exception) {
            null
        }
    }
}
