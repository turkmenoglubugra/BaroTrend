package com.example.barotrend

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class MyService : Service() {

    private var db: DBHelper? = DBHelper(this, null)

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        onTaskRemoved(intent)
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        Handler().postDelayed({
            try {
                val restartServiceIntent = Intent(applicationContext, this.javaClass)
                restartServiceIntent.setPackage(packageName)
                super.onTaskRemoved(rootIntent)
                startService(restartServiceIntent)
                if (pressure != 0F) {
                    db?.addValue(pressure)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 3600000)
    }

    companion object {
        var pressure: Float = 0.0f
    }
}