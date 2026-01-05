package com.ivpn.app
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import libv2ray.V2RayPoint
import libv2ray.Libv2ray

class V2RayService : VpnService() {
    private var mInterface: ParcelFileDescriptor? = null
    private val v2rayPoint = Libv2ray.newV2RayPoint(V2RayCallback(), Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)

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
        // 1. نمایش نوتیفیکیشن
        val channelId = "ivpn_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "VPN Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("FUTURIX")
            .setContentText("متصل به اینترنت امن")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
        startForeground(1, notification)

        // 2. تنظیم تانل VPN
        val builder = Builder()
        builder.setSession("FUTURIX")
        builder.setMtu(1500)
        builder.addAddress("10.10.10.10", 24)
        builder.addRoute("0.0.0.0", 0)
        
        try {
            mInterface = builder.establish()
            val fd = mInterface?.fd ?: 0
            
            // 3. استارت هسته V2Ray
            // نکته: ما فعلا کانفیگ را مستقیم پاس می‌دهیم. 
            // در نسخه کامل باید جیسون تولید شود. اینجا فقط استارت می زنیم.
            if (!v2rayPoint.isRunning) {
                v2rayPoint.configureFile(config)
                v2rayPoint.runLoop(false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun stopVPN() {
        try {
            if (v2rayPoint.isRunning) {
                v2rayPoint.stopLoop()
            }
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
    
    // کلاس کالبک برای دریافت وضعیت از هسته
    class V2RayCallback : libv2ray.V2RayVPNServiceSupportsSet {
        override fun onEmitStatus(j: Long, s: String?): Long { return 0 }
        override fun prepare(): Long { return 0 }
        override fun protect(l: Long): Long { return 0 }
        override fun setup(s: String?): Long { return 0 }
        override fun shutdown(): Long { return 0 }
    }
}
