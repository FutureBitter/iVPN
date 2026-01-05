package com.ivpn.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import libv2ray.Libv2ray
import libv2ray.CoreCallbackHandler

class V2RayService : VpnService() {
    private var mInterface: ParcelFileDescriptor? = null
    private val core = Libv2ray.newCoreController(V2RayCallback())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "STOP") {
            stopVPN()
            return START_NOT_STICKY
        }
        
        val configContent = intent?.getStringExtra("CONFIG_CONTENT") ?: ""
        if (configContent.isNotEmpty()) {
            startVPN(configContent)
        }
        return START_STICKY
    }

    private fun startVPN(config: String) {
        val channelId = "ivpn_connection"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "VPN Status", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("iVPN")
            .setContentText("متصل به اینترنت آزاد")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
        
        startForeground(1, notification)

        val builder = Builder()
        builder.setSession("iVPN")
        builder.setMtu(1500)
        builder.addAddress("10.10.10.10", 24)
        builder.addRoute("0.0.0.0", 0)
        
        try {
            mInterface = builder.establish()
            try {
                core.startLoop(config)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun stopVPN() {
        try {
            core.stopLoop()
            mInterface?.close()
            mInterface = null
            stopForeground(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopSelf()
    }
    
    override fun onDestroy() {
        stopVPN()
        super.onDestroy()
    }
    
    class V2RayCallback : CoreCallbackHandler {
        override fun onEmitStatus(l: Long, s: String?): Long { return 0 }
        override fun startup(): Long { return 0 }
        override fun shutdown(): Long { return 0 }
    }
}
