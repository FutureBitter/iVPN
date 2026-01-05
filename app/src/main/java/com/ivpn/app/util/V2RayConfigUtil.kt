package com.ivpn.app.util

import android.util.Base64
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object V2RayConfigUtil {
    data class ConfigItem(val name: String, val uri: String, var ping: Long = -1, var isSelected: Boolean = false)

    fun parseSubscription(content: String): List<ConfigItem> {
        val list = mutableListOf<ConfigItem>()
        try {
            val decoded = try {
                String(Base64.decode(content, Base64.DEFAULT), StandardCharsets.UTF_8)
            } catch (e: Exception) { content }
            
            decoded.lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("vless://")) {
                    try {
                        val name = URLDecoder.decode(trimmed.substringAfter("#"), "UTF-8")
                        list.add(ConfigItem(name, trimmed))
                    } catch(e: Exception) {
                        list.add(ConfigItem("Server", trimmed))
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return list
    }

    fun generateV2RayJson(uriString: String): String {
        try {
            val uri = URI(uriString)
            val userInfo = uri.userInfo
            val host = uri.host
            val port = uri.port
            val query = uri.query ?: ""
            
            val json = JSONObject()
            val log = JSONObject().put("loglevel", "warning")
            json.put("log", log)
            
            val inbounds = org.json.JSONArray()
            val inbound = JSONObject()
            inbound.put("port", 10808)
            inbound.put("listen", "127.0.0.1")
            inbound.put("protocol", "socks")
            inbound.put("tag", "socks")
            inbound.put("settings", JSONObject().put("udp", true))
            inbounds.put(inbound)
            json.put("inbounds", inbounds)
            
            val outbounds = org.json.JSONArray()
            val outbound = JSONObject()
            outbound.put("tag", "proxy")
            outbound.put("protocol", "vless")
            
            val vnext = org.json.JSONArray()
            val server = JSONObject()
            server.put("address", host)
            server.put("port", port)
            val users = org.json.JSONArray()
            val user = JSONObject()
            user.put("id", userInfo)
            user.put("encryption", "none")
            users.put(user)
            server.put("users", users)
            vnext.put(server)
            
            val settings = JSONObject()
            settings.put("vnext", vnext)
            outbound.put("settings", settings)
            
            val stream = JSONObject()
            stream.put("network", "tcp")
            if (query.contains("type=ws")) {
                stream.put("network", "ws")
                stream.put("wsSettings", JSONObject().put("path", "/").put("headers", JSONObject()))
            }
            if (query.contains("security=tls")) {
                stream.put("security", "tls")
            }
            outbound.put("streamSettings", stream)
            
            outbounds.put(outbound)
            json.put("outbounds", outbounds)
            
            return json.toString()
        } catch (e: Exception) {
            return ""
        }
    }
}
