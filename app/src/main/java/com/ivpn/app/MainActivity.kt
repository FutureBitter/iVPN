package com.ivpn.app
import android.content.Intent
import android.graphics.Color
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private var isConnected = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val prefs = getSharedPreferences("ivpn_data", MODE_PRIVATE)
        val subLink = prefs.getString("sub_link", "")
        
        val btnConnect = findViewById<Button>(R.id.btnConnect)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        btnConnect.setOnClickListener {
            if (!isConnected) {
                // درخواست مجوز VPN از اندروید
                val intent = VpnService.prepare(this)
                if (intent != null) {
                    startActivityForResult(intent, 1)
                } else {
                    startVpnService(subLink ?: "")
                    updateUI(true, btnConnect, tvStatus)
                }
            } else {
                // قطع اتصال
                val intent = Intent(this, V2RayService::class.java)
                intent.action = "STOP"
                startService(intent)
                updateUI(false, btnConnect, tvStatus)
            }
        }
        
        btnLogout.setOnClickListener {
            prefs.edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            val prefs = getSharedPreferences("ivpn_data", MODE_PRIVATE)
            val subLink = prefs.getString("sub_link", "")
            startVpnService(subLink ?: "")
            val btnConnect = findViewById<Button>(R.id.btnConnect)
            val tvStatus = findViewById<TextView>(R.id.tvStatus)
            updateUI(true, btnConnect, tvStatus)
        }
    }

    private fun startVpnService(link: String) {
        Toast.makeText(this, "در حال دریافت کانفیگ...", Toast.LENGTH_SHORT).show()
        // دانلود محتوای لینک سابسکریپشن
        val client = OkHttpClient()
        val request = Request.Builder().url(link).build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(applicationContext, "خطا در دانلود کانفیگ", Toast.LENGTH_SHORT).show() }
            }
            override fun onResponse(call: Call, response: Response) {
                val configBody = response.body?.string()
                if (configBody != null) {
                    // استارت سرویس با کانفیگ دانلود شده
                    val intent = Intent(applicationContext, V2RayService::class.java)
                    // نکته: اینجا باید کانفیگ خام را به فرمت جیسون V2Ray تبدیل کنیم
                    // فعلا متن خام را میفرستیم (نیاز به پارسر دارد در آینده)
                    intent.putExtra("CONFIG_CONTENT", configBody) 
                    startForegroundService(intent)
                    runOnUiThread { 
                        Toast.makeText(applicationContext, "سرویس استارت شد", Toast.LENGTH_SHORT).show() 
                    }
                }
            }
        })
    }

    private fun updateUI(connected: Boolean, btn: Button, txt: TextView) {
        isConnected = connected
        if (connected) {
            txt.text = "متصل شد"
            txt.setTextColor(Color.parseColor("#10B981"))
            btn.setBackgroundColor(Color.parseColor("#374151"))
            btn.text = "قطع اتصال"
        } else {
            txt.text = "قطع شده"
            txt.setTextColor(Color.parseColor("#94A3B8"))
            btn.setBackgroundColor(Color.parseColor("#1E293B"))
            btn.text = "اتصال"
        }
    }
}
