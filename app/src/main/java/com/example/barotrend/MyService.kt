package com.example.barotrend

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder

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
            val restartServiceIntent = Intent(applicationContext, this.javaClass)
            restartServiceIntent.setPackage(packageName)
            startService(restartServiceIntent)
            super.onTaskRemoved(rootIntent)
            if (pressure != 0F) {
                db?.addValue(pressure)
            }
        }, 1800000)

    }

    companion object {
        var pressure: Float = 0.0f
    }
}