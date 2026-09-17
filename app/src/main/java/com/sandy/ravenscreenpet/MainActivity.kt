package com.sandy.ravenscreenpet

import android.app.*
import android.os.Bundle
import android.content.*
import android.graphics.Color
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); render() }
    private fun render() {
        val pad = (24 * resources.displayMetrics.density).toInt()
        val box = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; setPadding(pad,pad,pad,pad); setBackgroundColor(Color.rgb(246,244,250)) }
        val title = TextView(this).apply { text="Raven 🖤"; textSize=34f; gravity=Gravity.CENTER; setTextColor(Color.rgb(25,24,30)) }
        val sub = TextView(this).apply { text="Your tiny screen companion\n\nRaven walks around your screen, naps, looks around, and gets a little sassy when you poke him too much."; textSize=17f; gravity=Gravity.CENTER; setPadding(0,pad/2,0,pad); setTextColor(Color.DKGRAY) }
        val start = Button(this).apply { text="Let Raven Out"; setOnClickListener { launchRaven() } }
        val stop = Button(this).apply { text="Put Raven Away"; setOnClickListener { stopService(Intent(this@MainActivity,RavenService::class.java)) } }
        box.addView(title); box.addView(sub); box.addView(start, LinearLayout.LayoutParams(-1,-2)); box.addView(stop, LinearLayout.LayoutParams(-1,-2)); setContentView(box)
    }
    private fun launchRaven() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            Toast.makeText(this,"Allow Display over other apps, then tap Let Raven Out again.",Toast.LENGTH_LONG).show(); return
        }
        startForegroundService(Intent(this,RavenService::class.java))
    }
}
