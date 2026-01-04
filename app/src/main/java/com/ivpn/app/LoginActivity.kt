package com.ivpn.app
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val prefs = getSharedPreferences("ivpn_data", MODE_PRIVATE)
        if (prefs.getBoolean("is_logged_in", false)) {
            goToMain()
            return
        }
        val etUser = findViewById<EditText>(R.id.etUsername)
        val etPass = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val loading = findViewById<ProgressBar>(R.id.progressBar)

        btnLogin.setOnClickListener {
            val user = etUser.text.toString()
            val pass = etPass.text.toString()
            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "لطفا اطلاعات را وارد کنید", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            loading.visibility = View.VISIBLE
            btnLogin.isEnabled = false
            val client = OkHttpClient()
            val json = "{\"username\":\"$user\", \"password\":\"$pass\"}"
            val body = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("https://fachur.ir/panel.php?api=login")
                .post(body)
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        loading.visibility = View.GONE
                        btnLogin.isEnabled = true
                        Toast.makeText(applicationContext, "خطا در اتصال", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onResponse(call: Call, response: Response) {
                    val respBody = response.body?.string()
                    runOnUiThread {
                        loading.visibility = View.GONE
                        btnLogin.isEnabled = true
                        try {
                            if (response.isSuccessful && respBody != null) {
                                val gson = Gson()
                                val apiResponse = gson.fromJson(respBody, ApiResponse::class.java)
                                if (apiResponse.success) {
                                    prefs.edit()
                                        .putBoolean("is_logged_in", true)
                                        .putString("sub_link", apiResponse.data.subscription_url)
                                        .putString("username", user)
                                        .apply()
                                    goToMain()
                                } else {
                                    Toast.makeText(applicationContext, apiResponse.message ?: "اطلاعات اشتباه است", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(applicationContext, "خطای سرور", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(applicationContext, "خطای داده", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            })
        }
    }
    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
data class ApiResponse(val success: Boolean, val data: UserData, val message: String?)
data class UserData(val subscription_url: String)
