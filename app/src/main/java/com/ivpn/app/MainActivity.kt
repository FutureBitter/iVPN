package com.ivpn.app
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private var isConnected = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val prefs = getSharedPreferences("ivpn_data", MODE_PRIVATE)
        val subLink = prefs.getString("sub_link", "No Link")
        val btnConnect = findViewById<Button>(R.id.btnConnect)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        btnConnect.setOnClickListener {
            if (!isConnected) {
                isConnected = true
                tvStatus.text = "متصل شد"
                tvStatus.setTextColor(Color.parseColor("#10B981"))
                btnConnect.setBackgroundColor(Color.parseColor("#374151"))
                Toast.makeText(this, "لینک دریافت شد: $subLink", Toast.LENGTH_LONG).show()
            } else {
                isConnected = false
                tvStatus.text = "قطع شده"
                tvStatus.setTextColor(Color.parseColor("#94A3B8"))
                btnConnect.setBackgroundColor(Color.parseColor("#1E293B"))
            }
        }
        btnLogout.setOnClickListener {
            prefs.edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
