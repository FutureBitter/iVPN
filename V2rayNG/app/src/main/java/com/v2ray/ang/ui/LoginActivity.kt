package com.v2ray.ang.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import com.v2ray.ang.R
import com.v2ray.ang.util.AngConfigManager
import com.v2ray.ang.dto.EConfigType
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val prefs = getSharedPreferences("app_auth", MODE_PRIVATE)
        if (prefs.getBoolean("is_logged_in", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        val etUser = findViewById<EditText>(R.id.etUsername)
        val etPass = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)

        btnLogin.setOnClickListener {
            val user = etUser.text.toString()
            val pass = etPass.text.toString()

            if (user.isEmpty() || pass.isEmpty()) {
                tvStatus.text = "لطفا همه فیلدها را پر کنید"
                return@setOnClickListener
            }

            tvStatus.text = "در حال اتصال..."
            btnLogin.isEnabled = false

            thread {
                try {
                    val url = URL("https://fachur.ir/panel.php?api=login")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")

                    val jsonInput = JSONObject()
                    jsonInput.put("username", user)
                    jsonInput.put("password", pass)

                    conn.outputStream.use { it.write(jsonInput.toString().toByteArray()) }

                    val responseCode = conn.responseCode
                    if (responseCode == 200) {
                        val response = conn.inputStream.bufferedReader().readText()
                        val jsonRes = JSONObject(response)
                        
                        if (jsonRes.getBoolean("success")) {
                            val data = jsonRes.getJSONObject("data")
                            val subUrl = data.getString("subscription_url")

                            prefs.edit().putBoolean("is_logged_in", true).apply()

                            runOnUiThread {
                                importConfig(subUrl)
                            }
                        } else {
                            runOnUiThread {
                                tvStatus.text = "نام کاربری یا رمز عبور اشتباه است"
                                btnLogin.isEnabled = true
                            }
                        }
                    } else {
                        runOnUiThread {
                            tvStatus.text = "خطا در ارتباط با سرور"
                            btnLogin.isEnabled = true
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        tvStatus.text = "خطا: ${e.message}"
                        btnLogin.isEnabled = true
                    }
                }
            }
        }
    }

    private fun importConfig(url: String) {
        try {
           val config = AngConfigManager.importBatchConfig(url, "", false)
           Toast.makeText(this, "خوش آمدید! کانفیگ دریافت شد.", Toast.LENGTH_LONG).show()
           startActivity(Intent(this, MainActivity::class.java))
           finish()
        } catch (e: Exception) {
            Toast.makeText(this, "خطا در ذخیره کانفیگ: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
