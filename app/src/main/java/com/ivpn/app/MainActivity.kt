package com.ivpn.app

import android.content.Intent
import android.graphics.Color
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ivpn.app.util.V2RayConfigUtil
import com.ivpn.app.util.V2RayConfigUtil.ConfigItem
import okhttp3.*
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import kotlin.concurrent.thread
import com.google.gson.Gson
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private var isConnected = false
    private lateinit var adapter: ConfigAdapter
    private var configList = mutableListOf<ConfigItem>()
    private var selectedConfig: ConfigItem? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val prefs = getSharedPreferences("ivpn_data", MODE_PRIVATE)
        val username = prefs.getString("username", "") ?: ""
        val subLink = prefs.getString("sub_link", "") ?: ""
        
        // المان‌های UI با ID های صحیح
        val btnLogout = findViewById<ImageView>(R.id.btnLogout)
        val tvUsed = findViewById<TextView>(R.id.tvUsed)
        val tvTotal = findViewById<TextView>(R.id.tvTotal)
        val tvExpire = findViewById<TextView>(R.id.tvExpire)
        val progress = findViewById<ProgressBar>(R.id.progressTraffic)
        val recycler = findViewById<RecyclerView>(R.id.recyclerConfigs)
        val btnConnect = findViewById<Button>(R.id.btnConnectMain) // دکمه جدید

        recycler.layoutManager = LinearLayoutManager(this)
        adapter = ConfigAdapter(configList) { item ->
            selectConfig(item)
        }
        recycler.adapter = adapter

        btnLogout.setOnClickListener {
            prefs.edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnConnect.setOnClickListener {
            if (!isConnected) {
                if (selectedConfig == null) {
                    Toast.makeText(this, "لطفا یک سرور انتخاب کنید", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val intent = VpnService.prepare(this)
                if (intent != null) {
                    startActivityForResult(intent, 1)
                } else {
                    startVpn()
                }
            } else {
                stopVpn()
            }
        }

        // پر کردن اطلاعات حجم (فعلا نمایشی تا به API وصل شود)
        tvUsed.text = "مصرف: ۰ گیگابایت"
        tvTotal.text = "کل: نامحدود"
        progress.progress = 0

        // دریافت لیست سرورها
        fetchUserData(subLink)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            startVpn()
        }
    }

    private fun fetchUserData(fallbackLink: String) {
        if (fallbackLink.isNotEmpty()) {
            val client = OkHttpClient()
            val request = Request.Builder().url(fallbackLink).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread { Toast.makeText(applicationContext, "خطا در دریافت لیست سرورها", Toast.LENGTH_SHORT).show() }
                }
                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    if (body != null) {
                        val parsed = V2RayConfigUtil.parseSubscription(body)
                        runOnUiThread {
                            configList.clear()
                            configList.addAll(parsed)
                            adapter.updateList(configList)
                            if (configList.isNotEmpty()) {
                                selectConfig(configList[0])
                                testPings()
                            } else {
                                Toast.makeText(applicationContext, "سروری یافت نشد", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            })
        }
    }

    private fun selectConfig(item: ConfigItem) {
        configList.forEach { it.isSelected = false }
        item.isSelected = true
        selectedConfig = item
        adapter.updateList(configList)
    }

    private fun testPings() {
        thread {
            configList.forEachIndexed { index, item ->
                try {
                    val uri = URI(item.uri)
                    val start = System.currentTimeMillis()
                    val socket = Socket()
                    socket.connect(InetSocketAddress(uri.host, uri.port), 2000) 
                    socket.close()
                    item.ping = System.currentTimeMillis() - start
                } catch (e: Exception) {
                    item.ping = -1 
                }
                runOnUiThread { adapter.notifyItemChanged(index) }
            }
            configList.sortBy { if (it.ping > 0) it.ping else 9999 }
            runOnUiThread { adapter.updateList(configList) }
        }
    }

    private fun startVpn() {
        selectedConfig?.let { config ->
            val jsonConfig = V2RayConfigUtil.generateV2RayJson(config.uri)
            if (jsonConfig.isNotEmpty()) {
                val intent = Intent(this, V2RayService::class.java)
                intent.putExtra("CONFIG_CONTENT", jsonConfig)
                startForegroundService(intent)
                isConnected = true
                findViewById<Button>(R.id.btnConnectMain).text = "قطع اتصال"
                findViewById<Button>(R.id.btnConnectMain).setBackgroundColor(Color.parseColor("#374151"))
            } else {
                Toast.makeText(this, "کانفیگ نامعتبر است", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopVpn() {
        val intent = Intent(this, V2RayService::class.java)
        intent.action = "STOP"
        startService(intent)
        isConnected = false
        findViewById<Button>(R.id.btnConnectMain).text = "اتصال هوشمند"
        findViewById<Button>(R.id.btnConnectMain).setBackgroundColor(Color.parseColor("#4F46E5"))
    }
}
