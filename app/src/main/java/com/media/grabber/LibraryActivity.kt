package com.media.grabber

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LibraryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)

        findViewById<Button>(R.id.btnNavHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<Button>(R.id.btnNavFiles).setOnClickListener { }
        findViewById<Button>(R.id.btnNavSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val llList = findViewById<LinearLayout>(R.id.llList)
        llList.removeAllViews()
        val items = History.list(this)
        if (items.isEmpty()) {
            val tv = TextView(this)
            tv.text = "No downloads yet\n\nShare a video link from any app,\nor use BROWSE to download something"
            tv.setTextColor(0xFF9FB4D8.toInt())
            tv.textSize = 15f
            tv.gravity = Gravity.CENTER
            tv.setPadding(40, 80, 40, 80)
            llList.addView(tv)
            return
        }
        val fmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        for ((index, item) in items.withIndex()) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(18, 14, 10, 14)
            row.gravity = Gravity.CENTER_VERTICAL
            row.setBackgroundResource(R.drawable.bg_card)
            row.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }

            val icon = TextView(this)
            icon.text = if (item.mime.startsWith("audio")) "\uD83C\uDFB5" else "\uD83C\uDFAC"
            icon.textSize = 22f
            row.addView(icon)

            val text = LinearLayout(this)
            text.orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp.marginStart = 14
            text.layoutParams = lp

            val name = TextView(this)
            name.text = item.name
            name.setTextColor(0xFFFFFFFF.toInt())
            name.textSize = 14f
            name.maxLines = 2
            name.ellipsize = android.text.TextUtils.TruncateAt.END
            text.addView(name)

            val meta = TextView(this)
            meta.text = formatSize(item.size) + "  •  " + fmt.format(Date(item.time))
            meta.setTextColor(0xFF9FB4D8.toInt())
            meta.textSize = 12f
            text.addView(meta)

            row.addView(text)

            val del = TextView(this)
            del.text = "\uD83D\uDDD1"
            del.textSize = 20f
            del.setPadding(24, 8, 24, 8)
            del.setOnClickListener {
                try {
                    contentResolver.delete(Uri.parse(item.uri), null, null)
                } catch (e: Exception) {
                    // file may already be gone - just drop it from history
                }
                History.removeAt(this, index)
                render()
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
            }
            row.addView(del)

            row.setOnClickListener {
                try {
                    val open = Intent(Intent.ACTION_VIEW)
                    open.setDataAndType(Uri.parse(item.uri), item.mime)
                    open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(open)
                } catch (e: Exception) {
                    Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show()
                }
            }

            llList.addView(row)
        }
    }

    private fun formatSize(bytes: Long): String {
        return if (bytes >= 1024 * 1024) {
            String.format(Locale.US, "%.1f MB", bytes / 1048576.0)
        } else {
            String.format(Locale.US, "%.0f KB", bytes / 1024.0)
        }
    }
}
